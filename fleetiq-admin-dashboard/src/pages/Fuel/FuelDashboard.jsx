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
  const [refuels, setRefuels] = useState([]);
  const [anomalies, setAnomalies] = useState([]);
  const [vehicle, setVehicle] = useState(null);

  useEffect(() => {
    const loadFuel = async () => {
      try {
        const data = await fetchService('fuel', 'fuel-readings');
        setFuelData(data?.readings || []);
        setRefuels(data?.refuels || []);
        setAnomalies(data?.anomalies || []);
        setVehicle(data?.vehicle || null);
      } catch (err) {
        console.error("Failed to load fuel data", err);
      }
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
          value={vehicle?.avgEfficiency ? `${vehicle.avgEfficiency} km/L` : 'N/A'}
          changeText={vehicle?.avgEfficiency ? "Optimal operation bounds" : "Waiting for telemetry..."}
          icon={Droplet}
        />
        <StatCard
          title="Refueling Frequency"
          value={`${refuels.length} Events`}
          changeText={refuels.length > 0 ? "Last 72 hours" : "No recent events"}
          icon={RefreshCw}
        />
        <StatCard
          title="Active Fuel Alerts"
          value={anomalies.filter(a => a.status === 'OPEN').length}
          changeText={anomalies.length > 0 ? "Action required on thefts" : "All clear"}
          isNegative={anomalies.filter(a => a.status === 'OPEN').length > 0}
          icon={AlertTriangle}
        />
      </div>

      <div className="grid grid-cols-3">
        {/* Left Circular Gauge */}
        <Card title="Current Fuel Level (Primary)" style={{ gridColumn: 'span 1', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ padding: '20px 0' }}>
            {vehicle?.currentFuelLevel !== undefined && vehicle?.tankCapacity ? (
               <FuelGauge percentage={(vehicle.currentFuelLevel / vehicle.tankCapacity) * 100} />
            ) : (
               <div style={{ width: '200px', height: '200px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '50%', border: '8px solid var(--border-color)', color: 'var(--text-tertiary)', fontStyle: 'italic', fontSize: '12px', textAlign: 'center', padding: '20px' }}>Waiting for live telemetry...</div>
            )}
          </div>
          <div style={{ textAlign: 'center', marginTop: '16px', fontSize: '14px', color: 'var(--text-secondary)' }}>
            <strong>{vehicle?.regNum || 'Select a vehicle'}</strong><br />
            {vehicle?.currentFuelLevel !== undefined ? `${vehicle.currentFuelLevel} Litres remaining (Capacity ${vehicle.tankCapacity || 0}L)` : 'No data'}
          </div>
        </Card>

        {/* Right Recharts Area Chart */}
        <div style={{ gridColumn: 'span 2' }}>
          <ChartCard title="Fuel consumption curves (Last 6 Hours)">
            <div style={{ width: '100%', height: '100%', minHeight: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              {fuelData.length > 0 ? (
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
              ) : (
                <div style={{ color: 'var(--text-tertiary)', fontStyle: 'italic' }}>
                  Waiting for live telemetry...
                </div>
              )}
            </div>
          </ChartCard>
        </div>
      </div>

      <div className="grid grid-cols-2">
        {/* Refueling log */}
        <Card title="Recent Refuelings">
          {refuels.length > 0 ? (
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
          ) : (
            <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-tertiary)', fontStyle: 'italic', fontSize: '14px' }}>
              No recent refuelings recorded.
            </div>
          )}
        </Card>

        {/* Anomaly list */}
        <Card title="Fuel Anomalies & Thefts">
          {anomalies.length > 0 ? (
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
          ) : (
            <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-tertiary)', fontStyle: 'italic', fontSize: '14px' }}>
              No active anomalies detected.
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};
export default FuelDashboard;
