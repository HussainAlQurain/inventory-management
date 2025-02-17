package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.dto.CategoryCreateDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper extends AbstractMapper<Category, CategoryCreateDTO>{
    public CategoryMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected Class<Category> getEntityClass() {
        return Category.class;
    }

    @Override
    protected Class<CategoryCreateDTO> getDtoClass() {
        return CategoryCreateDTO.class;
    }

}
