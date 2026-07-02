import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Card } from '../../components/common/Card';
import { fetchService } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import { Navigation, Play, Square } from 'lucide-react';
import '../../styles/components.css';

// Dynamically load Leaflet CDN assets to prevent server rendering or bundle crashes
export const LiveMap = () => {
  const mapContainer = useRef(null);
  const mapInstance = useRef(null);
  const markers = useRef({});
  const { tenantId } = useApp();
  const [vehicles, setVehicles] = useState([]);
  const [selectedVehicle, setSelectedVehicle] = useState(null);
  const [simulationActive, setSimulationActive] = useState(true);

  useEffect(() => {
    // 1. Fetch initial vehicles
    const initVehicles = async () => {
      const data = await fetchService('vehicle', 'vehicles');
      setVehicles(data || []);
    };
    initVehicles();
  }, []);

  useEffect(() => {
    // 2. Initialize Leaflet Map
    if (mapContainer.current && !mapInstance.current) {
      // Create map
      mapInstance.current = L.map(mapContainer.current).setView([12.971598, 77.594562], 13);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(mapInstance.current);
    }

    return () => {
      if (mapInstance.current) {
        mapInstance.current.remove();
        mapInstance.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!mapInstance.current || vehicles.length === 0) return;

    // Remove old markers
    Object.keys(markers.current).forEach(id => {
      markers.current[id].remove();
    });
    markers.current = {};

    // 3. Create marker icons and add to map
    vehicles.forEach(v => {
      if (v.latitude && v.longitude) {
        const marker = L.marker([v.latitude, v.longitude], {
          icon: L.divIcon({
            className: 'custom-vehicle-marker',
            html: `<div style="
              background-color: ${v.status === 'ACTIVE' ? 'var(--success)' : 'var(--warning)'};
              color: white;
              padding: 6px 12px;
              border-radius: 20px;
              font-weight: 600;
              font-size: 11px;
              box-shadow: 0 4px 6px rgba(0,0,0,0.15);
              white-space: nowrap;
              border: 2px solid var(--bg-secondary);
              display: flex;
              align-items: center;
              gap: 4px;
            ">
              <span style="display:inline-block; transform: rotate(${v.heading || 0}deg);">▲</span>
              ${v.registrationNumber || v.regNum}
            </div>`
          })
        }).addTo(mapInstance.current);

        marker.bindPopup(`
          <div style="font-family: var(--font-sans); color: var(--text-primary);">
            <h4 style="margin:0 0 4px; font-size:14px;">${v.registrationNumber || v.regNum}</h4>
            <p style="margin:0; font-size:12px; color:var(--text-secondary)">Speed: ${Math.round(v.speedKmh)} km/h</p>
            <p style="margin:0; font-size:12px; color:var(--text-secondary)">Driver: ${v.activeDriverName || 'Unassigned'}</p>
          </div>
        `);

        markers.current[v.id] = marker;
      }
    });

    // Zoom map to bounds of active vehicles
    const activeCoords = vehicles.filter(v => v.latitude && v.longitude).map(v => [v.latitude, v.longitude]);
    if (activeCoords.length > 0) {
      mapInstance.current.fitBounds(activeCoords, { padding: [50, 50] });
    }
  }, [vehicles]);

  // 4. Simulate active client-side movement when WebSocket is idle
  useEffect(() => {
    if (!simulationActive) return;

    const interval = setInterval(() => {
      setVehicles(prev => prev.map(v => {
        if (v.status !== 'ACTIVE') return v;
        // Move vehicle coordinates slightly
        const deltaLat = (Math.random() - 0.5) * 0.001;
        const deltaLng = (Math.random() - 0.5) * 0.001;
        const newLat = v.latitude + deltaLat;
        const newLng = v.longitude + deltaLng;
        const speed = 40 + Math.random() * 30; // 40-70 km/h

        // If markers are on map, update position
        if (markers.current[v.id]) {
          markers.current[v.id].setLatLng([newLat, newLng]);
        }

        return {
          ...v,
          latitude: newLat,
          longitude: newLng,
          speedKmh: speed,
          heading: Math.atan2(deltaLng, deltaLat) * (180 / Math.PI)
        };
      }));
    }, 3000);

    return () => clearInterval(interval);
  }, [simulationActive]);

  return (
    <div style={{ display: 'flex', gap: '24px', height: 'calc(100vh - 150px)' }}>
      {/* Side list of vehicles */}
      <div style={{ width: '320px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ fontSize: '20px', margin: 0 }}>Active Fleet</h2>
          <button 
            className="btn btn-secondary" 
            style={{ padding: '6px 12px', fontSize: '12px' }}
            onClick={() => setSimulationActive(!simulationActive)}
          >
            {simulationActive ? <Square size={12} /> : <Play size={12} />}
            <span>{simulationActive ? 'Pause Sim' : 'Start Sim'}</span>
          </button>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '12px' }}>
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
              onClick={() => {
                setSelectedVehicle(v);
                if (mapInstance.current && v.latitude && v.longitude) {
                  mapInstance.current.setView([v.latitude, v.longitude], 15);
                  markers.current[v.id]?.openPopup();
                }
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: '600', color: 'var(--text-primary)' }}>{v.registrationNumber || v.regNum}</span>
                <span style={{ 
                  fontSize: '11px', 
                  fontWeight: '600', 
                  padding: '2px 6px', 
                  borderRadius: '4px',
                  backgroundColor: v.status === 'ACTIVE' ? 'var(--success-bg)' : 'var(--warning-bg)',
                  color: v.status === 'ACTIVE' ? 'var(--success)' : 'var(--warning)'
                }}>{v.status}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px', fontSize: '13px' }}>
                <span>Driver: {v.activeDriverName || 'None'}</span>
                <span style={{ fontWeight: '500', color: 'var(--text-primary)' }}>{Math.round(v.speedKmh)} km/h</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Map display */}
      <Card style={{ flex: 1, padding: 0, overflow: 'hidden', position: 'relative' }}>
        <div ref={mapContainer} style={{ width: '100%', height: '100%' }} />
      </Card>
    </div>
  );
};
export default LiveMap;
