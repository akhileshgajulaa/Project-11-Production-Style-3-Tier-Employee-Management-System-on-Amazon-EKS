import React from 'react';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

export default function PageLayout({ title, children }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-area">
        <Topbar title={title} />
        <main className="page-content">{children}</main>
      </div>
    </div>
  );
}
