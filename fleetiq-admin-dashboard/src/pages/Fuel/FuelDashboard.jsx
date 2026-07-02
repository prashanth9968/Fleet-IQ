import React, { useEffect, useState } from 'react';
import { StatCard } from '../../components/common/StatCard';
import { Card } from '../../components/common/Card';
import { FuelGauge } from '../../components/common/FuelGauge';
import { ChartCard } from '../../components/common/ChartCard';
import { Table } from '../../components/common/Table';
import { fetchService } from '../../services/api';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';
import { Droplet, AlertTriangle, RefreshCw } from 'lucide-react';
import '../../styles/components.css';

export const FuelDashboard = () => {
  const [fuelData, setFuelData] = useState([]);
  const [refuels, setRefuels] = useState([
    { id: 'rf1', date: '2026-07-01 10:15', before: '15.0L', after: '380.0L', added: '365.0L', cost: '$410.50' },
    { id: 'rf2', date: '2026-06-29 14:30', before: '42.0L', after: '350.0L', added: '308.0L', cost: '$345.00' }
  ]);
  const [anomalies, setAnomalies] = useState([
    { id: 'an1', date: '2026-07-01 12:45', type: 'THEFT', volume: '-25.0L', location: 'Highway 4 Depo', status: 'OPEN' }
  ]);

  useEffect(() => {
    const loadFuel = async () => {
      const data = await fetchService('fuel', 'fuel-readings');
      setFuelData(data || []);
    };
    loadFuel();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div>
        <h1 style={{ fontSize: '32px', margin: '0 0 8px' }}>Fuel Monitoring</h1>
        <p style={{ color: 'var(--text-tertiary)' }}>Advanced fuel level indicators, anomaly detection, and consumption tracking</p>
      </div>

      <div className="grid grid-cols-3">
        <StatCard
          title="Average Efficiency"
          value="4.2 km/L"
          changeText="Optimal operation bounds"
          icon={Droplet}
        />
        <StatCard
          title="Refueling Frequency"
          value="2 Events"
          changeText="Last 72 hours"
          icon={RefreshCw}
        />
        <StatCard
          title="Active Fuel Alerts"
          value={anomalies.filter(a => a.status === 'OPEN').length}
          changeText="Action required on thefts"
          isNegative={anomalies.length > 0}
          icon={AlertTriangle}
        />
      </div>

      <div className="grid grid-cols-3">
        {/* Left Circular Gauge */}
        <Card title="Current Fuel Level (Primary)" style={{ gridColumn: 'span 1', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ padding: '20px 0' }}>
            <FuelGauge percentage={68.3} />
          </div>
          <div style={{ textAlign: 'center', marginTop: '16px', fontSize: '14px', color: 'var(--text-secondary)' }}>
            <strong>TATA Prima (KA-01-MJ-1024)</strong><br />
            273.2 Litres remaining (Capacity 400L)
          </div>
        </Card>

        {/* Right Recharts Area Chart */}
        <div style={{ gridColumn: 'span 2' }}>
          <ChartCard title="Fuel consumption curves (Last 6 Hours)">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={fuelData}>
                <defs>
                  <linearGradient id="colorFuel" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--accent)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                <XAxis dataKey="time" stroke="var(--text-tertiary)" fontSize={12} />
                <YAxis domain={[0, 100]} stroke="var(--text-tertiary)" fontSize={12} />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'var(--bg-secondary)', 
                    borderColor: 'var(--border-color)',
                    color: 'var(--text-primary)'
                  }} 
                />
                <Area type="monotone" dataKey="level" stroke="var(--accent)" strokeWidth={2} fillOpacity={1} fill="url(#colorFuel)" />
              </AreaChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>
      </div>

      <div className="grid grid-cols-2">
        {/* Refueling log */}
        <Card title="Recent Refuelings">
          <Table
            headers={['Date & Time', 'Fuel Before', 'Fuel After', 'Fuel Added', 'Total Cost']}
            data={refuels}
            renderRow={(rf) => (
              <tr key={rf.id}>
                <td>{rf.date}</td>
                <td>{rf.before}</td>
                <td>{rf.after}</td>
                <td style={{ color: 'var(--success)', fontWeight: '600' }}>+{rf.added}</td>
                <td style={{ fontWeight: '600' }}>{rf.cost}</td>
              </tr>
            )}
          />
        </Card>

        {/* Anomaly list */}
        <Card title="Fuel Anomalies & Thefts">
          <Table
            headers={['Date & Time', 'Alert Type', 'Discharge Volume', 'Location', 'Status']}
            data={anomalies}
            renderRow={(an) => (
              <tr key={an.id}>
                <td>{an.date}</td>
                <td><code style={{ color: 'var(--danger)', backgroundColor: 'var(--danger-bg)', padding: '2px 6px', borderRadius: '4px', fontSize: '12px' }}>{an.type}</code></td>
                <td style={{ color: 'var(--danger)', fontWeight: '600' }}>{an.volume}</td>
                <td>{an.location}</td>
                <td>
                  <span style={{
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '11px',
                    fontWeight: '600',
                    backgroundColor: an.status === 'OPEN' ? 'var(--danger-bg)' : 'var(--success-bg)',
                    color: an.status === 'OPEN' ? 'var(--danger)' : 'var(--success)'
                  }}>{an.status}</span>
                </td>
              </tr>
            )}
          />
        </Card>
      </div>
    </div>
  );
};
export default FuelDashboard;
