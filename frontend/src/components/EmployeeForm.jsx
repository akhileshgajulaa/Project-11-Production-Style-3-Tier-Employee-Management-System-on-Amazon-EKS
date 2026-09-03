import React, { useEffect, useState } from 'react';
import { getDepartments } from '../api/departmentApi';

const emptyForm = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  dateOfBirth: '',
  gender: '',
  jobTitle: '',
  departmentId: '',
  joiningDate: '',
  salary: '',
  address: '',
};

export default function EmployeeForm({ initialValues, onSubmit, submitLabel = 'Save', serverError }) {
  const [form, setForm] = useState({ ...emptyForm, ...initialValues });
  const [departments, setDepartments] = useState([]);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getDepartments().then(setDepartments).catch(() => {});
  }, []);

  useEffect(() => {
    if (initialValues) {
      setForm((prev) => ({ ...prev, ...initialValues }));
    }
  }, [initialValues]);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const validate = () => {
    const errs = {};
    if (!form.firstName.trim()) errs.firstName = 'First name is required';
    if (!form.lastName.trim()) errs.lastName = 'Last name is required';
    if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = 'Enter a valid email address';
    if (!/^[+]?[0-9\-\s]{7,15}$/.test(form.phone)) errs.phone = 'Enter a valid phone number';
    if (!form.jobTitle.trim()) errs.jobTitle = 'Job title is required';
    if (!form.departmentId) errs.departmentId = 'Department is required';
    if (!form.joiningDate) errs.joiningDate = 'Joining date is required';
    else if (new Date(form.joiningDate) > new Date()) errs.joiningDate = 'Joining date cannot be in the future';
    if (form.salary === '' || Number(form.salary) < 0) errs.salary = 'Salary must be zero or a positive number';
    if (form.dateOfBirth && new Date(form.dateOfBirth) > new Date()) errs.dateOfBirth = 'Date of birth must be in the past';
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSubmitting(true);
    try {
      await onSubmit({
        ...form,
        departmentId: Number(form.departmentId),
        salary: Number(form.salary),
        gender: form.gender || null,
        dateOfBirth: form.dateOfBirth || null,
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="card">
      {serverError && <div className="alert alert-danger">{serverError}</div>}

      <div className="form-row">
        <div className="form-group">
          <label className="form-label">First Name *</label>
          <input className="form-input" value={form.firstName} onChange={handleChange('firstName')} />
          {errors.firstName && <div className="form-error">{errors.firstName}</div>}
        </div>
        <div className="form-group">
          <label className="form-label">Last Name *</label>
          <input className="form-input" value={form.lastName} onChange={handleChange('lastName')} />
          {errors.lastName && <div className="form-error">{errors.lastName}</div>}
        </div>
      </div>

      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Email *</label>
          <input className="form-input" type="email" value={form.email} onChange={handleChange('email')} />
          {errors.email && <div className="form-error">{errors.email}</div>}
        </div>
        <div className="form-group">
          <label className="form-label">Phone *</label>
          <input className="form-input" value={form.phone} onChange={handleChange('phone')} placeholder="+91-9876543210" />
          {errors.phone && <div className="form-error">{errors.phone}</div>}
        </div>
      </div>

      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Date of Birth</label>
          <input className="form-input" type="date" value={form.dateOfBirth || ''} onChange={handleChange('dateOfBirth')} />
          {errors.dateOfBirth && <div className="form-error">{errors.dateOfBirth}</div>}
        </div>
        <div className="form-group">
          <label className="form-label">Gender</label>
          <select className="form-select" value={form.gender || ''} onChange={handleChange('gender')}>
            <option value="">Select…</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
      </div>

      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Job Title *</label>
          <input className="form-input" value={form.jobTitle} onChange={handleChange('jobTitle')} />
          {errors.jobTitle && <div className="form-error">{errors.jobTitle}</div>}
        </div>
        <div className="form-group">
          <label className="form-label">Department *</label>
          <select className="form-select" value={form.departmentId} onChange={handleChange('departmentId')}>
            <option value="">Select department…</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>{d.name}</option>
            ))}
          </select>
          {errors.departmentId && <div className="form-error">{errors.departmentId}</div>}
        </div>
      </div>

      <div className="form-row">
        <div className="form-group">
          <label className="form-label">Joining Date *</label>
          <input className="form-input" type="date" value={form.joiningDate} onChange={handleChange('joiningDate')} />
          {errors.joiningDate && <div className="form-error">{errors.joiningDate}</div>}
        </div>
        <div className="form-group">
          <label className="form-label">Salary (₹) *</label>
          <input className="form-input" type="number" min="0" step="0.01" value={form.salary} onChange={handleChange('salary')} />
          {errors.salary && <div className="form-error">{errors.salary}</div>}
        </div>
      </div>

      <div className="form-group">
        <label className="form-label">Address</label>
        <textarea className="form-textarea" rows={2} value={form.address || ''} onChange={handleChange('address')} />
      </div>

      <button type="submit" className="btn btn-primary" disabled={submitting}>
        {submitting ? 'Saving…' : submitLabel}
      </button>
    </form>
  );
}
