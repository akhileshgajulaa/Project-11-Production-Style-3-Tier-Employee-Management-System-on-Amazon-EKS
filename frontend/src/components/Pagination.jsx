import React from 'react';

export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  const pages = [];
  const maxButtons = 5;
  let start = Math.max(0, page - Math.floor(maxButtons / 2));
  let end = Math.min(totalPages, start + maxButtons);
  if (end - start < maxButtons) start = Math.max(0, end - maxButtons);

  for (let i = start; i < end; i++) pages.push(i);

  return (
    <div className="pagination">
      <button disabled={page === 0} onClick={() => onPageChange(0)}>&laquo;</button>
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)}>&lsaquo;</button>
      {pages.map((p) => (
        <button
          key={p}
          className={p === page ? 'active' : ''}
          onClick={() => onPageChange(p)}
        >
          {p + 1}
        </button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>&rsaquo;</button>
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(totalPages - 1)}>&raquo;</button>
    </div>
  );
}
