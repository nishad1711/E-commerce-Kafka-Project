package com.Ecommerce.product_service.service;

import com.Ecommerce.product_service.dto.ProductRequest;
import com.Ecommerce.product_service.entity.Product;
import com.Ecommerce.product_service.repository.ProductRepository;
import com.Ecommerce.product_service.repository.StockReleaseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockReleaseRepository stockReleaseRepository;

    public ProductService(
            ProductRepository productRepository,
            StockReleaseRepository stockReleaseRepository
    ) {
        this.productRepository = productRepository;
        this.stockReleaseRepository = stockReleaseRepository;
    }

    @Transactional
    public void reserveStock(Long productId, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        int updated = productRepository.reserveStock(productId, quantity);

        if (updated == 0) {
            throw new RuntimeException(
                    "Product not found or insufficient stock"
            );
        }
    }

    @Transactional
    public void releaseStock(
            Long productId,
            int quantity,
            String operationKey
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        if (operationKey == null || operationKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Operation key is required"
            );
        }

        int recorded = stockReleaseRepository.recordIfAbsent(operationKey);

        if (recorded == 0) {
            System.out.println(
                    "Duplicate stock release ignored: " + operationKey
            );
            return;
        }

        int updated = productRepository.releaseStock(productId, quantity);

        if (updated == 0) {
            throw new RuntimeException("Product not found");
        }
    }

    public Product createProduct(ProductRequest request) {

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(request.category())
                .build();

        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));
    }

    public Product updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }

        productRepository.deleteById(id);
    }
}