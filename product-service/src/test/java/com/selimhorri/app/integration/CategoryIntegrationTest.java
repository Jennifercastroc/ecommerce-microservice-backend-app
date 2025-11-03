package com.selimhorri.app.integration;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.service.CategoryService;
import com.selimhorri.app.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@Rollback
class CategoryIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testSaveAndFindById() {
        Category parent = createParentCategory("Integration Parent");
        CategoryDto request = buildCategoryDto("Integration Child", parent);

        CategoryDto saved = categoryService.save(request);
        assertNotNull(saved.getCategoryId());
        assertEquals(parent.getCategoryId(), saved.getParentCategoryDto().getCategoryId());

        CategoryDto found = categoryService.findById(saved.getCategoryId());
        assertEquals(saved.getCategoryId(), found.getCategoryId());
        assertEquals(parent.getCategoryId(), found.getParentCategoryDto().getCategoryId());
    }


    private Category createParentCategory(String title) {
        return categoryRepository.save(Category.builder()
                .categoryTitle(title)
                .imageUrl("http://example.com/" + UUID.randomUUID() + ".png")
                .build());
    }

    private CategoryDto buildCategoryDto(String title, Category parent) {
        CategoryDto parentDto = CategoryDto.builder()
                .categoryId(parent.getCategoryId())
                .categoryTitle(parent.getCategoryTitle())
                .imageUrl(parent.getImageUrl())
                .build();

        return CategoryDto.builder()
                .categoryTitle(title)
                .imageUrl("http://example.com/" + UUID.randomUUID() + ".png")
                .parentCategoryDto(parentDto)
                .build();
    }
}

