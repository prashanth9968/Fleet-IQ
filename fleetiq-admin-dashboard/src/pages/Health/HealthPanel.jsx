import React, { useEffect, useState } from 'react';
import { Card } from '../../components/common/Card';
import { Table } from '../../components/common/Table';
import { fetchService } from '../../services/api';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { ShieldAlert, Thermometer, Battery, Activity, Flame, Heart, AlertTriangle } from 'lucide-react';
import '../../styles/components.css';

export const HealthPanel = () => {
  const [vehicles, setVehicles] = useState([]);
  const [selectedVehicle, setSelectedVehicle] = useState(null);
  const [healthScores, setHealthScores] = useState({
    overall: 92, engine: 87, transmission: 99, electrical: 95, brakes: 92, battery: 85, tyres: 90, cooling: 80
  });
  const [dtcs, setDtcs] = useState([
    { id: '1', code: 'P0115', system: 'COOLING', severity: 'HIGH', description: 'Coolant Temperature Sensor Circuit Malfunction', action: 'Inspect sensor connector and coolant levels.' }
  ]);
  const [predictions, setPredictions] = useState([
    { id: '1', component: 'Cooling System Pump', probability: 76, date: '2026-07-15', confidence: 85, action: 'Replace coolant pump during next standard service interval.' },
    { id: '2', component: 'Battery Unit', probability: 55, date: '2026-08-01', confidence: 70, action: 'Inspect terminal corrosion and verify alternater output.' }
  ]);
  
  // Historical timeline trend data
  const healthHistory = [
    { date: '06-25', score: 95 },
    { date: '06-26', score: 94 },
    { date: '06-27', score: 94 },
    { date: '06-28', score: 90 },
    { date: '06-29', score: 90 },
    { date: '06-30', score: 86 },
    { date: '07-01', score: 80 }
  ];

  useEffect(() => {
    const loadVehicles = async () => {
      const data = await fetchService('vehicle', 'vehicles');
      setVehicles(data || []);
      if (data && data.length > 0) {
        setSelectedVehicle(data[0]);
      }
    };
    loadVehicles();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div>
        <h1 style={{ fontSize: '32px', margin: '0 0 8px' }}>Vehicle Health Diagnostics</h1>
        <p style={{ color: 'var(--text-tertiary)' }}>OBD telemetry diagnostics, active trouble codes, and AI-predicted failure probabilities</p>
      </div>

      <div style={{ display: 'flex', gap: '24px', alignItems: 'flex-start' }}>
        {/* Left Master List */}
        <div style={{ width: '320px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '600' }}>Active Fleet</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {vehicles.map(v => (
              <div
                key={v.id}
                className="card"
                style={{
                  padding: '16px',
                  cursor: 'pointer',
                  borderColor: selectedVehicle?.id === v.id ? 'var(--accent)' : 'var(--border-color)',
                  backgroundColor: selectedVehicle?.id === v.id ? 'var(--bg-tertiary)' : 'var(--bg-secondary)'
                }}
                onClick={() => setSelectedVehicle(v)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: '600', color: 'var(--text-primary)' }}>{v.registrationNumber || v.regNum}</span>
                  <span style={{
                    fontSize: '13px',
                    fontWeight: '600',
                    color: v.id === 'v1' ? 'var(--danger)' : 'var(--success)'
                  }}>{v.id === 'v1' ? '80%' : '98%'}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px', fontSize: '12px', color: 'var(--text-tertiary)' }}>
                  <span>{v.make} {v.model}</span>
                  <span>{v.id === 'v1' ? '1 DTC' : '0 DTC'}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Details Panel */}
        {selectedVehicle && (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {/* Top Multi-Dimensional Scores Grid */}
            <Card title={`${selectedVehicle.registrationNumber || selectedVehicle.regNum} Health Summary`}>
              <div style={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: '24px' }}>
                {/* Overall Score */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', borderRight: '1px solid var(--border-color)', paddingRight: '24px' }}>
                  <div style={{
                    width: '100px',
                    height: '100px',
                    borderRadius: '50%',
                    border: '8px solid var(--bg-tertiary)',
                    borderTopColor: selectedVehicle.id === 'v1' ? 'var(--danger)' : 'var(--success)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '22px',
                    fontWeight: '700',
                    color: 'var(--text-primary)',
                    marginBottom: '8px'
                  }}>
                    {selectedVehicle.id === 'v1' ? '80%' : '98%'}
                  </div>
                  <span style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Overall Score</span>
                </div>

                {/* Sub Components Index */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px' }}>
                  {[
                    { name: 'Engine', score: selectedVehicle.id === 'v1' ? 87 : 100 },
                    { name: 'Transmission', score: 99 },
                    { name: 'Electrical', score: 95 },
                    { name: 'Brakes', score: 92 },
                    { name: 'Battery', score: selectedVehicle.id === 'v1' ? 85 : 100 },
                    { name: 'Tyres', score: 90 },
                    { name: 'Emissions', score: 98 },
                    { name: 'Cooling', score: selectedVehicle.id === 'v1' ? 80 : 100 }
                  ].map((comp, idx) => (
                    <div key={idx} style={{
                      backgroundColor: 'var(--bg-tertiary)',
                      padding: '12px',
                      borderRadius: 'var(--radius-sm)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '4px'
                    }}>
                      <span style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>{comp.name}</span>
                      <span style={{
                        fontSize: '16px',
                        fontWeight: '700',
                        color: comp.score >= 90 ? 'var(--success)' : comp.score >= 80 ? 'var(--warning)' : 'var(--danger)'
                      }}>{comp.score}%</span>
                    </div>
                  ))}
                </div>
              </div>
            </Card>

            {/* Dials Row */}
            <div className="grid grid-cols-4">
              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Thermometer size={24} style={{ color: selectedVehicle.id === 'v1' ? 'var(--danger)' : 'var(--success)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Coolant Temp</div>
                    <h3 style={{ fontSize: '18px' }}>{selectedVehicle.id === 'v1' ? '106.5°C' : '82.0°C'}</h3>
                  </div>
                </div>
              </Card>

              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Battery size={24} style={{ color: 'var(--success)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Battery Voltage</div>
                    <h3 style={{ fontSize: '18px' }}>12.4 V</h3>
                  </div>
                </div>
              </Card>

              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Activity size={24} style={{ color: 'var(--accent)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Oil Pressure</div>
                    <h3 style={{ fontSize: '18px' }}>320 kPa</h3>
                  </div>
                </div>
              </Card>

              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Flame size={24} style={{ color: 'var(--warning)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Engine Load</div>
                    <h3 style={{ fontSize: '18px' }}>85.2%</h3>
                  </div>
                </div>
              </Card>
            </div>

            {/* predictions and timeline charts */}
            <div className="grid grid-cols-2">
              <Card title="Historical Score Trend (30 Days)">
                <div style={{ width: '100%', height: '220px', marginTop: '16px' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={selectedVehicle.id === 'v1' ? healthHistory : [{ date: '07-01', score: 98 }]}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                      <XAxis dataKey="date" stroke="var(--text-tertiary)" fontSize={11} />
                      <YAxis domain={[60, 100]} stroke="var(--text-tertiary)" fontSize={11} />
                      <Tooltip contentStyle={{ backgroundColor: 'var(--bg-secondary)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }} />
                      <Line type="monotone" dataKey="score" stroke="var(--danger)" strokeWidth={2.5} activeDot={{ r: 6 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </Card>

              <Card title="AI Predictive Maintenance">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '12px' }}>
                  {(selectedVehicle.id === 'v1' ? predictions : []).map(p => (
                    <div key={p.id} style={{
                      padding: '16px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--border-color)',
                      backgroundColor: 'var(--bg-tertiary)'
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                        <span style={{ fontWeight: '600', color: 'var(--text-primary)' }}>{p.component}</span>
                        <span style={{
                          fontSize: '12px',
                          fontWeight: '700',
                          color: p.probability >= 70 ? 'var(--danger)' : 'var(--warning)'
                        }}>
                          {p.probability}% Failure Risk
                        </span>
                      </div>
                      <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{p.action}</p>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '8px', fontSize: '11px', color: 'var(--text-tertiary)' }}>
                        <span>Target Date: {p.date}</span>
                        <span>Confidence Level: {p.confidence}%</span>
                      </div>
                    </div>
                  ))}
                  {selectedVehicle.id !== 'v1' && (
                    <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-tertiary)', fontSize: '14px' }}>
                      All components performing inside optimal margins. No critical maintenance predictions recorded.
                    </div>
                  )}
                </div>
              </Card>
            </div>

            {/* active trouble codes */}
            {selectedVehicle.id === 'v1' && (
              <Card title="Active Diagnostic Trouble Codes (DTCs)">
                <Table
                  headers={['Code', 'System', 'Severity', 'Description', 'Recommended Action']}
                  data={dtcs}
                  renderRow={(dtc) => (
                    <tr key={dtc.id}>
                      <td style={{ fontWeight: '700', color: 'var(--danger)' }}>{dtc.code}</td>
                      <td><code>{dtc.system}</code></td>
                      <td>
                        <span className="badge badge-high" style={{ padding: '2px 6px', fontSize: '11px' }}>
                          {dtc.severity}
                        </span>
                      </td>
                      <td style={{ fontSize: '13px' }}>{dtc.description}</td>
                      <td style={{ fontSize: '13px' }}>{dtc.action}</td>
                    </tr>
                  )}
                />
              </Card>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
export default HealthPanel;
