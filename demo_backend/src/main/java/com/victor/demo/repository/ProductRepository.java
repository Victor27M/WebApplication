package com.victor.demo.repository;

import com.victor.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByName(String name);

    List<Product> findByPriceLessThanEqual(Double maxPrice);

    List<Product> findByStockGreaterThan(Integer minStock);
}