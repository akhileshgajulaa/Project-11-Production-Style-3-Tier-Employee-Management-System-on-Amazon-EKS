package com.company.ems.controller;

import com.company.ems.entity.*;
import com.company.ems.repository.DepartmentRepository;
import com.company.ems.repository.EmployeeRepository;
import com.company.ems.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering employee creation, retrieval, update,
 * deactivation, validation and authorization rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Department department;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();

        department = departmentRepository.save(Department.builder()
                .name("Engineering")
                .description("Engineering department")
                .build());

        userRepository.save(User.builder()
                .username("hruser")
                .email("hr@company.local")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.HR_USER)
                .enabled(true)
                .build());
    }

    private Map<String, Object> validEmployeePayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", "Test");
        payload.put("lastName", "User");
        payload.put("email", "test.user@company.com");
        payload.put("phone", "+91-9999999999");
        payload.put("jobTitle", "Software Engineer");
        payload.put("departmentId", department.getId());
        payload.put("joiningDate", LocalDate.now().minusDays(1).toString());
        payload.put("salary", 900000);
        return payload;
    }

    @Test
    void listEmployees_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR_USER"})
    void createEmployee_withValidData_returns201AndGeneratesCode() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validEmployeePayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.email").value("test.user@company.com"));
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR_USER"})
    void createEmployee_withInvalidEmail_returns400() throws Exception {
        Map<String, Object> payload = validEmployeePayload();
        payload.put("email", "not-an-email");

        mockMvc.perform(post("/api/employees")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR_USER"})
    void createEmployee_withNegativeSalary_returns400() throws Exception {
        Map<String, Object> payload = validEmployeePayload();
        payload.put("salary", -500);

        mockMvc.perform(post("/api/employees")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR_USER"})
    void createEmployee_withDuplicateEmail_returns409() throws Exception {
        Employee existing = Employee.builder()
                .employeeCode("EMP9999")
                .firstName("Existing")
                .lastName("Person")
                .email("test.user@company.com")
                .phone("+91-8888888888")
                .jobTitle("Engineer")
                .department(department)
                .joiningDate(LocalDate.now().minusYears(1))
                .salary(new BigDecimal("500000"))
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeRepository.save(existing);

        mockMvc.perform(post("/api/employees")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validEmployeePayload())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR_USER"})
    void getEmployeeById_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/employees/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR_USER"})
    void updateEmployee_changesFields() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeCode("EMP8888")
                .firstName("Old")
                .lastName("Name")
                .email("old.name@company.com")
                .phone("+91-7777777777")
                .jobTitle("Engineer")
                .department(department)
                .joiningDate(LocalDate.now().minusYears(1))
                .salary(new BigDecimal("500000"))
                .status(EmployeeStatus.ACTIVE)
                .build());

        Map<String, Object> payload = validEmployeePayload();
        payload.put("email", "old.name@company.com");
        payload.put("firstName", "Updated");

        mockMvc.perform(put("/api/employees/" + saved.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR_USER"})
    void deactivateEmployee_asHrUser_returns403() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeCode("EMP7777")
                .firstName("ToDeactivate")
                .lastName("Person")
                .email("todeactivate@company.com")
                .phone("+91-6666666666")
                .jobTitle("Engineer")
                .department(department)
                .joiningDate(LocalDate.now().minusYears(1))
                .salary(new BigDecimal("500000"))
                .status(EmployeeStatus.ACTIVE)
                .build());

        // HR_USER is not permitted to delete/deactivate — only ADMIN can.
        mockMvc.perform(delete("/api/employees/" + saved.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deactivateEmployee_asAdmin_setsStatusInactive() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeCode("EMP6666")
                .firstName("ToDeactivate")
                .lastName("Person")
                .email("todeactivate2@company.com")
                .phone("+91-5555555555")
                .jobTitle("Engineer")
                .department(department)
                .joiningDate(LocalDate.now().minusYears(1))
                .salary(new BigDecimal("500000"))
                .status(EmployeeStatus.ACTIVE)
                .build());

        mockMvc.perform(delete("/api/employees/" + saved.getId()))
                .andExpect(status().isNoContent());

        Employee reloaded = employeeRepository.findById(saved.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(EmployeeStatus.INACTIVE, reloaded.getStatus());
    }
}
