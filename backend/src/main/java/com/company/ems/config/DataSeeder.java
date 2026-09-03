package com.company.ems.config;

import com.company.ems.entity.*;
import com.company.ems.repository.DepartmentRepository;
import com.company.ems.repository.EmployeeRepository;
import com.company.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Seeds baseline reference and demo data on first startup only (idempotent —
 * checks for existing rows before inserting). This lets the whole stack be
 * demoed immediately after `docker compose up` with no manual data entry.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        Map<String, Department> departments = seedDepartments();
        seedEmployees(departments);
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already present, skipping user seed.");
            return;
        }

        // NOTE: these are demo-only credentials for local/dev use.
        // In any real environment, rotate these immediately after first login
        // and manage the password hash exclusively via the database / Secret,
        // never via source control.
        User admin = User.builder()
                .username("admin")
                .email("admin@company.local")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        User hr = User.builder()
                .username("hruser")
                .email("hr@company.local")
                .password(passwordEncoder.encode("Hr@12345"))
                .role(Role.HR_USER)
                .enabled(true)
                .build();

        userRepository.save(admin);
        userRepository.save(hr);
        log.info("Seeded default users: admin / hruser");
    }

    private Map<String, Department> seedDepartments() {
        if (departmentRepository.count() > 0) {
            log.info("Departments already present, skipping department seed.");
            return departmentRepository.findAll().stream()
                    .collect(java.util.stream.Collectors.toMap(Department::getName, d -> d));
        }

        List<Department> departments = List.of(
                Department.builder().name("Engineering").description("Product development and platform engineering").build(),
                Department.builder().name("Human Resources").description("Talent acquisition, onboarding and employee relations").build(),
                Department.builder().name("Finance").description("Accounting, payroll and financial planning").build(),
                Department.builder().name("Sales").description("Customer acquisition and account management").build(),
                Department.builder().name("Marketing").description("Brand, growth and product marketing").build()
        );

        List<Department> saved = departmentRepository.saveAll(departments);
        log.info("Seeded {} departments", saved.size());
        return saved.stream().collect(java.util.stream.Collectors.toMap(Department::getName, d -> d));
    }

    private void seedEmployees(Map<String, Department> departments) {
        if (employeeRepository.count() > 0) {
            log.info("Employees already present, skipping employee seed.");
            return;
        }

        record Seed(String code, String first, String last, String email, String phone, LocalDate dob,
                    Gender gender, String title, String dept, LocalDate joined, String salary,
                    EmployeeStatus status, String address) {}

        List<Seed> seeds = List.of(
                new Seed("EMP1001", "Aditi", "Sharma", "aditi.sharma@company.com", "+91-9876543210",
                        LocalDate.of(1992, 4, 12), Gender.FEMALE, "Senior Software Engineer", "Engineering",
                        LocalDate.of(2021, 6, 1), "1450000.00", EmployeeStatus.ACTIVE, "Hyderabad, India"),
                new Seed("EMP1002", "Rahul", "Verma", "rahul.verma@company.com", "+91-9876543211",
                        LocalDate.of(1990, 11, 3), Gender.MALE, "DevOps Engineer", "Engineering",
                        LocalDate.of(2022, 1, 15), "1350000.00", EmployeeStatus.ACTIVE, "Bengaluru, India"),
                new Seed("EMP1003", "Sneha", "Reddy", "sneha.reddy@company.com", "+91-9876543212",
                        LocalDate.of(1995, 7, 22), Gender.FEMALE, "HR Business Partner", "Human Resources",
                        LocalDate.of(2020, 3, 10), "980000.00", EmployeeStatus.ACTIVE, "Hyderabad, India"),
                new Seed("EMP1004", "Karthik", "Iyer", "karthik.iyer@company.com", "+91-9876543213",
                        LocalDate.of(1988, 2, 18), Gender.MALE, "Finance Manager", "Finance",
                        LocalDate.of(2019, 9, 1), "1650000.00", EmployeeStatus.ACTIVE, "Chennai, India"),
                new Seed("EMP1005", "Priya", "Nair", "priya.nair@company.com", "+91-9876543214",
                        LocalDate.of(1993, 9, 30), Gender.FEMALE, "Sales Executive", "Sales",
                        LocalDate.of(2023, 2, 20), "820000.00", EmployeeStatus.ACTIVE, "Kochi, India"),
                new Seed("EMP1006", "Arjun", "Mehta", "arjun.mehta@company.com", "+91-9876543215",
                        LocalDate.of(1991, 5, 14), Gender.MALE, "Marketing Specialist", "Marketing",
                        LocalDate.of(2022, 7, 11), "760000.00", EmployeeStatus.ACTIVE, "Mumbai, India"),
                new Seed("EMP1007", "Divya", "Krishnan", "divya.krishnan@company.com", "+91-9876543216",
                        LocalDate.of(1994, 12, 5), Gender.FEMALE, "QA Engineer", "Engineering",
                        LocalDate.of(2021, 11, 8), "980000.00", EmployeeStatus.ACTIVE, "Hyderabad, India"),
                new Seed("EMP1008", "Vikram", "Singh", "vikram.singh@company.com", "+91-9876543217",
                        LocalDate.of(1987, 3, 25), Gender.MALE, "Engineering Manager", "Engineering",
                        LocalDate.of(2018, 4, 2), "2200000.00", EmployeeStatus.ACTIVE, "Pune, India"),
                new Seed("EMP1009", "Anjali", "Gupta", "anjali.gupta@company.com", "+91-9876543218",
                        LocalDate.of(1996, 8, 19), Gender.FEMALE, "Recruiter", "Human Resources",
                        LocalDate.of(2023, 5, 29), "700000.00", EmployeeStatus.ACTIVE, "Delhi, India"),
                new Seed("EMP1010", "Suresh", "Kumar", "suresh.kumar@company.com", "+91-9876543219",
                        LocalDate.of(1989, 1, 9), Gender.MALE, "Accountant", "Finance",
                        LocalDate.of(2020, 10, 19), "650000.00", EmployeeStatus.INACTIVE, "Hyderabad, India"),
                new Seed("EMP1011", "Neha", "Joshi", "neha.joshi@company.com", "+91-9876543220",
                        LocalDate.of(1997, 6, 27), Gender.FEMALE, "Frontend Developer", "Engineering",
                        LocalDate.of(2024, 1, 8), "1100000.00", EmployeeStatus.ACTIVE, "Bengaluru, India"),
                new Seed("EMP1012", "Manoj", "Pillai", "manoj.pillai@company.com", "+91-9876543221",
                        LocalDate.of(1990, 10, 2), Gender.MALE, "Sales Manager", "Sales",
                        LocalDate.of(2019, 6, 17), "1500000.00", EmployeeStatus.ACTIVE, "Chennai, India")
        );

        List<Employee> employees = seeds.stream().map(s -> Employee.builder()
                .employeeCode(s.code())
                .firstName(s.first())
                .lastName(s.last())
                .email(s.email())
                .phone(s.phone())
                .dateOfBirth(s.dob())
                .gender(s.gender())
                .jobTitle(s.title())
                .department(departments.get(s.dept()))
                .joiningDate(s.joined())
                .salary(new BigDecimal(s.salary()))
                .status(s.status())
                .address(s.address())
                .build()
        ).toList();

        employeeRepository.saveAll(employees);
        log.info("Seeded {} employees", employees.size());
    }
}
