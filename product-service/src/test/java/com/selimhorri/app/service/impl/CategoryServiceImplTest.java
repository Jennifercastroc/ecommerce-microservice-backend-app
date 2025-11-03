package com.selimhorri.app.service.impl;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.util.ProductUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        category = ProductUtil.getSampleCategory();
        categoryDto = ProductUtil.getSampleCategoryDto();
    }

    @Test
    void testFindById_ShouldReturnCategoryDto() {
        when(categoryRepository.findById(category.getCategoryId())).thenReturn(Optional.of(category));

        CategoryDto result = categoryService.findById(category.getCategoryId());

        assertNotNull(result);
        assertEquals(categoryDto.getCategoryId(), result.getCategoryId());
        assertEquals(categoryDto.getCategoryTitle(), result.getCategoryTitle());
    }

    @Test
    void testFindAll_ShouldReturnCategoryList() {
        when(categoryRepository.findAll()).thenReturn(Collections.singletonList(category));

        List<CategoryDto> result = categoryService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getCategoryTitle());
    }


}
