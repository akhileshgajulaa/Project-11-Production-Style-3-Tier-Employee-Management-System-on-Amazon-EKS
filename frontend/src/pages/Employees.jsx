import React, { useEffect, useState, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PageLayout from '../components/PageLayout';
import Pagination from '../components/Pagination';
import { getEmployees, deactivateEmployee } from '../api/employeeApi';
import { getDepartments } from '../api/departmentApi';
import { useAuth } from '../context/AuthContext';

export default function Employees() {
  const [employees, setEmployees] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const { isAdmin } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    getDepartments().then(setDepartments).catch(() => {});
  }, []);

  const fetchEmployees = useCallback(() => {
    setLoading(true);
    setError('');
    getEmployees({
      keyword: keyword || undefined,
      departmentId: departmentId || undefined,
      status: status || undefined,
      page,
      size: 10,
      sortBy: 'id',
      sortDir: 'desc',
    })
      .then((data) => {
        setEmployees(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch(() => setError('Unable to load employees.'))
      .finally(() => setLoading(false));
  }, [keyword, departmentId, status, page]);

  useEffect(() => {
    fetchEmployees();
  }, [fetchEmployees]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchEmployees();
  };

  const handleDeactivate = async (id, name) => {
    if (!window.confirm(`Deactivate ${name}? They will be marked inactive but not deleted.`)) return;
    setActionError('');
    try {
      await deactivateEmployee(id);
      fetchEmployees();
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Unable to deactivate employee.');
    }
  };

  return (
    <PageLayout title="Employees">
      <div className="page-header">
        <h1>Employees</h1>
        <Link to="/employees/new" className="btn btn-primary">+ Add Employee</Link>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {actionError && <div className="alert alert-danger">{actionError}</div>}

      <div className="card">
        <form className="toolbar" onSubmit={handleSearchSubmit}>
          <input
            className="form-input"
            style={{ maxWidth: 260 }}
            placeholder="Search by name, email, code, title…"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <div className="toolbar-filters">
            <select
              className="form-select"
              value={departmentId}
              onChange={(e) => { setDepartmentId(e.target.value); setPage(0); }}
            >
              <option value="">All Departments</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
            <select
              className="form-select"
              value={status}
              onChange={(e) => { setStatus(e.target.value); setPage(0); }}
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
            <button type="submit" className="btn btn-secondary">Search</button>
          </div>
        </form>

        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Employee ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Department</th>
                <th>Job Title</th>
                <th>Joining Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={9} className="text-muted">Loading…</td></tr>
              )}
              {!loading && employees.length === 0 && (
                <tr><td colSpan={9} className="text-muted">No employees found.</td></tr>
              )}
              {!loading && employees.map((emp) => (
                <tr key={emp.id}>
                  <td>{emp.employeeCode}</td>
                  <td>{emp.firstName} {emp.lastName}</td>
                  <td>{emp.email}</td>
                  <td>{emp.phone}</td>
                  <td>{emp.departmentName}</td>
                  <td>{emp.jobTitle}</td>
                  <td>{emp.joiningDate}</td>
                  <td>
                    <span className={`status-pill ${emp.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}`}>
                      {emp.status}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button className="btn btn-secondary btn-sm" onClick={() => navigate(`/employees/${emp.id}`)}>View</button>
                      <button className="btn btn-secondary btn-sm" onClick={() => navigate(`/employees/${emp.id}/edit`)}>Edit</button>
                      {isAdmin && emp.status === 'ACTIVE' && (
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => handleDeactivate(emp.id, `${emp.firstName} ${emp.lastName}`)}
                        >
                          Deactivate
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="text-muted" style={{ fontSize: 13, marginTop: 10 }}>
          {totalElements} employee{totalElements === 1 ? '' : 's'} total
        </div>

        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
    </PageLayout>
  );
}
