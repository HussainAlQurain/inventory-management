package com.rayvision.inventory_management.mappers;

public interface Mapper<E, D> {
    D mapTo(E entity);
    E mapFrom(D dto);
}
