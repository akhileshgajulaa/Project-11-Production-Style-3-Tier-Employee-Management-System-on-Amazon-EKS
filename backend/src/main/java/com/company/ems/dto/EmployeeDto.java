package com.company.ems.dto;

import com.company.ems.entity.EmployeeStatus;
import com.company.ems.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO used for both request payloads (create/update) and response payloads
 * for a single employee. Keeping one shape simplifies the frontend forms
 * while still validating on write.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {

    private Long id;

    @Size(max = 20)
    private String employeeCode; // ignored on create; generated server-side

    @NotBlank(message = "First name is required")
    @Size(max = 60)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 60)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\-\\s]{7,15}$", message = "Phone number must be 7-15 digits, optionally starting with +")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    @NotBlank(message = "Job title is required")
    @Size(max = 100)
    private String jobTitle;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String departmentName;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", message = "Salary cannot be negative")
    private BigDecimal salary;

    private EmployeeStatus status;

    @Size(max = 255)
    private String address;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
