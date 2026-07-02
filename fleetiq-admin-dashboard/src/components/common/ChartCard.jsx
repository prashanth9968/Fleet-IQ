import React from 'react';
import { Card } from './Card';

export const ChartCard = ({ title, children, actions }) => {
  return (
    <Card title={title} actions={actions}>
      <div style={{ width: '100%', height: '300px', marginTop: '16px' }}>
        {children}
      </div>
    </Card>
  );
};
