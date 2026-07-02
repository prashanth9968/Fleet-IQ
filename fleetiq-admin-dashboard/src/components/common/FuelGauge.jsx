import React from 'react';

export const FuelGauge = ({ percentage }) => {
  const normalized = Math.min(Math.max(percentage, 0), 100);
  const strokeDashoffset = 339.29 - (339.29 * normalized) / 100;

  // Determine color based on level
  let color = 'var(--success)';
  if (normalized < 20) {
    color = 'var(--danger)';
  } else if (normalized < 50) {
    color = 'var(--warning)';
  }

  return (
    <div className="gauge-wrapper">
      <svg width="120" height="120" style={{ transform: 'rotate(-90deg)' }}>
        <circle
          cx="60"
          cy="60"
          r="54"
          fill="transparent"
          stroke="var(--bg-tertiary)"
          strokeWidth="8"
        />
        <circle
          cx="60"
          cy="60"
          r="54"
          fill="transparent"
          stroke={color}
          strokeWidth="8"
          strokeDasharray="339.29"
          strokeDashoffset={strokeDashoffset}
          style={{ transition: 'stroke-dashoffset 0.5s ease' }}
        />
      </svg>
      <div className="gauge-text">{Math.round(normalized)}%</div>
    </div>
  );
};
