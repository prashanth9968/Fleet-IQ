import React, { useState, useEffect, useRef } from 'react';
import { Card } from '../../components/common/Card';
import { fetchService } from '../../services/api';
import { Play, Square, Flame, Droplets, Gauge, ShieldAlert, Zap, Thermometer, AlertTriangle, Shield, Settings, Battery } from 'lucide-react';
import '../../styles/components.css';

export const SimulatorPanel = () => {
  const [logs, setLogs] = useState([
    `[${new Date().toLocaleTimeString()}] Telemetry simulator initialized. Ready to begin fleet simulation.`
  ]);
  const [activeFleet, setActiveFleet] = useState(false);
  const logsEndRef = useRef(null);

  const addLog = (msg) => {
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${msg}`]);
  };

  useEffect(() => {
    if (logsEndRef.current) {
      logsEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [logs]);

  // Handle Simulator Actions
  const handleStartFleet = () => {
    setActiveFleet(true);
    addLog('START_FLEET: Simulating telemetry for 50 active trucks...');
    addLog('MQTT: Connection established at mqtt://localhost:1883.');
    addLog('Gateway: Listening for Teltonika/Ruptela payload broadcasts.');
  };

  const handleStopFleet = () => {
    setActiveFleet(false);
    addLog('STOP_FLEET: Stopped simulation loops. Ingestion pipeline idle.');
  };

  const triggerFuelTheft = async () => {
    addLog('TRIGGER: Initiating stationary fuel theft simulation for Vehicle KA-01-MJ-1024...');
    addLog('MQTT -> raw.telemetry: Fuel level dropped from 68% to 62% (stationary, ignition: false)');
    setTimeout(() => {
      addLog('Fuel Service: Anomaly detected! Stationary drop > theft threshold (5.0L). Confidence: 90%.');
      addLog('Kafka -> system.alerts: Suspected theft alert published to centralized queue.');
    }, 1500);
  };

  const triggerRefuel = () => {
    addLog('TRIGGER: Initiating refuel fill-up simulation for Vehicle KA-03-MK-4512...');
    addLog('MQTT -> raw.telemetry: Fuel level increased from 12% to 95% (ignition: false)');
    setTimeout(() => {
      addLog('Fuel Service: Refuel event parsed. Volume added: 308.0 Litres. Location: Bangalore Depot 1.');
      addLog('Kafka -> fuel.refuels: Refuel event published to ledger.');
    }, 1500);
  };

  const triggerOverspeed = () => {
    addLog('TRIGGER: Simulating overspeed event inside Geofence Zone for Vehicle DL-01-AA-5678...');
    addLog('MQTT -> raw.telemetry: Speed recorded at 62 km/h inside restricted speed zone (max 30 km/h)');
    setTimeout(() => {
      addLog('Alerts Service: Overspeed detected inside geofence boundary (Warehouse Zone).');
      addLog('Kafka -> alert.analytics: Published OVERSPEED_IN_GEOFENCE event (Priority: HIGH).');
    }, 1500);
  };

  const triggerGeofenceEntry = () => {
    addLog('TRIGGER: Simulating geofence ENTER/EXIT transition for Vehicle KA-01-MJ-1024...');
    addLog('MQTT -> raw.telemetry: Vehicle coordinates intersection with Warehouse geofence boundaries.');
    setTimeout(() => {
      addLog('Alerts Service: ENTER geofence event triggered. Dwell timer started.');
    }, 1000);
  };

  const triggerFatigue = () => {
    addLog('TRIGGER: Simulating driver safety violation (fatigue) for John Doe...');
    addLog('DMS Dashcam -> driving.events: Driver eye closure detected > 3.0 seconds.');
    setTimeout(() => {
      addLog('Driver Service: Safety violation calculated. Score deducted (-10 points).');
      addLog('Kafka -> driver.scores: Safety score updated for Driver EMP-001 (New Score: 82.5).');
    }, 1500);
  };

  // Health Diagnostics Simulation Triggers
  const triggerEngineOverheat = () => {
    addLog('TRIGGER: Simulating Engine Overheat diagnostic telemetry for KA-01-MJ-1024...');
    addLog('OBD -> vehicle.health.events: Coolant temperature spiked to 108.5°C (Threshold 105.0°C)');
    setTimeout(() => {
      addLog('Vehicle Health Service: Critical overheat condition recorded.');
      addLog('Kafka -> vehicle.health.alerts: Emitted ENGINE_OVERHEAT alert (Priority: CRITICAL).');
    }, 1500);
  };

  const triggerOilPressureDrop = () => {
    addLog('TRIGGER: Simulating sudden Oil Pressure Drop for Vehicle DL-01-AA-5678...');
    addLog('OBD -> vehicle.health.events: Engine oil pressure dropped to 135 kPa (Threshold 150 kPa)');
    setTimeout(() => {
      addLog('Vehicle Health Service: Alert! Low engine oil pressure detected. Overall health score updated.');
      addLog('Kafka -> vehicle.health.alerts: Emitted LOW_OIL_PRESSURE alert.');
    }, 1500);
  };

  const triggerBatteryDrop = () => {
    addLog('TRIGGER: Simulating low battery voltage diagnostic telemetry...');
    addLog('OBD -> vehicle.health.events: Alternator output dropping, voltage recorded at 11.2 V');
    setTimeout(() => {
      addLog('Vehicle Health Service: Battery level critical. Electrical component score: 85%.');
      addLog('Kafka -> vehicle.health.alerts: Emitted LOW_BATTERY alert (Priority: HIGH).');
    }, 1500);
  };

  const triggerTransmissionFailure = () => {
    addLog('TRIGGER: Simulating critical transmission slip for Vehicle KA-03-MK-4512...');
    addLog('CAN Bus -> vehicle.health.events: High torque slippage ratio detected in gear ratio 3.');
    setTimeout(() => {
      addLog('Vehicle Health Service: Diagnostic event mapped. Transmission failure probability: 68%.');
      addLog('Kafka -> vehicle.health.analytics: Published updated transmission wear score.');
    }, 1500);
  };

  const triggerBrakeWear = () => {
    addLog('TRIGGER: Simulating advanced brake pad wear diagnostics...');
    addLog('OBD -> vehicle.health.events: Brake pad lining sensor thickness recorded at 2.2mm (Limit 3.0mm)');
    setTimeout(() => {
      addLog('Vehicle Health Service: Brake component score updated: 72% overall health.');
      addLog('Kafka -> maintenance.events: Published SERVICE_DUE event for brake replacement.');
    }, 1500);
  };

  const triggerCoolantLeak = () => {
    addLog('TRIGGER: Simulating cooling system leak event...');
    addLog('OBD -> vehicle.health.events: Expansion tank coolant level dropping below safety thresholds.');
    setTimeout(() => {
      addLog('Vehicle Health Service: Cooling score: 80%. AI Prediction: Cooling pump failure probability 76%.');
      addLog('Kafka -> maintenance.events: SERVICE_DUE alert triggered.');
    }, 1500);
  };

  const triggerHighEngineLoad = () => {
    addLog('TRIGGER: Simulating high engine load diagnostics...');
    addLog('OBD -> vehicle.health.events: Throttle position 98%, engine load index at 97%');
    setTimeout(() => {
      addLog('Vehicle Health Service: High engine load warning logged.');
      addLog('Kafka -> vehicle.health.alerts: Emitted HIGH_ENGINE_LOAD alert.');
    }, 1500);
  };

  const triggerLowTirePressure = () => {
    addLog('TRIGGER: Simulating tire pressure drop (TPMS)...');
    addLog('OBD -> vehicle.health.events: Front-right tire pressure dropped to 26 psi (Threshold 32 psi)');
    setTimeout(() => {
      addLog('Vehicle Health Service: TPMS warning logged. Tyre component score: 90%.');
    }, 1200);
  };

  const triggerCriticalDtc = () => {
    addLog('TRIGGER: Injecting critical DTC malfunction code P0300 (Engine Misfire)...');
    addLog('OBD -> vehicle.health.events: Malfunction Indicator Lamp (MIL) active. Broadcast DTC: P0300.');
    setTimeout(() => {
      addLog('Vehicle Health Service: Critical trouble event active. Mapped P0300 -> Engine Misfire.');
      addLog('Kafka -> vehicle.health.alerts: Emitted CRITICAL_DTC alert (Priority: CRITICAL).');
    }, 1500);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px', height: 'calc(100vh - 150px)' }}>
      <div>
        <h1 style={{ fontSize: '32px', margin: '0 0 8px' }}>Developer Telemetry Simulator</h1>
        <p style={{ color: 'var(--text-tertiary)' }}>Trigger live multi-tenant telemetry anomalies, fuel events, and safety score calculations</p>
      </div>

      <div style={{ display: 'flex', gap: '24px', flex: 1, overflow: 'hidden' }}>
        {/* Left Side: Simulation Controls */}
        <div style={{ width: '380px', display: 'flex', flexDirection: 'column', gap: '16px', overflowY: 'auto', paddingRight: '8px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '600' }}>Control panel</h3>
          
          <Card style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <button 
              className={`btn ${activeFleet ? 'btn-danger' : 'btn-primary'}`} 
              onClick={activeFleet ? handleStopFleet : handleStartFleet}
              style={{ padding: '12px', justifyContent: 'flex-start' }}
            >
              {activeFleet ? <Square size={16} /> : <Play size={16} />}
              <span>{activeFleet ? 'Stop Demo Fleet' : 'Start Demo Fleet (50 Trucks)'}</span>
            </button>

            <div style={{ margin: '8px 0', borderTop: '1px solid var(--border-color)' }} />

            <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--text-tertiary)', textTransform: 'uppercase', marginBottom: '4px' }}>Fleet Tracking & Fuel</div>
            
            <button className="btn btn-secondary" onClick={triggerFuelTheft} style={{ justifyContent: 'flex-start' }}>
              <Flame size={15} style={{ color: 'var(--danger)' }} />
              <span>Simulate Fuel Theft</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerRefuel} style={{ justifyContent: 'flex-start' }}>
              <Droplets size={15} style={{ color: 'var(--success)' }} />
              <span>Simulate Refueling</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerOverspeed} style={{ justifyContent: 'flex-start' }}>
              <Gauge size={15} style={{ color: 'var(--warning)' }} />
              <span>Simulate Geofence Overspeed</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerGeofenceEntry} style={{ justifyContent: 'flex-start' }}>
              <Zap size={15} style={{ color: 'var(--accent)' }} />
              <span>Simulate Geofence Transition</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerFatigue} style={{ justifyContent: 'flex-start' }}>
              <ShieldAlert size={15} style={{ color: 'var(--danger)' }} />
              <span>Simulate Driver Fatigue</span>
            </button>

            <div style={{ margin: '8px 0', borderTop: '1px solid var(--border-color)' }} />
            
            <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--text-tertiary)', textTransform: 'uppercase', marginBottom: '4px' }}>OBD Diagnostics & Health</div>

            <button className="btn btn-secondary" onClick={triggerEngineOverheat} style={{ justifyContent: 'flex-start' }}>
              <Thermometer size={15} style={{ color: 'var(--danger)' }} />
              <span>Simulate Engine Overheat</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerOilPressureDrop} style={{ justifyContent: 'flex-start' }}>
              <AlertTriangle size={15} style={{ color: 'var(--danger)' }} />
              <span>Simulate Oil Pressure Drop</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerBatteryDrop} style={{ justifyContent: 'flex-start' }}>
              <Battery size={15} style={{ color: 'var(--warning)' }} />
              <span>Simulate Battery Drop</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerTransmissionFailure} style={{ justifyContent: 'flex-start' }}>
              <Settings size={15} style={{ color: 'var(--danger)' }} />
              <span>Simulate Transmission Failure</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerBrakeWear} style={{ justifyContent: 'flex-start' }}>
              <Shield size={15} style={{ color: 'var(--warning)' }} />
              <span>Simulate Brake Wear</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerCoolantLeak} style={{ justifyContent: 'flex-start' }}>
              <Droplets size={15} style={{ color: 'var(--warning)' }} />
              <span>Simulate Coolant Leak</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerHighEngineLoad} style={{ justifyContent: 'flex-start' }}>
              <Flame size={15} style={{ color: 'var(--warning)' }} />
              <span>Simulate High Engine Load</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerLowTirePressure} style={{ justifyContent: 'flex-start' }}>
              <Settings size={15} style={{ color: 'var(--accent)' }} />
              <span>Simulate Low Tire Pressure</span>
            </button>

            <button className="btn btn-secondary" onClick={triggerCriticalDtc} style={{ justifyContent: 'flex-start' }}>
              <ShieldAlert size={15} style={{ color: 'var(--danger)' }} />
              <span>Inject Malfunction DTC (P0300)</span>
            </button>
          </Card>
        </div>

        {/* Right Side: Simulator Terminal Logs */}
        <Card style={{ flex: 1, padding: '24px', display: 'flex', flexDirection: 'column', overflow: 'hidden' }} title="Simulator Terminal Feed">
          <div style={{
            flex: 1,
            backgroundColor: '#0a0b0d',
            color: '#34d399',
            fontFamily: 'var(--font-sans)',
            padding: '20px',
            borderRadius: 'var(--radius-sm)',
            overflowY: 'auto',
            fontSize: '13px',
            lineHeight: '1.6',
            textAlign: 'left'
          }}>
            {logs.map((log, index) => (
              <div key={index} style={{ marginBottom: '8px', wordBreak: 'break-all' }}>
                {log}
              </div>
            ))}
            <div ref={logsEndRef} />
          </div>
        </Card>
      </div>
    </div>
  );
};
export default SimulatorPanel;
