package com.rayvision.inventory_management;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    public CompanyServiceTest()
    {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCompany_ShouldReturnCompany_WhenNameIsProvided()
    {
        // Arrange
        Company company = Company.builder().id(1L).name("AHAB").build();
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        Company result = companyService.createCompany(company);

        // Assert
        assertNotNull(company);
        assertEquals("AHAB", company.getName());
        assertEquals(1, company.getId());
        verify(companyRepository, times(1)).save(any(Company.class));
    }
}
