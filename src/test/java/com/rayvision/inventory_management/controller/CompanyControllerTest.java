package com.rayvision.inventory_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayvision.inventory_management.controllers.CompanyController;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(CompanyController.class)
public class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyService companyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createCompany_ShouldReturnCompany_WhenRequestIsValid() throws Exception {
        // Arrange
        Company company = Company.builder().name("AHAB").id(1L).build();
        Mockito.when(companyService.createCompany(any(Company.class))).thenReturn(company);
    }
}
