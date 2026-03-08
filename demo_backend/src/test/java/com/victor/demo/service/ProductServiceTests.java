package com.victor.demo.service;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.Product;
import com.victor.demo.model.ProductCreateDTO;
import com.victor.demo.model.ProductPatchDTO;
import com.victor.demo.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTests {

    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductService productService;

    private AutoCloseable closeable;

    @BeforeEach void setUp() { closeable = MockitoAnnotations.openMocks(this); }
    @AfterEach void tearDown() throws Exception { closeable.close(); }

    // ── getProducts ──────────────────────────────────────────────────────────

    @Test
    void testGetProducts_ReturnsList() {
        when(productRepository.findAll()).thenReturn(List.of(new Product(), new Product()));

        assertEquals(2, productService.getProducts().size());
        verify(productRepository).findAll();
    }

    @Test
    void testGetProducts_Empty() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertTrue(productService.getProducts().isEmpty());
    }

    // ── getProductById ───────────────────────────────────────────────────────

    @Test
    void testGetProductById_Found() {
        UUID id = UUID.randomUUID();
        Product p = new Product();
        p.setId(id);
        when(productRepository.findById(id)).thenReturn(Optional.of(p));

        assertEquals(id, productService.getProductById(id).getId());
    }

    @Test
    void testGetProductById_NotFound_ThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> productService.getProductById(id));
    }

    // ── addProduct ───────────────────────────────────────────────────────────

    @Test
    void testAddProduct_HappyPath() throws ValidationException {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("Laptop");
        dto.setDescription("Fast laptop");
        dto.setPrice(999.99);
        dto.setStock(10);

        Product saved = new Product();
        saved.setId(UUID.randomUUID());
        saved.setName("Laptop");
        saved.setPrice(999.99);
        saved.setStock(10);

        when(productRepository.findByName("Laptop")).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenReturn(saved);

        Product result = productService.addProduct(dto);

        assertNotNull(result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(999.99, result.getPrice());
        verify(productRepository).save(any());
    }

    @Test
    void testAddProduct_DuplicateName_ThrowsValidationException() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("Laptop");
        when(productRepository.findByName("Laptop")).thenReturn(Optional.of(new Product()));

        assertThrows(ValidationException.class, () -> productService.addProduct(dto));
        verify(productRepository, never()).save(any());
    }

    // ── updateProduct ────────────────────────────────────────────────────────

    @Test
    void testUpdateProduct_HappyPath() throws ValidationException {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setName("Old");

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("New");
        dto.setPrice(50.0);
        dto.setStock(5);

        Product updated = new Product();
        updated.setId(id);
        updated.setName("New");

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.findByName("New")).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenReturn(updated);

        Product result = productService.updateProduct(id, dto);

        assertEquals("New", result.getName());
        verify(productRepository).save(any());
    }

    @Test
    void testUpdateProduct_SameName_DoesNotThrow() throws ValidationException {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setName("Laptop");

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("Laptop"); // same name, same product
        dto.setPrice(100.0);
        dto.setStock(3);

        Product sameProduct = new Product();
        sameProduct.setId(id); // same id → not a duplicate

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.findByName("Laptop")).thenReturn(Optional.of(sameProduct));
        when(productRepository.save(any())).thenReturn(existing);

        assertDoesNotThrow(() -> productService.updateProduct(id, dto));
    }

    @Test
    void testUpdateProduct_DuplicateName_ThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Product existing = new Product();
        existing.setId(id);
        existing.setName("Old");

        Product other = new Product();
        other.setId(otherId);
        other.setName("Taken");

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("Taken");
        dto.setPrice(10.0);
        dto.setStock(1);

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.findByName("Taken")).thenReturn(Optional.of(other));

        assertThrows(IllegalStateException.class, () -> productService.updateProduct(id, dto));
        verify(productRepository, never()).save(any());
    }

    @Test
    void testUpdateProduct_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> productService.updateProduct(id, new ProductCreateDTO()));
    }

    // ── patchProduct ─────────────────────────────────────────────────────────

    @Test
    void testPatchProduct_PriceOnly() throws ValidationException {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setName("Laptop");
        existing.setPrice(500.0);
        existing.setStock(10);

        ProductPatchDTO patch = new ProductPatchDTO();
        patch.setPrice(299.0);

        Product saved = new Product();
        saved.setId(id);
        saved.setPrice(299.0);

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(saved);

        Product result = productService.patchProduct(id, patch);

        assertEquals(299.0, result.getPrice());
    }

    @Test
    void testPatchProduct_StockOnly() throws ValidationException {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setStock(5);

        ProductPatchDTO patch = new ProductPatchDTO();
        patch.setStock(99);

        Product saved = new Product();
        saved.setStock(99);

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(saved);

        assertEquals(99, productService.patchProduct(id, patch).getStock());
    }

    @Test
    void testPatchProduct_NameDuplicate_ThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Product existing = new Product();
        existing.setId(id);
        existing.setName("Old");

        Product other = new Product();
        other.setId(otherId);
        other.setName("Taken");

        ProductPatchDTO patch = new ProductPatchDTO();
        patch.setName("Taken");

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.findByName("Taken")).thenReturn(Optional.of(other));

        assertThrows(IllegalStateException.class, () -> productService.patchProduct(id, patch));
    }

    @Test
    void testPatchProduct_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> productService.patchProduct(id, new ProductPatchDTO()));
    }

    // ── deleteProduct ────────────────────────────────────────────────────────

    @Test
    void testDeleteProduct_HappyPath() throws ValidationException {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);
        doNothing().when(productRepository).deleteById(id);

        productService.deleteProduct(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    void testDeleteProduct_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        assertThrows(ValidationException.class, () -> productService.deleteProduct(id));
        verify(productRepository, never()).deleteById(any());
    }
}