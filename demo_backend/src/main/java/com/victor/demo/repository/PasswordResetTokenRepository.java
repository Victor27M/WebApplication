package com.victor.demo.repository;

import com.victor.demo.model.PasswordResetToken;
import com.victor.demo.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByPersonAndCodeAndUsedFalseAndExpiresAtAfter(
            Person person, String code, LocalDateTime now);

    void deleteByPerson(Person person);
}