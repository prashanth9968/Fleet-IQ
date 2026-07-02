import React from 'react';

export const StatCard = ({ title, value, changeText, isNegative, icon: Icon, className = '' }) => {
  return (
    <div className={`card ${className}`}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div className="card-title">{title}</div>
          <div className="card-value">{value}</div>
          {changeText && (
            <div style={{ 
              fontSize: '13px', 
              fontWeight: '500', 
              marginTop: '8px', 
              color: isNegative ? 'var(--danger)' : 'var(--success)' 
            }}>
              {changeText}
            </div>
          )}
        </div>
        {Icon && (
          <div style={{
            padding: '8px',
            borderRadius: 'var(--radius-sm)',
            backgroundColor: 'var(--bg-tertiary)',
            color: 'var(--text-primary)'
          }}>
            <Icon size={20} />
          </div>
        )}
      </div>
    </div>
  );
};
