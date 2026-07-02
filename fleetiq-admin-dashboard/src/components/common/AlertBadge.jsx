import React from 'react';

export const AlertBadge = ({ priority }) => {
  const normalized = (priority || 'INFO').toLowerCase();
  
  return (
    <span className={`badge badge-${normalized}`}>
      {normalized}
    </span>
  );
};
