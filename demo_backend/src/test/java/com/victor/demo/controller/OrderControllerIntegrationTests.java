package com.victor.demo.controller;

import com.victor.demo.model.*;
import com.victor.demo.repository.OrderRepository;
import com.victor.demo.repository.PersonRepository;
import com.victor.demo.repository.ProductRepository;
import com.victor.demo.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class OrderControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private JwtService jwtService;

    private String adminToken;
    private String customerToken;
    private UUID personId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        personRepository.deleteAll();

        adminToken = jwtService.generateToken("admin@test.com", "ADMIN");
        customerToken = jwtService.generateToken("customer@test.com", "CUSTOMER");

        Person person = new Person();
        person.setName("Test Person");
        person.setEmail("order.test@example.com");
        person.setPassword("$2a$10$hash");
        person.setAge(25);
        person.setRole(PersonRole.CUSTOMER);
        personId = personRepository.save(person).getId();

        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("desc");
        product.setPrice(10.0);
        product.setStock(100);
        productId = productRepository.save(product).getId();
    }

    // ── GET all ──────────────────────────────────────────────────────────────

    @Test
    void testGetOrders_EmptyList() throws Exception {
        mockMvc.perform(get("/order").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetOrders_WithOrders() throws Exception {
        createTestOrder(OrderStatus.PENDING);

        mockMvc.perform(get("/order").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetOrders_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/order")).andExpect(status().isUnauthorized());
    }

    @Test
    void testGetOrders_WithCustomerToken_Returns403() throws Exception {
        mockMvc.perform(get("/order").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // ── GET by person ─────────────────────────────────────────────────────────

    @Test
    void testGetOrdersByPerson_ReturnsOnlyThatPersonsOrders() throws Exception {
        createTestOrder(OrderStatus.PENDING);

        mockMvc.perform(get("/order/person/" + personId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetOrdersByPerson_UnknownPerson_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/order/person/" + UUID.randomUUID()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST ────────────────────────────────────────────────────────────────

    @Test
    void testAddOrder_ValidPayload_Returns201() throws Exception {
        String json = """
                {
                  "personId": "%s",
                  "items": [{ "productId": "%s", "quantity": 2 }],
                  "destination": "Cluj-Napoca"
                }
                """.formatted(personId, productId);

        mockMvc.perform(post("/order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testAddOrder_PersonNotFound_Returns400() throws Exception {
        String json = """
                {
                  "personId": "%s",
                  "items": [{ "productId": "%s", "quantity": 1 }],
                  "destination": "Cluj"
                }
                """.formatted(UUID.randomUUID(), productId);

        mockMvc.perform(post("/order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddOrder_ProductNotFound_Returns400() throws Exception {
        String json = """
                {
                  "personId": "%s",
                  "items": [{ "productId": "%s", "quantity": 1 }],
                  "destination": "Cluj"
                }
                """.formatted(personId, UUID.randomUUID());

        mockMvc.perform(post("/order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddOrder_EmptyItems_Returns400() throws Exception {
        String json = """
                {
                  "personId": "%s",
                  "items": [],
                  "destination": "Cluj"
                }
                """.formatted(personId);

        mockMvc.perform(post("/order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddOrder_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT ─────────────────────────────────────────────────────────────────

    @Test
    void testUpdateOrder_ValidPayload() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.PENDING);

        String json = """
                {
                  "personId": "%s",
                  "items": [{ "productId": "%s", "quantity": 3 }],
                  "destination": "Bucuresti",
                  "status": "CONFIRMED"
                }
                """.formatted(personId, productId);

        mockMvc.perform(put("/order/" + orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void testUpdateOrder_AlreadyDelivered_Returns400() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.DELIVERED);

        String json = """
                {
                  "personId": "%s",
                  "items": [{ "productId": "%s", "quantity": 1 }],
                  "destination": "Cluj",
                  "status": "CONFIRMED"
                }
                """.formatted(personId, productId);

        mockMvc.perform(put("/order/" + orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateOrder_AlreadyCancelled_Returns400() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.CANCELLED);

        String json = """
                {
                  "personId": "%s",
                  "items": [{ "productId": "%s", "quantity": 1 }],
                  "destination": "Cluj",
                  "status": "CONFIRMED"
                }
                """.formatted(personId, productId);

        mockMvc.perform(put("/order/" + orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateOrder_NotFound_Returns400() throws Exception {
        String json = """
                {
                  "personId": "%s",
                  "items": [{ "productId": "%s", "quantity": 1 }],
                  "destination": "Cluj",
                  "status": "CONFIRMED"
                }
                """.formatted(personId, productId);

        mockMvc.perform(put("/order/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH ───────────────────────────────────────────────────────────────

    @Test
    void testPatchOrder_StatusOnly() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.PENDING);

        mockMvc.perform(patch("/order/" + orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void testPatchOrder_NotFound_Returns400() throws Exception {
        mockMvc.perform(patch("/order/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void testDeleteOrder_Returns204() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.PENDING);

        mockMvc.perform(delete("/order/" + orderId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteOrder_NotFound_Returns400() throws Exception {
        mockMvc.perform(delete("/order/" + UUID.randomUUID()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteOrder_AlreadyShipped_Returns400() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.SHIPPED);

        mockMvc.perform(delete("/order/" + orderId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteOrder_AlreadyDelivered_Returns400() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.DELIVERED);

        mockMvc.perform(delete("/order/" + orderId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteOrder_Cancelled_Returns204() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.CANCELLED);

        mockMvc.perform(delete("/order/" + orderId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteOrder_WithoutToken_Returns401() throws Exception {
        UUID orderId = createTestOrder(OrderStatus.PENDING);

        mockMvc.perform(delete("/order/" + orderId)).andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UUID createTestOrder(OrderStatus status) {
        Order order = new Order();
        order.setPerson(personRepository.findById(personId).orElseThrow());
        order.setDestination("Cluj-Napoca");
        order.setStatus(status);
        order.setOrderDate(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        return orderRepository.save(order).getId();
    }
}