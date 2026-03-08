package com.victor.demo.controller;

import com.victor.demo.model.Product;
import com.victor.demo.repository.ProductRepository;
import com.victor.demo.service.JwtService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
public class ProductControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private JwtService jwtService;

    private String adminToken;
    private String customerToken;

    private static final String FIXTURE_PATH = "src/test/resources/fixtures/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        productRepository.flush();

        adminToken = jwtService.generateToken("admin@test.com", "ADMIN");
        customerToken = jwtService.generateToken("customer@test.com", "CUSTOMER");

        String json = loadFixture("product_seed.json");
        List<Product> products = objectMapper.readValue(json, new TypeReference<>() {});
        productRepository.saveAll(products);
    }

    // ── GET all ──────────────────────────────────────────────────────────────

    @Test
    void testGetProducts_ReturnsSeeded() throws Exception {
        mockMvc.perform(get("/product").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", Matchers.containsInAnyOrder("Laptop", "Mouse")));
    }

    @Test
    void testGetProducts_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/product")).andExpect(status().isUnauthorized());
    }

    @Test
    void testGetProducts_WithCustomerToken_Returns200() throws Exception {
        mockMvc.perform(get("/product").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    // ── GET by id ────────────────────────────────────────────────────────────

    @Test
    void testGetProductById_Found() throws Exception {
        Product existing = productRepository.findAll().getFirst();

        mockMvc.perform(get("/product/" + existing.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId().toString()));
    }

    @Test
    void testGetProductById_NotFound_Returns409() throws Exception {
        mockMvc.perform(get("/product/" + UUID.randomUUID()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    // ── POST ────────────────────────────────────────────────────────────────

    @Test
    void testAddProduct_ValidPayload() throws Exception {
        String json = loadFixture("valid_product.json");

        mockMvc.perform(post("/product")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(89.99))
                .andExpect(jsonPath("$.stock").value(30));
    }

    @Test
    void testAddProduct_InvalidPayload_Returns400() throws Exception {
        String json = loadFixture("invalid_product.json");

        mockMvc.perform(post("/product")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.stock").exists());
    }

    @Test
    void testAddProduct_DuplicateName_Returns400() throws Exception {
        String json = """
                { "name": "Laptop", "price": 500.0, "stock": 5 }
                """;

        mockMvc.perform(post("/product")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddProduct_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT ─────────────────────────────────────────────────────────────────

    @Test
    void testUpdateProduct_ValidPayload() throws Exception {
        Product existing = productRepository.findAll().getFirst();

        String json = """
                { "name": "Updated Product", "description": "Updated", "price": 999.0, "stock": 3 }
                """;

        mockMvc.perform(put("/product/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.price").value(999.0));
    }

    @Test
    void testUpdateProduct_NotFound_Returns400() throws Exception {
        String json = """
                { "name": "Valid Name", "price": 10.0, "stock": 1 }
                """;

        mockMvc.perform(put("/product/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH ───────────────────────────────────────────────────────────────

    @Test
    void testPatchProduct_PriceOnly() throws Exception {
        Product existing = productRepository.findAll().getFirst();

        mockMvc.perform(patch("/product/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 1.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1.99));
    }

    @Test
    void testPatchProduct_NotFound_Returns400() throws Exception {
        mockMvc.perform(patch("/product/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 1.0}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void testDeleteProduct_Returns204() throws Exception {
        Product existing = productRepository.findAll().getFirst();

        mockMvc.perform(delete("/product/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteProduct_NotFound_Returns400() throws Exception {
        mockMvc.perform(delete("/product/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    private String loadFixture(String f) throws IOException {
        return Files.readString(Paths.get(FIXTURE_PATH + f));
    }
}