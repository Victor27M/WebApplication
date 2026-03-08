package com.victor.demo.service;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.Product;
import com.victor.demo.model.ProductCreateDTO;
import com.victor.demo.model.ProductPatchDTO;
import com.victor.demo.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(UUID uuid) {
        return productRepository.findById(uuid).orElseThrow(
                () -> new IllegalStateException("Product with id " + uuid + " not found"));
    }

    public Product addProduct(ProductCreateDTO dto) throws ValidationException {
        if (productRepository.findByName(dto.getName()).isPresent()) {
            throw new ValidationException("A product with name '" + dto.getName() + "' already exists");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());

        return productRepository.save(product);
    }

    public Product updateProduct(UUID uuid, ProductCreateDTO dto) throws ValidationException {
        Product existing = productRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Product with id " + uuid + " not found"));

        productRepository.findByName(dto.getName())
                .filter(p -> !p.getId().equals(uuid))
                .ifPresent(p -> { throw new IllegalStateException("A product with name '" + dto.getName() + "' already exists"); });

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setStock(dto.getStock());

        return productRepository.save(existing);
    }

    public Product patchProduct(UUID uuid, ProductPatchDTO patch) throws ValidationException {
        Product existing = productRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Product with id " + uuid + " not found"));

        if (patch.getName() != null) {
            productRepository.findByName(patch.getName())
                    .filter(p -> !p.getId().equals(uuid))
                    .ifPresent(p -> { throw new IllegalStateException("A product with name '" + patch.getName() + "' already exists"); });
            existing.setName(patch.getName());
        }
        if (patch.getDescription() != null) {
            existing.setDescription(patch.getDescription());
        }
        if (patch.getPrice() != null) {
            existing.setPrice(patch.getPrice());
        }
        if (patch.getStock() != null) {
            existing.setStock(patch.getStock());
        }

        return productRepository.save(existing);
    }

    public void deleteProduct(UUID uuid) throws ValidationException {
        if (!productRepository.existsById(uuid)) {
            throw new ValidationException("Product with id " + uuid + " not found");
        }
        productRepository.deleteById(uuid);
    }
}