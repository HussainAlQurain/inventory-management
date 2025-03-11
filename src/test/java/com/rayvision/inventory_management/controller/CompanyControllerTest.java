package com.rayvision.inventory_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayvision.inventory_management.controllers.CompanyController;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


import static org.mockito.ArgumentMatchers.any;


@WebMvcTest(CompanyController.class)
public class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyServiceImpl companyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createCompany_ShouldReturnCompany_WhenRequestIsValid() throws Exception {
        // Arrange
        Company company = Company.builder().name("AHAB").build();
        Mockito.when(companyService.save(any(Company.class))).thenReturn(company);

        // Act & Assert
        mockMvc.perform(post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(company)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AHAB"));
    }
}
