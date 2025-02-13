package com.rayvision.inventory_management.mappers.impl;

import com.rayvision.inventory_management.mappers.Mapper;
import org.modelmapper.ModelMapper;

public abstract class AbstractMapper<E, D> implements Mapper<E, D> {
    protected ModelMapper modelMapper;
    public AbstractMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public D mapTo(E entity) {
        return modelMapper.map(entity, getDtoClass());
    }

    @Override
    public E mapFrom(D dto) {
        return modelMapper.map(dto, getEntityClass());
    }

    protected abstract Class<E> getEntityClass();
    protected abstract Class<D> getDtoClass();

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
