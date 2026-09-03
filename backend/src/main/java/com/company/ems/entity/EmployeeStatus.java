package com.company.ems.entity;

/**
 * Lifecycle status of an employee record. Employees are soft-deleted
 * by moving them to INACTIVE rather than physically removing rows,
 * which preserves history and referential integrity.
 */
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE
}
