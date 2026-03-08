package com.victor.demo.service;

import com.victor.demo.model.PasswordResetToken;
import com.victor.demo.model.Person;
import com.victor.demo.repository.PasswordResetTokenRepository;
import com.victor.demo.repository.PersonRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PersonRepository personRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void requestReset(String email) {
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with email: " + email));

        tokenRepository.deleteByPerson(person);

        String code = String.format("%06d", new Random().nextInt(1_000_000));

        PasswordResetToken token = new PasswordResetToken();
        token.setPerson(person);
        token.setCode(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        emailService.sendPasswordResetCode(email, code);
    }

    @Transactional
    public void confirmReset(String email, String code, String newPassword) {
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with email: " + email));

        PasswordResetToken token = tokenRepository
                .findByPersonAndCodeAndUsedFalseAndExpiresAtAfter(person, code, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code"));

        person.setPassword(passwordEncoder.encode(newPassword));
        personRepository.save(person);

        token.setUsed(true);
        tokenRepository.save(token);

        emailService.sendPasswordChangeConfirmation(email);
    }
}