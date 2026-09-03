package com.company.ems.repository;

import com.company.ems.entity.Employee;
import com.company.ems.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmail(String email);
    boolean existsByEmployeeCode(String employeeCode);
    Optional<Employee> findByEmail(String email);

    long countByStatus(EmployeeStatus status);

    @Query("select e from Employee e where e.joiningDate >= :since order by e.joiningDate desc")
    List<Employee> findRecentlyJoined(@Param("since") LocalDate since);

    @Query("select d.name as department, count(e) as total from Employee e join e.department d group by d.name")
    List<Object[]> countGroupedByDepartment();

    @Query("select coalesce(max(cast(substring(e.employeeCode, 4) as int)), 0) from Employee e")
    Integer findMaxEmployeeCodeSuffix();
}
