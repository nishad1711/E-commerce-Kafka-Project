package com.Ecommerce.product_service;

import com.Ecommerce.product_service.dto.ProductRequest;
import com.Ecommerce.product_service.entity.Product;
import com.Ecommerce.product_service.service.ProductService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_product_db",
        "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
        "app.jwt.secret=product-service-test-secret-key-2026-0123456789",
        "app.internal-service.token=local-test-token",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Value("${app.jwt.secret}")
    private String jwtSecret;

    @MockitoBean
    private ProductService productService;

    @Test
    void publicProductListShouldReturnOk() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    @Test
    void createProductWithoutTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userShouldNotCreateProduct() throws Exception {
        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + generateToken("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldCreateProduct() throws Exception {
        Product product = Product.builder()
                .name("Test Product")
                .description("Test description")
                .price(100.0)
                .stock(10)
                .category("Test")
                .build();

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(product);

        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + generateToken("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson()))
                .andExpect(status().isOk());
    }

    @Test
    void stockReservationWithoutInternalTokenShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(post("/products/1/reserve-stock")
                        .param("quantity", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stockReservationWithInternalTokenShouldReturnNoContent()
            throws Exception {

        mockMvc.perform(post("/products/1/reserve-stock")
                        .param("quantity", "1")
                        .header("X-Internal-Service-Token", "local-test-token"))
                .andExpect(status().isNoContent());
    }

    private String productJson() {
        return """
                {
                    "name": "Test Product",
                    "description": "Test description",
                    "price": 100.0,
                    "stock": 10,
                    "category": "Test"
                }
                """;
    }

    private String generateToken(String role) {
        return Jwts.builder()
                .subject("test@example.com")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(
                        jwtSecret.getBytes(StandardCharsets.UTF_8)
                ))
                .compact();
    }

}
