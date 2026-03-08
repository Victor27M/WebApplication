package com.victor.demo.service;

import com.victor.demo.model.LoginResponse;
import com.victor.demo.model.Person;
import com.victor.demo.model.PersonRole;
import com.victor.demo.repository.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTests {

    @Mock private PersonRepository personRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private SecurityService securityService;

    private AutoCloseable closeable;

    @BeforeEach void setUp() { closeable = MockitoAnnotations.openMocks(this); }
    @AfterEach void tearDown() throws Exception { closeable.close(); }

    @Test
    void testLogin_AdminSuccess() {
        String email = "admin@example.com";
        String raw = "Password1!";
        String hash = "$2a$10$hashed";

        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setEmail(email);
        person.setPassword(hash);
        person.setRole(PersonRole.ADMIN);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches(raw, hash)).thenReturn(true);
        when(jwtService.generateToken(email, "ADMIN")).thenReturn("admin-token");

        LoginResponse result = securityService.login(email, raw);

        assertTrue(result.success());
        assertEquals("ADMIN", result.role());
        assertEquals("admin-token", result.token());
        assertNull(result.errorMessage());
    }

    @Test
    void testLogin_CustomerSuccess() {
        String email = "customer@example.com";
        String raw = "Password1!";
        String hash = "$2a$10$hashed";

        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setEmail(email);
        person.setPassword(hash);
        person.setRole(PersonRole.CUSTOMER);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches(raw, hash)).thenReturn(true);
        when(jwtService.generateToken(email, "CUSTOMER")).thenReturn("customer-token");

        LoginResponse result = securityService.login(email, raw);

        assertTrue(result.success());
        assertEquals("CUSTOMER", result.role());
        assertEquals("customer-token", result.token());
    }

    @Test
    void testLogin_WrongPassword_ReturnsFalse() {
        String email = "john@example.com";
        String hash = "$2a$10$hashed";

        Person person = new Person();
        person.setEmail(email);
        person.setPassword(hash);
        person.setRole(PersonRole.CUSTOMER);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("wrongpassword", hash)).thenReturn(false);

        LoginResponse result = securityService.login(email, "wrongpassword");

        assertFalse(result.success());
        assertEquals("Incorrect password", result.errorMessage());
        assertNull(result.token());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void testLogin_EmailNotFound_ReturnsFalse() {
        String email = "missing@example.com";
        when(personRepository.findByEmail(email)).thenReturn(Optional.empty());

        LoginResponse result = securityService.login(email, "Password1!");

        assertFalse(result.success());
        assertTrue(result.errorMessage().contains(email));
        assertNull(result.token());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void testLogin_ReturnsPersonId() {
        UUID id = UUID.randomUUID();
        String email = "john@example.com";

        Person person = new Person();
        person.setId(id);
        person.setEmail(email);
        person.setPassword("hash");
        person.setRole(PersonRole.ADMIN);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        LoginResponse result = securityService.login(email, "Password1!");

        assertEquals(id, result.personId());
    }
}