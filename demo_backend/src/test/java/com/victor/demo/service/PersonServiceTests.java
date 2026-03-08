package com.victor.demo.service;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.Person;
import com.victor.demo.model.PersonCreateDTO;
import com.victor.demo.model.PersonPatchDTO;
import com.victor.demo.model.PersonRole;
import com.victor.demo.repository.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonServiceTests {

    @Mock private PersonRepository personRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private PersonService personService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hashed_" + inv.getArgument(0));
    }

    @AfterEach
    void tearDown() throws Exception { closeable.close(); }

    // ── getpeople ────────────────────────────────────────────────────────────

    @Test
    void testGetPeople_ReturnsList() {
        List<Person> people = List.of(new Person(), new Person());
        when(personRepository.findAll()).thenReturn(people);

        List<Person> result = personService.getPeople();

        assertEquals(2, result.size());
        verify(personRepository, times(1)).findAll();
    }

    @Test
    void testGetPeople_Empty_ReturnsEmptyList() {
        when(personRepository.findAll()).thenReturn(List.of());

        List<Person> result = personService.getPeople();

        assertTrue(result.isEmpty());
    }

    // ── getPerson by id/email ────────────────────────────────────────────────

    @Test
    void testGetPersonById_Found() {
        UUID id = UUID.randomUUID();
        Person person = new Person();
        person.setId(id);
        when(personRepository.findById(id)).thenReturn(Optional.of(person));

        Person result = personService.getPersonById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void testGetPersonById_NotFound_ThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> personService.getPersonById(id));
    }

    @Test
    void testGetPersonByEmail_Found() {
        Person person = new Person();
        person.setEmail("test@example.com");
        when(personRepository.findByEmail("test@example.com")).thenReturn(Optional.of(person));

        Person result = personService.getPersonByEmail("test@example.com");

        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetPersonByEmail_NotFound_ThrowsIllegalState() {
        when(personRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> personService.getPersonByEmail("missing@example.com"));
    }

    // ── addPerson ────────────────────────────────────────────────────────────

    @Test
    void testAddPerson_HappyPath() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("John");
        dto.setPassword("Password1!");
        dto.setAge(30);
        dto.setEmail("john@example.com");
        dto.setRole(PersonRole.ADMIN);

        Person saved = new Person();
        saved.setId(UUID.randomUUID());
        saved.setName("John");
        saved.setEmail("john@example.com");
        saved.setPassword("hashed_Password1!");
        saved.setRole(PersonRole.ADMIN);

        when(personRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenReturn(saved);

        Person result = personService.addPerson(dto);

        assertNotNull(result.getId());
        assertEquals("John", result.getName());
        assertEquals(PersonRole.ADMIN, result.getRole());
        verify(passwordEncoder).encode("Password1!");
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void testAddPerson_DefaultsToCustomerRole() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("Jane");
        dto.setPassword("Password1!");
        dto.setAge(25);
        dto.setEmail("jane@example.com");
        // no role set

        Person saved = new Person();
        saved.setId(UUID.randomUUID());
        saved.setRole(PersonRole.CUSTOMER);

        when(personRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenReturn(saved);

        Person result = personService.addPerson(dto);

        assertEquals(PersonRole.CUSTOMER, result.getRole());
    }

    @Test
    void testAddPerson_DuplicateEmail_ThrowsValidationException() {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setEmail("john@example.com");

        when(personRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class, () -> personService.addPerson(dto));
        verify(personRepository, never()).save(any());
    }

    @Test
    void testAddPerson_PasswordIsHashed() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("John");
        dto.setPassword("Plaintext1!");
        dto.setAge(30);
        dto.setEmail("john@example.com");

        Person saved = new Person();
        saved.setPassword("hashed_Plaintext1!");

        when(personRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(personRepository.save(any())).thenReturn(saved);

        Person result = personService.addPerson(dto);

        assertEquals("hashed_Plaintext1!", result.getPassword());
    }

    // ── updatePerson ─────────────────────────────────────────────────────────

    @Test
    void testUpdatePerson_HappyPath() throws ValidationException {
        UUID id = UUID.randomUUID();
        Person existing = new Person();
        existing.setId(id);
        existing.setEmail("old@example.com");
        existing.setPassword("hashed_old");
        existing.setRole(PersonRole.CUSTOMER);

        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("Jane");
        dto.setAge(25);
        dto.setEmail("jane@example.com");
        dto.setPassword("NewPass1!");

        Person updated = new Person();
        updated.setId(id);
        updated.setName("Jane");

        when(personRepository.findById(id)).thenReturn(Optional.of(existing));
        when(personRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(personRepository.save(any())).thenReturn(updated);

        Person result = personService.updatePerson(id, dto);

        assertEquals("Jane", result.getName());
        verify(passwordEncoder).encode("NewPass1!");
        verify(personRepository).save(any());
    }

    @Test
    void testUpdatePerson_SameEmail_DoesNotCheckDuplicate() throws ValidationException {
        UUID id = UUID.randomUUID();
        Person existing = new Person();
        existing.setId(id);
        existing.setEmail("same@example.com");

        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("Same");
        dto.setAge(30);
        dto.setEmail("same@example.com"); // same email
        dto.setPassword("Pass1!");

        when(personRepository.findById(id)).thenReturn(Optional.of(existing));
        when(personRepository.save(any())).thenReturn(existing);

        assertDoesNotThrow(() -> personService.updatePerson(id, dto));
        verify(personRepository, never()).findByEmail(any());
    }

    @Test
    void testUpdatePerson_DuplicateEmail_ThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(id);
        existing.setEmail("old@example.com");

        Person other = new Person();
        other.setId(otherId);
        other.setEmail("taken@example.com");

        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setEmail("taken@example.com");
        dto.setPassword("Pass1!");

        when(personRepository.findById(id)).thenReturn(Optional.of(existing));
        when(personRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        assertThrows(IllegalStateException.class, () -> personService.updatePerson(id, dto));
        verify(personRepository, never()).save(any());
    }

    @Test
    void testUpdatePerson_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> personService.updatePerson(id, new PersonCreateDTO()));
    }

    // ── patchPerson ──────────────────────────────────────────────────────────

    @Test
    void testPatchPerson_NameOnly() throws ValidationException {
        UUID id = UUID.randomUUID();
        Person existing = new Person();
        existing.setId(id);
        existing.setName("Old");
        existing.setEmail("test@example.com");

        PersonPatchDTO patch = new PersonPatchDTO();
        patch.setName("New");

        Person saved = new Person();
        saved.setName("New");

        when(personRepository.findById(id)).thenReturn(Optional.of(existing));
        when(personRepository.save(any())).thenReturn(saved);

        Person result = personService.patchPerson(id, patch);

        assertEquals("New", result.getName());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void testPatchPerson_PasswordIsHashed() throws ValidationException {
        UUID id = UUID.randomUUID();
        Person existing = new Person();
        existing.setId(id);
        existing.setEmail("test@example.com");

        PersonPatchDTO patch = new PersonPatchDTO();
        patch.setPassword("NewPass1!");

        when(personRepository.findById(id)).thenReturn(Optional.of(existing));
        when(personRepository.save(any())).thenReturn(existing);

        personService.patchPerson(id, patch);

        verify(passwordEncoder).encode("NewPass1!");
    }

    @Test
    void testPatchPerson_DuplicateEmail_ThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(id);
        existing.setEmail("mine@example.com");

        Person other = new Person();
        other.setId(otherId);
        other.setEmail("taken@example.com");

        PersonPatchDTO patch = new PersonPatchDTO();
        patch.setEmail("taken@example.com");

        when(personRepository.findById(id)).thenReturn(Optional.of(existing));
        when(personRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        assertThrows(IllegalStateException.class, () -> personService.patchPerson(id, patch));
    }

    @Test
    void testPatchPerson_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> personService.patchPerson(id, new PersonPatchDTO()));
    }

    // ── deletePerson ─────────────────────────────────────────────────────────

    @Test
    void testDeletePerson_CallsRepository() {
        UUID id = UUID.randomUUID();
        doNothing().when(personRepository).deleteById(id);

        personService.deletePerson(id);

        verify(personRepository, times(1)).deleteById(id);
    }
}