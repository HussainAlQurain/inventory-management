package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PrepItemLocationDTO {
    private Long id;
    private Long subRecipeId;      // references SubRecipe
    private Long locationId;       // references Location

    private Double onHand;
    private Double minOnHand;
    private Double par;
    private Double lastCount;
    // If you want a date as well:
    private LocalDate lastCountDate;
}
