package com.victor.demo.service;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.Person;
import com.victor.demo.model.PersonCreateDTO;
import com.victor.demo.model.PersonPatchDTO;
import com.victor.demo.model.PersonRole;
import com.victor.demo.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Person> getPeople() {
        return personRepository.findAll();
    }

    public Person addPerson(PersonCreateDTO personDTO) throws ValidationException {
        if (personRepository.findByEmail(personDTO.getEmail()).isPresent()) {
            throw new ValidationException("A person with email '" + personDTO.getEmail() + "' already exists");
        }
        Person person = new Person();
        person.setName(personDTO.getName());
        person.setAge(personDTO.getAge());
        person.setEmail(personDTO.getEmail());
        person.setPassword(passwordEncoder.encode(personDTO.getPassword()));
        person.setRole(personDTO.getRole() != null ? personDTO.getRole() : PersonRole.CUSTOMER);
        return personRepository.save(person);
    }

    public Person updatePerson(UUID uuid, PersonCreateDTO dto) throws ValidationException {
        Person existing = personRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Person with id " + uuid + " not found"));

        if (!existing.getEmail().equals(dto.getEmail())) {
            personRepository.findByEmail(dto.getEmail())
                    .ifPresent(p -> { throw new IllegalStateException(
                            "A person with email '" + dto.getEmail() + "' already exists"); });
        }

        existing.setName(dto.getName());
        existing.setAge(dto.getAge());
        existing.setEmail(dto.getEmail());
        existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        existing.setRole(dto.getRole() != null ? dto.getRole() : existing.getRole());
        return personRepository.save(existing);
    }

    public Person patchPerson(UUID uuid, PersonPatchDTO dto) throws ValidationException {
        Person existing = personRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Person with id " + uuid + " not found"));

        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getPassword() != null) existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getAge() != null) existing.setAge(dto.getAge());
        if (dto.getEmail() != null) {
            personRepository.findByEmail(dto.getEmail())
                    .filter(p -> !p.getId().equals(uuid))
                    .ifPresent(p -> { throw new IllegalStateException(
                            "A person with email '" + dto.getEmail() + "' already exists"); });
            existing.setEmail(dto.getEmail());
        }
        if (dto.getRole() != null) existing.setRole(dto.getRole());
        return personRepository.save(existing);
    }

    public void deletePerson(UUID uuid) {
        personRepository.deleteById(uuid);
    }

    public Person getPersonByEmail(String email) {
        return personRepository.findByEmail(email).orElseThrow(
                () -> new IllegalStateException("Person with email " + email + " not found"));
    }

    public Person getPersonById(UUID uuid) {
        return personRepository.findById(uuid).orElseThrow(
                () -> new IllegalStateException("Person with id " + uuid + " not found"));
    }
}