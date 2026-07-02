import React, { useEffect, useState } from 'react';
import { StatCard } from '../../components/common/StatCard';
import { Card } from '../../components/common/Card';
import { Table } from '../../components/common/Table';
import { AlertBadge } from '../../components/common/AlertBadge';
import { fetchService } from '../../services/api';
import { Truck, Users, Droplet, AlertTriangle, ChevronRight } from 'lucide-react';
import '../../styles/components.css';

export const OverviewDashboard = () => {
  const [vehicles, setVehicles] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        const [vehiclesData, driversData, alertsData] = await Promise.all([
          fetchService('vehicle', 'vehicles'),
          fetchService('driver', 'drivers'),
          fetchService('alerts', 'alerts')
        ]);
        setVehicles(vehiclesData || []);
        setDrivers(driversData || []);
        setAlerts(alertsData || []);
      } catch (err) {
        console.error('Failed to load dashboard statistics', err);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  if (loading) {
    return <div style={{ color: 'var(--text-secondary)' }}>Loading Dashboard...</div>;
  }

  const activeVehicles = vehicles.filter(v => v.status === 'ACTIVE').length;
  const activeDrivers = drivers.filter(d => d.status === 'ACTIVE').length;
  const openAlertsCount = alerts.filter(a => a.status === 'OPEN').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div>
        <h1 style={{ fontSize: '32px', margin: '0 0 8px' }}>Operations Overview</h1>
        <p style={{ color: 'var(--text-tertiary)' }}>Real-time telemetry, driver safety scoring, and fuel consumption indicators</p>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-4">
        <StatCard
          title="Total Vehicles"
          value={vehicles.length}
          changeText={`${activeVehicles} active in shift`}
          icon={Truck}
        />
        <StatCard
          title="Active Drivers"
          value={drivers.length}
          changeText={`${activeDrivers} currently driving`}
          icon={Users}
        />
        <StatCard
          title="Avg Fuel efficiency"
          value="4.2 km/L"
          changeText="+0.1 km/L vs last week"
          icon={Droplet}
        />
        <StatCard
          title="Active Alerts"
          value={openAlertsCount}
          changeText={`${alerts.filter(a => a.priority === 'CRITICAL').length} critical event(s)`}
          isNegative={openAlertsCount > 0}
          icon={AlertTriangle}
        />
      </div>

      <div className="grid grid-cols-2">
        {/* Recent Alerts Card */}
        <Card title="Recent Alerts" actions={<a href="/alerts" style={{ color: 'var(--accent)', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '4px' }}>View all <ChevronRight size={14} /></a>}>
          <Table
            headers={['Vehicle', 'Type', 'Priority', 'Alert Message', 'Status']}
            data={alerts.slice(0, 5)}
            emptyMessage="No active alerts recorded."
            renderRow={(alert) => (
              <tr key={alert.id}>
                <td style={{ fontWeight: '600' }}>v1</td>
                <td><code style={{ fontSize: '12px' }}>{alert.alertType}</code></td>
                <td><AlertBadge priority={alert.priority} /></td>
                <td style={{ fontSize: '13px' }}>{alert.message}</td>
                <td>
                  <span style={{ 
                    padding: '4px 8px', 
                    borderRadius: '4px', 
                    fontSize: '11px', 
                    fontWeight: '600',
                    backgroundColor: alert.status === 'OPEN' ? 'var(--danger-bg)' : 'var(--success-bg)',
                    color: alert.status === 'OPEN' ? 'var(--danger)' : 'var(--success)'
                  }}>
                    {alert.status}
                  </span>
                </td>
              </tr>
            )}
          />
        </Card>

        {/* Safety Leaderboard */}
        <Card title="Driver Safety Leaderboard" actions={<a href="/drivers" style={{ color: 'var(--accent)', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '4px' }}>Leaderboard <ChevronRight size={14} /></a>}>
          <Table
            headers={['Rank', 'Driver Name', 'Employee ID', 'Safety Score']}
            data={[...drivers].sort((a, b) => b.safetyScore - a.safetyScore).slice(0, 5)}
            emptyMessage="No driver scores computed."
            renderRow={(driver, idx) => (
              <tr key={driver.id}>
                <td style={{ fontWeight: '700', width: '50px' }}>#{idx + 1}</td>
                <td style={{ fontWeight: '500' }}>{driver.firstName} {driver.lastName}</td>
                <td>{driver.employeeId}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontWeight: '600', color: driver.safetyScore >= 90 ? 'var(--success)' : driver.safetyScore >= 80 ? 'var(--warning)' : 'var(--danger)' }}>
                      {driver.safetyScore}
                    </span>
                    <div style={{
                      width: '80px',
                      height: '6px',
                      backgroundColor: 'var(--bg-tertiary)',
                      borderRadius: '3px',
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        width: `${driver.safetyScore}%`,
                        height: '100%',
                        backgroundColor: driver.safetyScore >= 90 ? 'var(--success)' : driver.safetyScore >= 80 ? 'var(--warning)' : 'var(--danger)'
                      }} />
                    </div>
                  </div>
                </td>
              </tr>
            )}
          />
        </Card>
      </div>
    </div>
  );
};
export default OverviewDashboard;
