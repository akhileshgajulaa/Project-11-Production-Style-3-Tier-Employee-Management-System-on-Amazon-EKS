import React from 'react';
import PageLayout from '../components/PageLayout';
import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user } = useAuth();

  return (
    <PageLayout title="Profile">
      <div className="page-header">
        <h1>Profile</h1>
      </div>
      <div className="card" style={{ maxWidth: 480 }}>
        <div className="detail-grid">
          <div className="detail-item"><div className="label">Username</div><div className="value">{user?.username}</div></div>
          <div className="detail-item"><div className="label">Email</div><div className="value">{user?.email}</div></div>
          <div className="detail-item"><div className="label">Role</div><div className="value">{user?.role}</div></div>
        </div>
      </div>
    </PageLayout>
  );
}
