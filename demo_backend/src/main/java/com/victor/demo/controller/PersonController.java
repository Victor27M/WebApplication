package com.victor.demo.controller;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.PersonCreateDTO;
import com.victor.demo.model.PersonPatchDTO;
import com.victor.demo.service.PersonService;
import com.victor.demo.model.Person;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@AllArgsConstructor
@CrossOrigin
public class PersonController {
    private final PersonService personService;

    @GetMapping("/person")
    public List<Person> getPeople() {
        return personService.getPeople();
    }

    @GetMapping("/person/{uuid}")
    public Person getPersonById(@PathVariable UUID uuid) {
        return personService.getPersonById(uuid);
    }

    @GetMapping("/person/email/{email}")
    public Person getPersonByEmail(@PathVariable String email) {
        return personService.getPersonByEmail(email);
    }

    @PostMapping("/person")
    @ResponseStatus(HttpStatus.CREATED)          // also add this — POST should return 201
    public Person addPerson(@Valid @RequestBody PersonCreateDTO personDTO) throws ValidationException {
        return personService.addPerson(personDTO);
    }

    @PutMapping("/person/{uuid}")
    public Person updatePerson(@PathVariable UUID uuid,
                               @Valid @RequestBody PersonCreateDTO dto)
            throws ValidationException {
        return personService.updatePerson(uuid, dto);
    }

    @DeleteMapping("/person/{uuid}")
    public void deletePerson(@PathVariable UUID uuid) {
        personService.deletePerson(uuid);
    }

    @PatchMapping("/person/{uuid}")
    public Person patchPerson(@PathVariable UUID uuid,
                              @Valid @RequestBody PersonPatchDTO dto)
            throws ValidationException {
        return personService.patchPerson(uuid, dto);
    }
}
