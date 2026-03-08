package com.victor.demo.service;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.*;
import com.victor.demo.repository.OrderRepository;
import com.victor.demo.repository.PersonRepository;
import com.victor.demo.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTests {

    @Mock private OrderRepository orderRepository;
    @Mock private PersonRepository personRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderService orderService;

    private AutoCloseable closeable;

    @BeforeEach void setUp() { closeable = MockitoAnnotations.openMocks(this); }
    @AfterEach void tearDown() throws Exception { closeable.close(); }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Person makePerson(UUID id) {
        Person p = new Person();
        p.setId(id);
        p.setEmail("test@example.com");
        p.setPassword("hash");
        p.setRole(PersonRole.CUSTOMER);
        return p;
    }

    private Product makeProduct(UUID id, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName("Product-" + id);
        p.setPrice(10.0);
        p.setStock(stock);
        return p;
    }

    private Order makeOrder(UUID id, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setStatus(status);
        o.setItems(new ArrayList<>());
        return o;
    }

    private OrderItemDTO makeItemDTO(UUID productId, int qty) {
        OrderItemDTO d = new OrderItemDTO();
        d.setProductId(productId);
        d.setQuantity(qty);
        return d;
    }

    // ── getOrders ─────────────────────────────────────────────────────────────

    @Test
    void testGetOrders_ReturnsList() {
        when(orderRepository.findAll()).thenReturn(List.of(makeOrder(UUID.randomUUID(), OrderStatus.PENDING)));

        assertEquals(1, orderService.getOrders().size());
        verify(orderRepository).findAll();
    }

    @Test
    void testGetOrders_Empty() {
        when(orderRepository.findAll()).thenReturn(List.of());

        assertTrue(orderService.getOrders().isEmpty());
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    void testGetOrderById_Found() {
        UUID id = UUID.randomUUID();
        Order o = makeOrder(id, OrderStatus.PENDING);
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));

        assertEquals(id, orderService.getOrderById(id).getId());
    }

    @Test
    void testGetOrderById_NotFound_ThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> orderService.getOrderById(id));
    }

    // ── getOrdersByPerson ─────────────────────────────────────────────────────

    @Test
    void testGetOrdersByPerson_ReturnsList() {
        UUID personId = UUID.randomUUID();
        when(orderRepository.findByPersonId(personId)).thenReturn(
                List.of(makeOrder(UUID.randomUUID(), OrderStatus.PENDING)));

        assertEquals(1, orderService.getOrdersByPerson(personId).size());
    }

    @Test
    void testGetOrdersByPerson_Empty() {
        UUID personId = UUID.randomUUID();
        when(orderRepository.findByPersonId(personId)).thenReturn(List.of());

        assertTrue(orderService.getOrdersByPerson(personId).isEmpty());
    }

    // ── addOrder ──────────────────────────────────────────────────────────────

    @Test
    void testAddOrder_HappyPath() throws ValidationException {
        UUID personId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Person person = makePerson(personId);
        Product product = makeProduct(productId, 20);

        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setDestination("Cluj");
        dto.setItems(List.of(makeItemDTO(productId, 2)));

        Order saved = makeOrder(UUID.randomUUID(), OrderStatus.PENDING);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenReturn(saved);

        Order result = orderService.addOrder(dto);

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(orderRepository).save(any());
    }

    @Test
    void testAddOrder_PersonNotFound_ThrowsValidationException() {
        UUID personId = UUID.randomUUID();
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setItems(List.of(makeItemDTO(UUID.randomUUID(), 1)));

        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> orderService.addOrder(dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testAddOrder_ProductNotFound_ThrowsValidationException() {
        UUID personId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setItems(List.of(makeItemDTO(productId, 1)));
        dto.setDestination("Cluj");

        when(personRepository.findById(personId)).thenReturn(Optional.of(makePerson(personId)));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> orderService.addOrder(dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testAddOrder_EmptyItems_ThrowsValidationException() {
        UUID personId = UUID.randomUUID();
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setItems(List.of());
        dto.setDestination("Cluj");

        when(personRepository.findById(personId)).thenReturn(Optional.of(makePerson(personId)));

        assertThrows(ValidationException.class, () -> orderService.addOrder(dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testAddOrder_NullItems_ThrowsValidationException() {
        UUID personId = UUID.randomUUID();
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setItems(null);

        when(personRepository.findById(personId)).thenReturn(Optional.of(makePerson(personId)));

        assertThrows(ValidationException.class, () -> orderService.addOrder(dto));
    }

    @Test
    void testAddOrder_ShippedStatus_DeductsStock() throws ValidationException {
        UUID personId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Product product = makeProduct(productId, 10);

        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setStatus(OrderStatus.SHIPPED);
        dto.setItems(List.of(makeItemDTO(productId, 3)));
        dto.setDestination("Cluj");

        Order saved = makeOrder(UUID.randomUUID(), OrderStatus.SHIPPED);

        when(personRepository.findById(personId)).thenReturn(Optional.of(makePerson(personId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(saved);

        orderService.addOrder(dto);

        // stock deducted: 10 - 3 = 7
        assertEquals(7, product.getStock());
        verify(productRepository).save(product);
    }

    @Test
    void testAddOrder_InsufficientStock_ThrowsValidationException() {
        UUID personId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Product product = makeProduct(productId, 2); // only 2 in stock

        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setStatus(OrderStatus.SHIPPED);
        dto.setItems(List.of(makeItemDTO(productId, 5))); // requesting 5
        dto.setDestination("Cluj");

        when(personRepository.findById(personId)).thenReturn(Optional.of(makePerson(personId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class, () -> orderService.addOrder(dto));
    }

    // ── updateOrder ───────────────────────────────────────────────────────────

    @Test
    void testUpdateOrder_HappyPath() throws ValidationException {
        UUID orderId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order existing = makeOrder(orderId, OrderStatus.PENDING);
        Product product = makeProduct(productId, 20);

        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPersonId(personId);
        dto.setItems(List.of(makeItemDTO(productId, 1)));
        dto.setDestination("Bucuresti");
        dto.setStatus(OrderStatus.CONFIRMED);

        Order updated = makeOrder(orderId, OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        when(personRepository.findById(personId)).thenReturn(Optional.of(makePerson(personId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenReturn(updated);

        Order result = orderService.updateOrder(orderId, dto);

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void testUpdateOrder_AlreadyDelivered_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(makeOrder(orderId, OrderStatus.DELIVERED)));

        assertThrows(ValidationException.class, () -> orderService.updateOrder(orderId, new OrderCreateDTO()));
    }

    @Test
    void testUpdateOrder_AlreadyCancelled_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(makeOrder(orderId, OrderStatus.CANCELLED)));

        assertThrows(ValidationException.class, () -> orderService.updateOrder(orderId, new OrderCreateDTO()));
    }

    @Test
    void testUpdateOrder_NotFound_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> orderService.updateOrder(orderId, new OrderCreateDTO()));
    }

    // ── patchOrder ────────────────────────────────────────────────────────────

    @Test
    void testPatchOrder_StatusOnly() throws ValidationException {
        UUID orderId = UUID.randomUUID();
        Order existing = makeOrder(orderId, OrderStatus.PENDING);

        OrderPatchDTO patch = new OrderPatchDTO();
        patch.setStatus(OrderStatus.CONFIRMED);

        Order saved = makeOrder(orderId, OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenReturn(saved);

        Order result = orderService.patchOrder(orderId, patch);

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void testPatchOrder_DestinationOnly() throws ValidationException {
        UUID orderId = UUID.randomUUID();
        Order existing = makeOrder(orderId, OrderStatus.PENDING);

        OrderPatchDTO patch = new OrderPatchDTO();
        patch.setDestination("Timisoara");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenReturn(existing);

        orderService.patchOrder(orderId, patch);

        assertEquals("Timisoara", existing.getDestination());
    }

    @Test
    void testPatchOrder_AlreadyDelivered_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(makeOrder(orderId, OrderStatus.DELIVERED)));

        assertThrows(ValidationException.class, () -> orderService.patchOrder(orderId, new OrderPatchDTO()));
    }

    @Test
    void testPatchOrder_AlreadyCancelled_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(makeOrder(orderId, OrderStatus.CANCELLED)));

        assertThrows(ValidationException.class, () -> orderService.patchOrder(orderId, new OrderPatchDTO()));
    }

    @Test
    void testPatchOrder_NotFound_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> orderService.patchOrder(orderId, new OrderPatchDTO()));
    }

    // ── deleteOrder ───────────────────────────────────────────────────────────

    @Test
    void testDeleteOrder_HappyPath() throws ValidationException {
        UUID orderId = UUID.randomUUID();
        Order order = makeOrder(orderId, OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderRepository).deleteById(orderId);

        orderService.deleteOrder(orderId);

        verify(orderRepository).deleteById(orderId);
    }

    @Test
    void testDeleteOrder_NotFound_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> orderService.deleteOrder(orderId));
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteOrder_Shipped_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(makeOrder(orderId, OrderStatus.SHIPPED)));

        assertThrows(ValidationException.class, () -> orderService.deleteOrder(orderId));
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteOrder_Delivered_ThrowsValidationException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(makeOrder(orderId, OrderStatus.DELIVERED)));

        assertThrows(ValidationException.class, () -> orderService.deleteOrder(orderId));
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteOrder_Cancelled_DeletesSuccessfully() throws ValidationException {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(makeOrder(orderId, OrderStatus.CANCELLED)));
        doNothing().when(orderRepository).deleteById(orderId);

        orderService.deleteOrder(orderId);

        verify(orderRepository).deleteById(orderId);
    }
}