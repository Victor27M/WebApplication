package com.victor.demo.service;

import com.victor.demo.model.PasswordResetToken;
import com.victor.demo.model.Person;
import com.victor.demo.repository.PasswordResetTokenRepository;
import com.victor.demo.repository.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTests {

    @Mock
    private PersonRepository personRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    private Person makePerson(String email) {
        Person p = new Person();
        p.setId(UUID.randomUUID());
        p.setEmail(email);
        p.setPassword("$2a$10$hashed");
        return p;
    }

    @Test
    void testRequestReset_Success_SendsEmail() {
        String email = "user@example.com";
        Person person = makePerson(email);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(tokenRepository).deleteByPerson(person);
        doNothing().when(emailService).sendPasswordResetCode(anyString(), anyString());

        assertDoesNotThrow(() -> passwordResetService.requestReset(email));

        verify(tokenRepository).deleteByPerson(person);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetCode(eq(email), anyString());
    }

    @Test
    void testRequestReset_EmailNotFound_ThrowsIllegalArgument() {
        when(personRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.requestReset("missing@example.com"));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetCode(any(), any());
    }

    @Test
    void testRequestReset_DeletesOldTokensFirst() {
        String email = "user@example.com";
        Person person = makePerson(email);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(tokenRepository).deleteByPerson(person);
        doNothing().when(emailService).sendPasswordResetCode(anyString(), anyString());

        passwordResetService.requestReset(email);

        verify(tokenRepository).deleteByPerson(person);
    }

    @Test
    void testConfirmReset_Success_UpdatesPassword() {
        String email = "user@example.com";
        String code = "123456";
        String newPassword = "NewPass1!";

        Person person = makePerson(email);
        PasswordResetToken token = new PasswordResetToken();
        token.setPerson(person);
        token.setCode(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        token.setUsed(false);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(tokenRepository.findByPersonAndCodeAndUsedFalseAndExpiresAtAfter(
                eq(person), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newHashed");
        when(personRepository.save(any())).thenReturn(person);
        when(tokenRepository.save(any())).thenReturn(token);
        doNothing().when(emailService).sendPasswordChangeConfirmation(email);

        assertDoesNotThrow(() -> passwordResetService.confirmReset(email, code, newPassword));

        verify(passwordEncoder).encode(newPassword);
        verify(personRepository).save(person);
        verify(emailService).sendPasswordChangeConfirmation(email);
        assertTrue(token.isUsed());
    }

    @Test
    void testConfirmReset_EmailNotFound_ThrowsIllegalArgument() {
        when(personRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.confirmReset("missing@example.com", "123456", "NewPass1!"));

        verify(personRepository, never()).save(any());
    }

    @Test
    void testConfirmReset_InvalidCode_ThrowsIllegalArgument() {
        String email = "user@example.com";
        Person person = makePerson(email);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(tokenRepository.findByPersonAndCodeAndUsedFalseAndExpiresAtAfter(
                eq(person), eq("999999"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.confirmReset(email, "999999", "NewPass1!"));

        verify(personRepository, never()).save(any());
    }

    @Test
    void testConfirmReset_SendsConfirmationEmail() {
        String email = "user@example.com";
        Person person = makePerson(email);
        PasswordResetToken token = new PasswordResetToken();
        token.setPerson(person);
        token.setCode("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(tokenRepository.findByPersonAndCodeAndUsedFalseAndExpiresAtAfter(
                eq(person), eq("123456"), any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
        when(personRepository.save(any())).thenReturn(person);
        when(tokenRepository.save(any())).thenReturn(token);
        doNothing().when(emailService).sendPasswordChangeConfirmation(email);

        passwordResetService.confirmReset(email, "123456", "NewPass1!");

        verify(emailService).sendPasswordChangeConfirmation(email);
    }
}