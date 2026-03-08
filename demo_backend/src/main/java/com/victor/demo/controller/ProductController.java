package com.victor.demo.controller;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.Product;
import com.victor.demo.model.ProductCreateDTO;
import com.victor.demo.model.ProductPatchDTO;
import com.victor.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{uuid}")
    public Product getProductById(@PathVariable UUID uuid) {
        return productService.getProductById(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product addProduct(@Valid @RequestBody ProductCreateDTO dto) throws ValidationException {
        return productService.addProduct(dto);
    }

    @PutMapping("/{uuid}")
    public Product updateProduct(@PathVariable UUID uuid,
                                 @Valid @RequestBody ProductCreateDTO dto) throws ValidationException {
        return productService.updateProduct(uuid, dto);
    }

    @PatchMapping("/{uuid}")
    public Product patchProduct(@PathVariable UUID uuid,
                                @Valid @RequestBody ProductPatchDTO patch) throws ValidationException {
        return productService.patchProduct(uuid, patch);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID uuid) throws ValidationException {
        productService.deleteProduct(uuid);
    }
}