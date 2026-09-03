import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageLayout from '../components/PageLayout';
import EmployeeForm from '../components/EmployeeForm';
import { createEmployee } from '../api/employeeApi';

export default function AddEmployee() {
  const [serverError, setServerError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (payload) => {
    setServerError('');
    try {
      const created = await createEmployee(payload);
      navigate(`/employees/${created.id}`);
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Unable to create employee.');
    }
  };

  return (
    <PageLayout title="Add Employee">
      <div className="page-header">
        <h1>Add Employee</h1>
      </div>
      <EmployeeForm onSubmit={handleSubmit} submitLabel="Create Employee" serverError={serverError} />
    </PageLayout>
  );
}
