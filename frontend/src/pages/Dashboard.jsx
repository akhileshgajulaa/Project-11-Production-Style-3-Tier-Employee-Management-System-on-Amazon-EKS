import React, { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import PageLayout from '../components/PageLayout';
import { getDashboardStatistics } from '../api/dashboardApi';
import { Link } from 'react-router-dom';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    getDashboardStatistics()
      .then((data) => {
        if (mounted) setStats(data);
      })
      .catch(() => {
        if (mounted) setError('Unable to load dashboard statistics.');
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => { mounted = false; };
  }, []);

  const chartData = stats
    ? Object.entries(stats.employeesByDepartment || {}).map(([name, value]) => ({ name, employees: value }))
    : [];

  return (
    <PageLayout title="Dashboard">
      {error && <div className="alert alert-danger">{error}</div>}

      {loading && <div className="spinner-wrap">Loading dashboard…</div>}

      {stats && (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-label">Total Employees</div>
              <div className="stat-value">{stats.totalEmployees}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Active Employees</div>
              <div className="stat-value">{stats.activeEmployees}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Inactive Employees</div>
              <div className="stat-value">{stats.inactiveEmployees}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Total Departments</div>
              <div className="stat-value">{stats.totalDepartments}</div>
            </div>
          </div>

          <div className="card" style={{ marginBottom: 24 }}>
            <div className="section-title">Employees by Department</div>
            <div style={{ width: '100%', height: 280 }}>
              <ResponsiveContainer>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e5eb" />
                  <XAxis dataKey="name" fontSize={12} />
                  <YAxis allowDecimals={false} fontSize={12} />
                  <Tooltip />
                  <Bar dataKey="employees" fill="#2453ff" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="card">
            <div className="section-title">Recently Joined Employees</div>
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Department</th>
                    <th>Job Title</th>
                    <th>Joining Date</th>
                  </tr>
                </thead>
                <tbody>
                  {(stats.recentlyJoined || []).length === 0 && (
                    <tr><td colSpan={4} className="text-muted">No recent joiners in the last 3 months.</td></tr>
                  )}
                  {(stats.recentlyJoined || []).map((emp) => (
                    <tr key={emp.id}>
                      <td>
                        <Link to={`/employees/${emp.id}`}>{emp.fullName}</Link>
                      </td>
                      <td>{emp.department}</td>
                      <td>{emp.jobTitle}</td>
                      <td>{emp.joiningDate}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </PageLayout>
  );
}
