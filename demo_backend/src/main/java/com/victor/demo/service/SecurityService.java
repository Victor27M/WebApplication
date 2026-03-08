package com.victor.demo.service;

import com.victor.demo.model.LoginResponse;
import com.victor.demo.model.Person;
import com.victor.demo.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(String email, String password) {
        Optional<Person> maybePerson = personRepository.findByEmail(email);
        if (maybePerson.isEmpty()) {
            return new LoginResponse(false, null, null, null,
                    "Person with email " + email + " not found");
        }

        Person person = maybePerson.get();
        if (!passwordEncoder.matches(password, person.getPassword())) {
            return new LoginResponse(false, null, null, null,
                    "Incorrect password");
        }

        String token = jwtService.generateToken(email, person.getRole().name());
        return new LoginResponse(true, person.getRole().name(), person.getId(), token, null);
    }
}