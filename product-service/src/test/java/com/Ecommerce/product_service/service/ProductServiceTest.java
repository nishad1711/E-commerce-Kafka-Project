package com.Ecommerce.product_service.service;

import com.Ecommerce.product_service.dto.ProductRequest;
import com.Ecommerce.product_service.entity.Product;
import com.Ecommerce.product_service.repository.ProductRepository;
import com.Ecommerce.product_service.repository.StockReleaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReleaseRepository stockReleaseRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest request() {
        return new ProductRequest(
                "Keyboard", "Mechanical keyboard",
                2499.0, 10, "Electronics"
        );
    }

    private Product product() {
        return Product.builder()
                .id(1L)
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(2499.0)
                .stock(10)
                .category("Electronics")
                .build();
    }

    @Test
    void reserveStockSuccessfully() {
        when(productRepository.reserveStock(1L, 2)).thenReturn(1);

        assertDoesNotThrow(() -> productService.reserveStock(1L, 2));

        verify(productRepository).reserveStock(1L, 2);
    }

    @Test
    void reserveStockRejectsZeroQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.reserveStock(1L, 0));

        verifyNoInteractions(productRepository);
    }

    @Test
    void reserveStockRejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.reserveStock(1L, -1));

        verifyNoInteractions(productRepository);
    }

    @Test
    void reserveStockRejectsInsufficientStock() {
        when(productRepository.reserveStock(1L, 20)).thenReturn(0);

        assertThrows(RuntimeException.class,
                () -> productService.reserveStock(1L, 20));
    }

    @Test
    void releaseStockSuccessfully() {
        when(stockReleaseRepository.recordIfAbsent("ORDER-1")).thenReturn(1);
        when(productRepository.releaseStock(1L, 2)).thenReturn(1);

        assertDoesNotThrow(
                () -> productService.releaseStock(1L, 2, "ORDER-1")
        );

        verify(productRepository).releaseStock(1L, 2);
    }

    @Test
    void duplicateStockReleaseIsIgnored() {
        when(stockReleaseRepository.recordIfAbsent("ORDER-1")).thenReturn(0);

        assertDoesNotThrow(
                () -> productService.releaseStock(1L, 2, "ORDER-1")
        );

        verify(productRepository, never()).releaseStock(anyLong(), anyInt());
    }

    @Test
    void releaseStockRejectsZeroQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.releaseStock(1L, 0, "ORDER-1"));

        verifyNoInteractions(stockReleaseRepository, productRepository);
    }

    @Test
    void releaseStockRejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.releaseStock(1L, -1, "ORDER-1"));

        verifyNoInteractions(stockReleaseRepository, productRepository);
    }

    @Test
    void releaseStockRejectsNullOperationKey() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.releaseStock(1L, 1, null));

        verifyNoInteractions(stockReleaseRepository, productRepository);
    }

    @Test
    void releaseStockRejectsBlankOperationKey() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.releaseStock(1L, 1, "  "));

        verifyNoInteractions(stockReleaseRepository, productRepository);
    }

    @Test
    void releaseStockThrowsWhenProductDoesNotExist() {
        when(stockReleaseRepository.recordIfAbsent("ORDER-1")).thenReturn(1);
        when(productRepository.releaseStock(99L, 2)).thenReturn(0);

        assertThrows(RuntimeException.class,
                () -> productService.releaseStock(99L, 2, "ORDER-1"));
    }

    @Test
    void createProductSuccessfully() {
        when(productRepository.save(any(Product.class))).thenReturn(product());

        Product result = productService.createProduct(request());

        assertEquals("Keyboard", result.getName());
        assertEquals(2499.0, result.getPrice());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getAllProductsReturnsProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product()));

        List<Product> result = productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals("Keyboard", result.get(0).getName());
    }

    @Test
    void getProductByIdReturnsProductWhenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));

        Product result = productService.getProductById(1L);

        assertEquals("Keyboard", result.getName());
    }

    @Test
    void getProductByIdThrowsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> productService.getProductById(99L));
    }

    @Test
    void updateProductSuccessfully() {
        Product existing = product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        Product result = productService.updateProduct(1L, request());

        assertEquals("Keyboard", result.getName());
        verify(productRepository).save(existing);
    }

    @Test
    void updateProductThrowsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> productService.updateProduct(99L, request()));

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProductSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProductThrowsWhenNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> productService.deleteProduct(99L));

        verify(productRepository, never()).deleteById(anyLong());
    }
}
