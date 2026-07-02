import React, { useEffect, useState } from 'react';
import { Card } from '../../components/common/Card';
import { Table } from '../../components/common/Table';
import { fetchService } from '../../services/api';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip } from 'recharts';
import { Shield, MapPin, Truck, AlertTriangle, Activity } from 'lucide-react';
import '../../styles/components.css';

export const DriverProfiles = () => {
  const [drivers, setDrivers] = useState([]);
  const [selectedDriver, setSelectedDriver] = useState(null);

  // Mock trend data for safety scores
  const scoreTrend = [
    { week: 'W1', score: 85 },
    { week: 'W2', score: 89 },
    { week: 'W3', score: 88 },
    { week: 'W4', score: 92.5 }
  ];

  useEffect(() => {
    const loadDrivers = async () => {
      const data = await fetchService('driver', 'drivers');
      setDrivers(data || []);
      if (data && data.length > 0) {
        setSelectedDriver(data[0]);
      }
    };
    loadDrivers();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div>
        <h1 style={{ fontSize: '32px', margin: '0 0 8px' }}>Driver Directory & Profiles</h1>
        <p style={{ color: 'var(--text-tertiary)' }}>Operator shifts, real-time safety scores, and violation indicators</p>
      </div>

      <div style={{ display: 'flex', gap: '24px', alignItems: 'flex-start' }}>
        {/* Left Side: Master List */}
        <div style={{ width: '350px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '600' }}>Fleet Operators</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {drivers.map(d => (
              <div
                key={d.id}
                className="card"
                style={{
                  padding: '16px',
                  cursor: 'pointer',
                  borderColor: selectedDriver?.id === d.id ? 'var(--accent)' : 'var(--border-color)',
                  backgroundColor: selectedDriver?.id === d.id ? 'var(--bg-tertiary)' : 'var(--bg-secondary)'
                }}
                onClick={() => setSelectedDriver(d)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: '600', color: 'var(--text-primary)' }}>{d.firstName} {d.lastName}</span>
                  <span style={{
                    fontSize: '13px',
                    fontWeight: '600',
                    color: d.safetyScore >= 90 ? 'var(--success)' : 'var(--warning)'
                  }}>{d.safetyScore}%</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px', fontSize: '12px', color: 'var(--text-tertiary)' }}>
                  <span>ID: {d.employeeId}</span>
                  <span>{d.status}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Side: Detail Profile Card */}
        {selectedDriver && (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <Card title={`${selectedDriver.firstName} ${selectedDriver.lastName} Profile`}>
              <div style={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: '24px' }}>
                {/* Left Circular Score */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', borderRight: '1px solid var(--border-color)', paddingRight: '24px' }}>
                  <div style={{
                    width: '100px',
                    height: '100px',
                    borderRadius: '50%',
                    border: '8px solid var(--bg-tertiary)',
                    borderTopColor: selectedDriver.safetyScore >= 90 ? 'var(--success)' : 'var(--warning)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '22px',
                    fontWeight: '700',
                    color: 'var(--text-primary)',
                    marginBottom: '8px'
                  }}>
                    {selectedDriver.safetyScore}%
                  </div>
                  <span style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Safety Score</span>
                </div>

                {/* Right Details Grid */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <Truck size={18} style={{ color: 'var(--accent)' }} />
                    <div>
                      <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Current Vehicle</div>
                      <div style={{ fontWeight: '600', color: 'var(--text-primary)' }}>KA-01-MJ-1024</div>
                    </div>
                  </div>
                  
                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <MapPin size={18} style={{ color: 'var(--accent)' }} />
                    <div>
                      <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Current Location</div>
                      <div style={{ fontWeight: '600', color: 'var(--text-primary)' }}>Bangalore City</div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <Shield size={18} style={{ color: 'var(--success)' }} />
                    <div>
                      <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>License Expiry</div>
                      <div style={{ fontWeight: '600', color: 'var(--text-primary)' }}>{selectedDriver.licenseExpiry}</div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <Activity size={18} style={{ color: 'var(--accent)' }} />
                    <div>
                      <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Total Trips</div>
                      <div style={{ fontWeight: '600', color: 'var(--text-primary)' }}>24 Trips</div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <AlertTriangle size={18} style={{ color: 'var(--warning)' }} />
                    <div>
                      <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Violations</div>
                      <div style={{ fontWeight: '600', color: 'var(--text-primary)' }}>2 Active</div>
                    </div>
                  </div>
                </div>
              </div>
            </Card>

            <div className="grid grid-cols-2">
              {/* Score Trend Recharts LineChart */}
              <Card title="Safety Score Trend">
                <div style={{ width: '100%', height: '220px', marginTop: '16px' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={scoreTrend}>
                      <XAxis dataKey="week" stroke="var(--text-tertiary)" fontSize={11} />
                      <YAxis domain={[70, 100]} stroke="var(--text-tertiary)" fontSize={11} />
                      <Tooltip contentStyle={{ backgroundColor: 'var(--bg-secondary)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }} />
                      <Line type="monotone" dataKey="score" stroke="var(--accent)" strokeWidth={2.5} activeDot={{ r: 6 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </Card>

              {/* Driving Violations logs */}
              <Card title="Recent Safety Violations">
                <Table
                  headers={['Timestamp', 'Event Type', 'Speed recorded', 'Score Impact']}
                  data={[
                    { time: '12:00', type: 'SPEEDING', speed: '95 km/h (limit 80)', impact: '-5.0' },
                    { time: '10:45', type: 'HARSH_BRAKING', speed: 'G-Force 1.2g', impact: '-3.0' }
                  ]}
                  renderRow={(item, idx) => (
                    <tr key={idx}>
                      <td style={{ fontSize: '13px', color: 'var(--text-tertiary)' }}>{item.time}</td>
                      <td><code style={{ fontSize: '12px', color: 'var(--danger)', backgroundColor: 'var(--danger-bg)', padding: '2px 6px', borderRadius: '4px' }}>{item.type}</code></td>
                      <td style={{ fontSize: '13px' }}>{item.speed}</td>
                      <td style={{ color: 'var(--danger)', fontWeight: '600' }}>{item.impact}</td>
                    </tr>
                  )}
                />
              </Card>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
export default DriverProfiles;
