package com.selimhorri.app.util;

import com.selimhorri.app.domain.Product;
import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;

public class ProductUtil {


    public static Product getSampleProduct() {
        Category category = getSampleCategory();
        return Product.builder()
                .productId(1)
                .productTitle("Laptop Dell XPS 13")
                .imageUrl("https://example.com/laptop.png")
                .sku("SKU12345")
                .priceUnit(1200.0)
                .quantity(5)
                .category(category)
                .build();
    }

    public static Category getSampleCategory() {
        return Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("https://example.com/category.png")
                .build();
    }

    public static ProductDto getSampleProductDto() {
        CategoryDto categoryDto = getSampleCategoryDto();
        return ProductDto.builder()
                .productId(1)
                .productTitle("Laptop Dell XPS 13")
                .imageUrl("https://example.com/laptop.png")
                .sku("SKU12345")
                .priceUnit(1200.0)
                .quantity(5)
                .categoryDto(categoryDto)
                .build();
    }

    public static CategoryDto getSampleCategoryDto() {
        return CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("https://example.com/category.png")
                .build();
    }
}
