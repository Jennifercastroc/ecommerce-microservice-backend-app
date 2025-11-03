package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "server.servlet.context-path=/product-service"
        })
class ProductControllerTest {

    private static final String BASE_PATH = "/product-service/api/products";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                System.err.println("Response error: " + body);
                super.handleError(response);
            }
        });
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void shouldReturnAllProducts() {
        Category category = categoryRepository.save(Category.builder()
                .categoryTitle("Laptops")
                .imageUrl("http://example.com/laptops.png")
                .build());

        Product persistedProduct = productRepository.save(Product.builder()
                .productTitle("Ultrabook X1")
                .imageUrl("http://example.com/x1.png")
                .sku("SKU-" + UUID.randomUUID())
                .priceUnit(1299.99)
                .quantity(5)
                .category(category)
                .build());

        ResponseEntity<DtoCollectionResponse<ProductDto>> response = restTemplate.exchange(
                baseUrl(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<DtoCollectionResponse<ProductDto>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCollection());
        assertTrue(response.getBody().getCollection().stream()
                .anyMatch(dto -> persistedProduct.getSku().equals(dto.getSku())));
    }

    @Test
    void shouldCreateProduct() {
        Category category = categoryRepository.save(Category.builder()
                .categoryTitle("Accessories")
                .imageUrl("http://example.com/accessories.png")
                .build());

        ProductDto requestPayload = ProductDto.builder()
                .productTitle("Mechanical Keyboard")
                .imageUrl("http://example.com/keyboard.png")
                .sku("SKU-" + UUID.randomUUID())
                .priceUnit(199.99)
                .quantity(15)
                .categoryDto(CategoryDto.builder()
                        .categoryId(category.getCategoryId())
                        .categoryTitle(category.getCategoryTitle())
                        .imageUrl(category.getImageUrl())
                        .build())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ProductDto> response = restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                new HttpEntity<>(requestPayload, headers),
                ProductDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getProductId());
        assertEquals(requestPayload.getSku(), response.getBody().getSku());

        assertThat(productRepository.findById(response.getBody().getProductId())).isPresent();
    }

    private String baseUrl() {
        return "http://localhost:" + port + BASE_PATH;
    }
}
