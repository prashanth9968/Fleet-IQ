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

  useEffect(() => {
    const loadVehicles = async () => {
      try {
        const data = await fetchService('vehicle', 'vehicles');
        setVehicles(data || []);
        if (data && data.length > 0) {
          setSelectedVehicle(data[0]);
        }
      } catch (err) {
        console.error("Failed to load vehicles", err);
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
            {vehicles.length === 0 ? (
              <div style={{ color: 'var(--text-tertiary)', fontSize: '14px', fontStyle: 'italic' }}>No vehicles found.</div>
            ) : vehicles.map(v => (
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
                  <span style={{ fontWeight: '600', color: 'var(--text-primary)' }}>{v.registrationNumber || v.regNum || 'Unknown Reg'}</span>
                  {v.healthScore !== undefined && (
                    <span style={{
                      fontSize: '13px',
                      fontWeight: '600',
                      color: v.healthScore < 85 ? 'var(--danger)' : 'var(--success)'
                    }}>{v.healthScore}%</span>
                  )}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px', fontSize: '12px', color: 'var(--text-tertiary)' }}>
                  <span>{v.make} {v.model}</span>
                  <span>{v.dtcCount !== undefined ? `${v.dtcCount} DTC` : 'No data'}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Details Panel */}
        {selectedVehicle && (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {/* Top Multi-Dimensional Scores Grid */}
            <Card title={`${selectedVehicle.registrationNumber || selectedVehicle.regNum || 'Vehicle'} Health Summary`}>
              <div style={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: '24px' }}>
                {/* Overall Score */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', borderRight: '1px solid var(--border-color)', paddingRight: '24px' }}>
                  {selectedVehicle.healthScore !== undefined ? (
                    <>
                      <div style={{
                        width: '100px',
                        height: '100px',
                        borderRadius: '50%',
                        border: '8px solid var(--bg-tertiary)',
                        borderTopColor: selectedVehicle.healthScore < 85 ? 'var(--danger)' : 'var(--success)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '22px',
                        fontWeight: '700',
                        color: 'var(--text-primary)',
                        marginBottom: '8px'
                      }}>
                        {selectedVehicle.healthScore}%
                      </div>
                      <span style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Overall Score</span>
                    </>
                  ) : (
                    <div style={{ textAlign: 'center', color: 'var(--text-tertiary)', fontSize: '14px', fontStyle: 'italic' }}>
                      No diagnostics received yet.
                    </div>
                  )}
                </div>

                {/* Sub Components Index */}
                {selectedVehicle.components ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px' }}>
                    {[
                      { name: 'Engine', score: selectedVehicle.components.engine },
                      { name: 'Transmission', score: selectedVehicle.components.transmission },
                      { name: 'Electrical', score: selectedVehicle.components.electrical },
                      { name: 'Brakes', score: selectedVehicle.components.brakes },
                      { name: 'Battery', score: selectedVehicle.components.battery },
                      { name: 'Tyres', score: selectedVehicle.components.tyres },
                      { name: 'Emissions', score: selectedVehicle.components.emissions },
                      { name: 'Cooling', score: selectedVehicle.components.cooling }
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
                        }}>{comp.score !== undefined ? `${comp.score}%` : 'N/A'}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-tertiary)', fontStyle: 'italic', fontSize: '14px' }}>
                    Waiting for component telemetry...
                  </div>
                )}
              </div>
            </Card>

            {/* Dials Row */}
            <div className="grid grid-cols-4">
              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Thermometer size={24} style={{ color: selectedVehicle.coolantTemp && selectedVehicle.coolantTemp > 100 ? 'var(--danger)' : 'var(--success)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Coolant Temp</div>
                    <h3 style={{ fontSize: '18px' }}>{selectedVehicle.coolantTemp !== undefined ? `${selectedVehicle.coolantTemp}°C` : 'N/A'}</h3>
                  </div>
                </div>
              </Card>

              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Battery size={24} style={{ color: 'var(--success)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Battery Voltage</div>
                    <h3 style={{ fontSize: '18px' }}>{selectedVehicle.batteryVolts !== undefined ? `${selectedVehicle.batteryVolts} V` : 'N/A'}</h3>
                  </div>
                </div>
              </Card>

              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Activity size={24} style={{ color: 'var(--accent)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Oil Pressure</div>
                    <h3 style={{ fontSize: '18px' }}>{selectedVehicle.oilPressure !== undefined ? `${selectedVehicle.oilPressure} kPa` : 'N/A'}</h3>
                  </div>
                </div>
              </Card>

              <Card style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Flame size={24} style={{ color: 'var(--warning)' }} />
                  <div>
                    <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>Engine Load</div>
                    <h3 style={{ fontSize: '18px' }}>{selectedVehicle.engineLoad !== undefined ? `${selectedVehicle.engineLoad}%` : 'N/A'}</h3>
                  </div>
                </div>
              </Card>
            </div>

            {/* predictions and timeline charts */}
            <div className="grid grid-cols-2">
              <Card title="Historical Score Trend (30 Days)">
                <div style={{ width: '100%', height: '220px', marginTop: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {selectedVehicle.healthHistory && selectedVehicle.healthHistory.length > 0 ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={selectedVehicle.healthHistory}>
                        <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                        <XAxis dataKey="date" stroke="var(--text-tertiary)" fontSize={11} />
                        <YAxis domain={[60, 100]} stroke="var(--text-tertiary)" fontSize={11} />
                        <Tooltip contentStyle={{ backgroundColor: 'var(--bg-secondary)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }} />
                        <Line type="monotone" dataKey="score" stroke="var(--danger)" strokeWidth={2.5} activeDot={{ r: 6 }} />
                      </LineChart>
                    </ResponsiveContainer>
                  ) : (
                    <div style={{ color: 'var(--text-tertiary)', fontSize: '14px', fontStyle: 'italic' }}>
                      No telemetry received yet.
                    </div>
                  )}
                </div>
              </Card>

              <Card title="AI Predictive Maintenance">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '12px' }}>
                  {selectedVehicle.predictions && selectedVehicle.predictions.length > 0 ? (
                    selectedVehicle.predictions.map(p => (
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
                    ))
                  ) : (
                    <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-tertiary)', fontSize: '14px', fontStyle: 'italic' }}>
                      All components performing inside optimal margins. No critical maintenance predictions recorded.
                    </div>
                  )}
                </div>
              </Card>
            </div>

            {/* active trouble codes */}
            <Card title="Active Diagnostic Trouble Codes (DTCs)">
              {selectedVehicle.dtcs && selectedVehicle.dtcs.length > 0 ? (
                <Table
                  headers={['Code', 'System', 'Severity', 'Description', 'Recommended Action']}
                  data={selectedVehicle.dtcs}
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
              ) : (
                <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-tertiary)', fontSize: '14px', fontStyle: 'italic' }}>
                  No active DTCs.
                </div>
              )}
            </Card>
          </div>
        )}
      </div>
    </div>
  );
};
export default HealthPanel;
