import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PageLayout from '../components/PageLayout';
import EmployeeForm from '../components/EmployeeForm';
import { getEmployeeById, updateEmployee } from '../api/employeeApi';

export default function EditEmployee() {
  const { id } = useParams();
  const [employee, setEmployee] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [serverError, setServerError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    getEmployeeById(id)
      .then(setEmployee)
      .catch(() => setLoadError('Unable to load employee details.'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleSubmit = async (payload) => {
    setServerError('');
    try {
      await updateEmployee(id, payload);
      navigate(`/employees/${id}`);
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Unable to update employee.');
    }
  };

  return (
    <PageLayout title="Edit Employee">
      <div className="page-header">
        <h1>Edit Employee</h1>
      </div>
      {loading && <div className="spinner-wrap">Loading…</div>}
      {loadError && <div className="alert alert-danger">{loadError}</div>}
      {employee && (
        <EmployeeForm
          initialValues={employee}
          onSubmit={handleSubmit}
          submitLabel="Save Changes"
          serverError={serverError}
        />
      )}
    </PageLayout>
  );
}
