import React, { useEffect, useState } from 'react';
import PageLayout from '../components/PageLayout';
import { getDepartments, createDepartment, updateDepartment, deleteDepartment } from '../api/departmentApi';
import { useAuth } from '../context/AuthContext';

const emptyForm = { name: '', description: '' };

export default function Departments() {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const { isAdmin } = useAuth();

  const load = () => {
    setLoading(true);
    getDepartments()
      .then(setDepartments)
      .catch(() => setError('Unable to load departments.'))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
    setFormError('');
  };

  const handleEdit = (dept) => {
    setForm({ name: dept.name, description: dept.description || '' });
    setEditingId(dept.id);
    setFormError('');
  };

  const handleDelete = async (dept) => {
    if (!window.confirm(`Delete department "${dept.name}"? This only works if it has no employees assigned.`)) return;
    try {
      await deleteDepartment(dept.id);
      load();
    } catch (err) {
      setError(err?.response?.data?.message || 'Unable to delete department.');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!form.name.trim()) {
      setFormError('Department name is required.');
      return;
    }
    try {
      if (editingId) {
        await updateDepartment(editingId, form);
      } else {
        await createDepartment(form);
      }
      resetForm();
      load();
    } catch (err) {
      setFormError(err?.response?.data?.message || 'Unable to save department.');
    }
  };

  return (
    <PageLayout title="Departments">
      <div className="page-header">
        <h1>Departments</h1>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {isAdmin && (
        <form className="card" onSubmit={handleSubmit} style={{ marginBottom: 20 }}>
          <div className="section-title">{editingId ? 'Edit Department' : 'Add New Department'}</div>
          {formError && <div className="alert alert-danger">{formError}</div>}
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Name *</label>
              <input
                className="form-input"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Description</label>
              <input
                className="form-input"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" className="btn btn-primary">
              {editingId ? 'Save Changes' : 'Add Department'}
            </button>
            {editingId && (
              <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancel</button>
            )}
          </div>
        </form>
      )}

      <div className="card">
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Employees</th>
                {isAdmin && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {loading && <tr><td colSpan={4} className="text-muted">Loading…</td></tr>}
              {!loading && departments.length === 0 && (
                <tr><td colSpan={4} className="text-muted">No departments found.</td></tr>
              )}
              {!loading && departments.map((d) => (
                <tr key={d.id}>
                  <td>{d.name}</td>
                  <td>{d.description || '—'}</td>
                  <td>{d.employeeCount}</td>
                  {isAdmin && (
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(d)}>Edit</button>
                        <button className="btn btn-danger btn-sm" onClick={() => handleDelete(d)}>Delete</button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </PageLayout>
  );
}
