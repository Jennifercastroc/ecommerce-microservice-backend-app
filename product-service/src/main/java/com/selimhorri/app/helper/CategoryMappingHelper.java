package com.selimhorri.app.helper;

import java.util.Optional;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;

public interface CategoryMappingHelper {
	
	public static CategoryDto map(final Category category) {
		
		final Category parentCategory = category.getParentCategory();
		
		return CategoryDto.builder()
				.categoryId(category.getCategoryId())
				.categoryTitle(category.getCategoryTitle())
				.imageUrl(category.getImageUrl())
				.parentCategoryDto(Optional.ofNullable(parentCategory)
						.map(parent -> CategoryDto.builder()
								.categoryId(parent.getCategoryId())
								.categoryTitle(parent.getCategoryTitle())
								.imageUrl(parent.getImageUrl())
								.build())
						.orElse(null))
				.build();
	}
	
	public static Category map(final CategoryDto categoryDto) {
		
		final Category parentCategory = Optional.ofNullable(categoryDto.getParentCategoryDto())
				.filter(dto -> dto.getCategoryId() != null)
				.map(dto -> Category.builder()
						.categoryId(dto.getCategoryId())
						.categoryTitle(dto.getCategoryTitle())
						.imageUrl(dto.getImageUrl())
						.build())
				.orElse(null);
		
		return Category.builder()
				.categoryId(categoryDto.getCategoryId())
				.categoryTitle(categoryDto.getCategoryTitle())
				.imageUrl(categoryDto.getImageUrl())
				.parentCategory(parentCategory)
				.build();
	}
	
	
	
}










