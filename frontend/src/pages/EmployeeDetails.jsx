import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import PageLayout from '../components/PageLayout';
import { getEmployeeById, deactivateEmployee } from '../api/employeeApi';
import { useAuth } from '../context/AuthContext';

export default function EmployeeDetails() {
  const { id } = useParams();
  const [employee, setEmployee] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const { isAdmin } = useAuth();
  const navigate = useNavigate();

  const load = () => {
    setLoading(true);
    getEmployeeById(id)
      .then(setEmployee)
      .catch(() => setError('Unable to load employee.'))
      .finally(() => setLoading(false));
  };

  useEffect(load, [id]);

  const handleDeactivate = async () => {
    if (!window.confirm('Deactivate this employee?')) return;
    try {
      await deactivateEmployee(id);
      load();
    } catch (err) {
      setError(err?.response?.data?.message || 'Unable to deactivate employee.');
    }
  };

  return (
    <PageLayout title="Employee Details">
      <div className="page-header">
        <h1>Employee Details</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          <Link to="/employees" className="btn btn-secondary">Back to List</Link>
          {employee && (
            <button className="btn btn-secondary" onClick={() => navigate(`/employees/${id}/edit`)}>Edit</button>
          )}
          {employee && isAdmin && employee.status === 'ACTIVE' && (
            <button className="btn btn-danger" onClick={handleDeactivate}>Deactivate</button>
          )}
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {loading && <div className="spinner-wrap">Loading…</div>}

      {employee && (
        <div className="card">
          <div className="page-header">
            <h1 style={{ fontSize: 18 }}>
              {employee.firstName} {employee.lastName}{' '}
              <span className={`status-pill ${employee.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}`}>
                {employee.status}
              </span>
            </h1>
          </div>

          <div className="detail-grid">
            <div className="detail-item"><div className="label">Employee ID</div><div className="value">{employee.employeeCode}</div></div>
            <div className="detail-item"><div className="label">Email</div><div className="value">{employee.email}</div></div>
            <div className="detail-item"><div className="label">Phone</div><div className="value">{employee.phone}</div></div>
            <div className="detail-item"><div className="label">Department</div><div className="value">{employee.departmentName}</div></div>
            <div className="detail-item"><div className="label">Job Title</div><div className="value">{employee.jobTitle}</div></div>
            <div className="detail-item"><div className="label">Joining Date</div><div className="value">{employee.joiningDate}</div></div>
            <div className="detail-item"><div className="label">Date of Birth</div><div className="value">{employee.dateOfBirth || '—'}</div></div>
            <div className="detail-item"><div className="label">Gender</div><div className="value">{employee.gender || '—'}</div></div>
            <div className="detail-item"><div className="label">Salary</div><div className="value">₹{Number(employee.salary).toLocaleString('en-IN')}</div></div>
            <div className="detail-item"><div className="label">Address</div><div className="value">{employee.address || '—'}</div></div>
          </div>
        </div>
      )}
    </PageLayout>
  );
}
