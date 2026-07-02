import React, { useEffect, useState } from 'react';
import { Card } from '../../components/common/Card';
import { Table } from '../../components/common/Table';
import { AlertBadge } from '../../components/common/AlertBadge';
import { fetchService } from '../../services/api';
import { Wrench, Calendar, ClipboardList, CheckCircle } from 'lucide-react';
import '../../styles/components.css';

export const MaintenanceDashboard = () => {
  const [schedules, setSchedules] = useState([
    { id: 'ms1', vehicle: 'KA-01-MJ-1024', type: 'OIL_CHANGE', interval: '5,000 km', lastDate: '2026-05-01', nextDate: '2026-08-01', status: 'ACTIVE', progress: 85 },
    { id: 'ms2', vehicle: 'DL-01-AA-5678', type: 'BRAKE_INSPECTION', interval: '15,000 km', lastDate: '2026-03-10', nextDate: '2026-07-10', status: 'OVERDUE', progress: 100 }
  ]);
  const [workOrders, setWorkOrders] = useState([
    { id: 'wo1', orderNumber: 'WO-2026-0042', vehicle: 'DL-01-AA-5678', title: 'Replace Front Brake Pads', priority: 'HIGH', status: 'IN_PROGRESS', technician: 'Alex Mercer', scheduledDate: '2026-07-02' },
    { id: 'wo2', orderNumber: 'WO-2026-0045', vehicle: 'KA-01-MJ-1024', title: 'Coolant Pump Leak Repair', priority: 'CRITICAL', status: 'ASSIGNED', technician: 'Dave Miller', scheduledDate: '2026-07-03' }
  ]);

  const handleCompleteWorkOrder = async (id) => {
    await fetchService('health', 'maintenance/complete-work-order', {
      method: 'POST',
      body: JSON.stringify({ workOrderId: id })
    });
    setWorkOrders(prev => prev.map(wo => wo.id === id ? { ...wo, status: 'COMPLETED' } : wo));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div>
        <h1 style={{ fontSize: '32px', margin: '0 0 8px' }}>Maintenance Management</h1>
        <p style={{ color: 'var(--text-tertiary)' }}>Technician work orders, scheduled fleet services, and compliance timelines</p>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-3">
        <Card style={{ padding: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <ClipboardList size={24} style={{ color: 'var(--accent)' }} />
            <div>
              <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Active Work Orders</div>
              <h3 style={{ fontSize: '18px' }}>{workOrders.filter(w => w.status !== 'COMPLETED').length} Open</h3>
            </div>
          </div>
        </Card>

        <Card style={{ padding: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Calendar size={24} style={{ color: 'var(--warning)' }} />
            <div>
              <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Overdue Services</div>
              <h3 style={{ fontSize: '18px' }}>{schedules.filter(s => s.status === 'OVERDUE').length} Overdue</h3>
            </div>
          </div>
        </Card>

        <Card style={{ padding: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Wrench size={24} style={{ color: 'var(--success)' }} />
            <div>
              <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Technicians Dispatched</div>
              <h3 style={{ fontSize: '18px' }}>2 Active</h3>
            </div>
          </div>
        </Card>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {/* Active Work Orders */}
        <Card title="Open Work Orders">
          <Table
            headers={['Order Number', 'Vehicle', 'Job Title', 'Priority', 'Technician', 'Scheduled Date', 'Status', 'Action']}
            data={workOrders}
            renderRow={(wo) => (
              <tr key={wo.id}>
                <td style={{ fontWeight: '600' }}>{wo.orderNumber}</td>
                <td>{wo.vehicle}</td>
                <td style={{ fontWeight: '500' }}>{wo.title}</td>
                <td><AlertBadge priority={wo.priority} /></td>
                <td>{wo.technician}</td>
                <td>{wo.scheduledDate}</td>
                <td>
                  <span style={{
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '11px',
                    fontWeight: '600',
                    backgroundColor: wo.status === 'IN_PROGRESS' ? 'var(--accent-light)' : wo.status === 'COMPLETED' ? 'var(--success-bg)' : 'var(--warning-bg)',
                    color: wo.status === 'IN_PROGRESS' ? 'var(--accent)' : wo.status === 'COMPLETED' ? 'var(--success)' : 'var(--warning)'
                  }}>{wo.status}</span>
                </td>
                <td>
                  {wo.status !== 'COMPLETED' ? (
                    <button
                      className="btn btn-secondary"
                      style={{ padding: '6px 12px', fontSize: '12px' }}
                      onClick={() => handleCompleteWorkOrder(wo.id)}
                    >
                      Complete Job
                    </button>
                  ) : (
                    <span style={{ color: 'var(--text-tertiary)', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <CheckCircle size={14} style={{ color: 'var(--success)' }} /> Resolved
                    </span>
                  )}
                </td>
              </tr>
            )}
          />
        </Card>

        {/* Scheduled Maintenance */}
        <Card title="Scheduled Fleet Maintenance Logs">
          <Table
            headers={['Vehicle', 'Service Type', 'Interval', 'Last Service Date', 'Next Due Date', 'Timeline Progress', 'Status']}
            data={schedules}
            renderRow={(s) => (
              <tr key={s.id}>
                <td style={{ fontWeight: '600' }}>{s.vehicle}</td>
                <td><code>{s.type}</code></td>
                <td>{s.interval}</td>
                <td>{s.lastDate}</td>
                <td style={{ fontWeight: '500' }}>{s.nextDate}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div style={{
                      width: '100px',
                      height: '6px',
                      backgroundColor: 'var(--bg-tertiary)',
                      borderRadius: '3px',
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        width: `${s.progress}%`,
                        height: '100%',
                        backgroundColor: s.status === 'OVERDUE' ? 'var(--danger)' : 'var(--accent)'
                      }} />
                    </div>
                    <span style={{ fontSize: '11px', fontWeight: '600' }}>{s.progress}%</span>
                  </div>
                </td>
                <td>
                  <span style={{
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '11px',
                    fontWeight: '600',
                    backgroundColor: s.status === 'OVERDUE' ? 'var(--danger-bg)' : 'var(--success-bg)',
                    color: s.status === 'OVERDUE' ? 'var(--danger)' : 'var(--success)'
                  }}>{s.status}</span>
                </td>
              </tr>
            )}
          />
        </Card>
      </div>
    </div>
  );
};
export default MaintenanceDashboard;
