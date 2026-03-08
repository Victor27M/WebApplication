package com.victor.demo.controller;

import com.victor.demo.model.Person;
import com.victor.demo.repository.PersonRepository;
import com.victor.demo.service.JwtService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class PersonControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private PersonRepository personRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String customerToken;

    private static final String FIXTURE_PATH = "src/test/resources/fixtures/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        personRepository.deleteAll();
        personRepository.flush();

        adminToken = jwtService.generateToken("admin@test.com", "ADMIN");
        customerToken = jwtService.generateToken("customer@test.com", "CUSTOMER");

        seedDatabase();
    }

    private void seedDatabase() throws Exception {
        String json = loadFixture("person_seed.json");
        List<Person> people = objectMapper.readValue(json, new TypeReference<>() {});
        people.forEach(p -> p.setPassword(passwordEncoder.encode(p.getPassword())));
        personRepository.saveAll(people);
    }

    // ── GET all ──────────────────────────────────────────────────────────────

    @Test
    void testGetPeople_ReturnsSeededData() throws Exception {
        mockMvc.perform(get("/person").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", Matchers.containsInAnyOrder("John Doe", "Jane Doe")));
    }

    @Test
    void testGetPeople_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/person"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetPeople_WithCustomerToken_Returns403() throws Exception {
        mockMvc.perform(get("/person").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // ── GET by id ────────────────────────────────────────────────────────────

    @Test
    void testGetPersonById_Found() throws Exception {
        Person existing = personRepository.findAll().getFirst();

        mockMvc.perform(get("/person/" + existing.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId().toString()));
    }

    @Test
    void testGetPersonById_NotFound() throws Exception {
        mockMvc.perform(get("/person/" + UUID.randomUUID()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
    }

    // ── GET by email ─────────────────────────────────────────────────────────

    @Test
    void testGetPersonByEmail_Found() throws Exception {
        mockMvc.perform(get("/person/email/john.doe@example.com").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void testGetPersonByEmail_NotFound() throws Exception {
        mockMvc.perform(get("/person/email/nobody@example.com").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
    }

    // ── POST ────────────────────────────────────────────────────────────────

    @Test
    void testAddPerson_ValidPayload() throws Exception {
        String json = loadFixture("valid_person.json");

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.password").value(Matchers.startsWith("$2a$")))
                .andExpect(jsonPath("$.age").value(28))
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"));
    }

    @Test
    void testAddPerson_InvalidPayload_Returns400() throws Exception {
        String json = loadFixture("invalid_person.json");

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddPerson_DuplicateEmail_Returns400() throws Exception {
        String json = """
                {
                  "name": "Duplicate",
                  "password": "Password1!",
                  "age": 30,
                  "email": "john.doe@example.com"
                }
                """;

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddPerson_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT ─────────────────────────────────────────────────────────────────

    @Test
    void testUpdatePerson_ValidPayload() throws Exception {
        Person existing = personRepository.findAll().getFirst();

        String json = """
                {
                  "name": "Updated Name",
                  "password": "NewPass1!",
                  "age": 40,
                  "email": "updated@example.com"
                }
                """;

        mockMvc.perform(put("/person/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.age").value(40))
                .andExpect(jsonPath("$.password").value(Matchers.startsWith("$2a$")));
    }

    @Test
    void testUpdatePerson_NotFound_Returns400() throws Exception {
        String json = """
                { "name": "Valid Name", "password": "Password1!", "age": 30, "email": "valid@example.com" }
                """;

        mockMvc.perform(put("/person/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH ───────────────────────────────────────────────────────────────

    @Test
    void testPatchPerson_NameOnly() throws Exception {
        Person existing = personRepository.findAll().getFirst();

        mockMvc.perform(patch("/person/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Patched Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Patched Name"));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void testDeletePerson_Returns200() throws Exception {
        Person existing = personRepository.findAll().getFirst();

        mockMvc.perform(delete("/person/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDeletePerson_NonExistent_Returns200() throws Exception {
        mockMvc.perform(delete("/person/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String loadFixture(String fileName) throws IOException {
        return Files.readString(Paths.get(FIXTURE_PATH + fileName));
    }
}