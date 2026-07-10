import React from 'react';

export const Card = ({ title, children, className = '', actions, style }) => {
  return (
    <div className={`card ${className}`} style={style}>
      {(title || actions) && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          {title && <h3 style={{ fontSize: '18px', fontWeight: '600' }}>{title}</h3>}
          {actions && <div>{actions}</div>}
        </div>
      )}
      {children}
    </div>
  );
};
