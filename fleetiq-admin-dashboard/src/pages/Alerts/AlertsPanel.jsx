import React, { useEffect, useState } from 'react';
import { Table } from '../../components/common/Table';
import { Card } from '../../components/common/Card';
import { AlertBadge } from '../../components/common/AlertBadge';
import { fetchService } from '../../services/api';
import { AlertTriangle, CheckCircle, Clock } from 'lucide-react';
import '../../styles/components.css';

export const AlertsPanel = () => {
  const [alerts, setAlerts] = useState([]);
  const [filterPriority, setFilterPriority] = useState('ALL');

  useEffect(() => {
    loadAlerts();
  }, []);

  const loadAlerts = async () => {
    const data = await fetchService('alerts', 'alerts');
    setAlerts(data || []);
  };

  const handleAcknowledge = async (id) => {
    // API endpoint: PUT /api/v1/alerts/{id}/acknowledge
    await fetchService('alerts', `alerts/${id}/acknowledge`, { method: 'PUT' });
    setAlerts(prev => prev.map(a => a.id === id ? { ...a, status: 'ACKNOWLEDGED' } : a));
  };

  const filtered = filterPriority === 'ALL' 
    ? alerts 
    : alerts.filter(a => a.priority === filterPriority);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '32px', margin: '0' }}>System Alerts</h1>
          <p style={{ color: 'var(--text-tertiary)' }}>Central dispatching log for fuel leaks, spatial boundaries, and safety violations</p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'INFO'].map(p => (
            <button
              key={p}
              className="btn"
              style={{
                padding: '8px 12px',
                fontSize: '12px',
                backgroundColor: filterPriority === p ? 'var(--primary)' : 'var(--bg-secondary)',
                color: filterPriority === p ? 'var(--bg-secondary)' : 'var(--text-primary)',
                borderColor: 'var(--border-color)'
              }}
              onClick={() => setFilterPriority(p)}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      <Card>
        <Table
          headers={['Timestamp', 'Vehicle', 'Event Source', 'Alert Code', 'Priority', 'Description', 'Status', 'Action']}
          data={filtered}
          emptyMessage="No active alerts match this filter."
          renderRow={(a) => (
            <tr key={a.id}>
              <td style={{ fontSize: '13px', color: 'var(--text-tertiary)' }}>
                {new Date(a.createdAt).toLocaleString()}
              </td>
              <td style={{ fontWeight: '600' }}>v1</td>
              <td style={{ fontSize: '13px' }}>
                {a.alertType?.includes('FUEL') ? 'fleetiq-fuel-service' : 'fleetiq-alerts-service'}
              </td>
              <td><code style={{ fontSize: '12px' }}>{a.alertType}</code></td>
              <td><AlertBadge priority={a.priority} /></td>
              <td style={{ fontSize: '13px' }}>{a.message}</td>
              <td>
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', fontWeight: '500' }}>
                  {a.status === 'OPEN' ? <Clock size={14} style={{ color: 'var(--danger)' }} /> : <CheckCircle size={14} style={{ color: 'var(--success)' }} />}
                  <span style={{ color: a.status === 'OPEN' ? 'var(--danger)' : 'var(--success)' }}>
                    {a.status}
                  </span>
                </div>
              </td>
              <td>
                {a.status === 'OPEN' ? (
                  <button 
                    className="btn btn-secondary" 
                    style={{ padding: '6px 12px', fontSize: '12px' }}
                    onClick={() => handleAcknowledge(a.id)}
                  >
                    Acknowledge
                  </button>
                ) : (
                  <span style={{ color: 'var(--text-tertiary)', fontSize: '12px' }}>Resolved</span>
                )}
              </td>
            </tr>
          )}
        />
      </Card>
    </div>
  );
};
export default AlertsPanel;
