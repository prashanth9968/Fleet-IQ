import { useEffect, useState } from 'react';

const PORTS = {
  auth: 8081,
  vehicle: 8083,
  tracking: 8082,
  fuel: 8084,
  alerts: 8085,
  driver: 8086
};

// Standard multi-tenant mock data fallbacks
const MOCK_VEHICLES = [
  { id: 'v1', registrationNumber: 'KA-01-MJ-1024', make: 'TATA', model: 'Prima 4930.S', status: 'ACTIVE', fuelTankCapacityLitres: 400.0, odometerReadingKm: 45210.5, activeDriverName: 'John Doe', speedKmh: 45.0, latitude: 12.971598, longitude: 77.594562 },
  { id: 'v2', registrationNumber: 'KA-03-MK-4512', make: 'Leyland', model: 'U-Truck 3718', status: 'ACTIVE', fuelTankCapacityLitres: 350.0, odometerReadingKm: 31250.2, activeDriverName: 'Jane Smith', speedKmh: 0.0, latitude: 12.978598, longitude: 77.604562 },
  { id: 'v3', registrationNumber: 'MH-12-NN-8899', make: 'Volvo', model: 'FMX 460', status: 'IN_MAINTENANCE', fuelTankCapacityLitres: 450.0, odometerReadingKm: 98120.4, activeDriverName: null, speedKmh: 0.0, latitude: 12.961598, longitude: 77.584562 },
  { id: 'v4', registrationNumber: 'DL-01-AA-5678', make: 'BharatBenz', model: '3528C', status: 'ACTIVE', fuelTankCapacityLitres: 380.0, odometerReadingKm: 18450.0, activeDriverName: 'Robert Vance', speedKmh: 62.0, latitude: 12.981598, longitude: 77.614562 }
];

const MOCK_DRIVERS = [
  { id: 'd1', firstName: 'John', lastName: 'Doe', employeeId: 'EMP-001', phone: '+91 98765 43210', status: 'ACTIVE', safetyScore: 92.5, email: 'john@omega.com', licenseNumber: 'DL-123456', licenseExpiry: '2028-10-15', currentVehicle: 'v1' },
  { id: 'd2', firstName: 'Jane', lastName: 'Smith', employeeId: 'EMP-002', phone: '+91 87654 32109', status: 'ACTIVE', safetyScore: 96.0, email: 'jane@omega.com', licenseNumber: 'DL-654321', licenseExpiry: '2029-05-12', currentVehicle: 'v2' },
  { id: 'd3', firstName: 'Robert', lastName: 'Vance', employeeId: 'EMP-003', phone: '+91 76543 21098', status: 'ACTIVE', safetyScore: 84.0, email: 'robert@omega.com', licenseNumber: 'DL-987654', licenseExpiry: '2027-11-20', currentVehicle: 'v4' },
  { id: 'd4', firstName: 'Sarah', lastName: 'Connor', employeeId: 'EMP-004', phone: '+91 65432 10987', status: 'ON_LEAVE', safetyScore: 91.0, email: 'sarah@omega.com', licenseNumber: 'DL-456789', licenseExpiry: '2030-01-05', currentVehicle: null }
];

const MOCK_ALERTS = [
  { id: 'a1', vehicleId: 'v1', alertType: 'FUEL_THEFT', priority: 'CRITICAL', message: 'Sudden fuel drop detected (12.5L) while stationary', status: 'OPEN', createdAt: new Date(Date.now() - 30 * 60000).toISOString() },
  { id: 'a2', vehicleId: 'v4', alertType: 'OVERSPEED_IN_GEOFENCE', priority: 'HIGH', message: 'Vehicle exceeded speed limit inside Warehouse Zone (62 km/h vs 30 km/h)', status: 'OPEN', createdAt: new Date(Date.now() - 5 * 60000).toISOString() },
  { id: 'a3', vehicleId: 'v2', alertType: 'EXCESSIVE_DWELL_TIME', priority: 'MEDIUM', message: 'Vehicle exceeded maximum dwell time in Yard 3 (3.5 hours)', status: 'OPEN', createdAt: new Date(Date.now() - 90 * 60000).toISOString() }
];

const MOCK_FUEL_CURVES = [
  { time: '10:00', level: 98.2 },
  { time: '11:00', level: 92.5 },
  { time: '12:00', level: 86.4 },
  { time: '13:00', level: 80.1 },
  { time: '14:00', level: 74.0 },
  { time: '15:00', level: 68.3 }
];

export const fetchService = async (serviceName, endpoint, options = {}) => {
  const port = PORTS[serviceName];
  const url = `http://localhost:${port}/api/v1/${endpoint}`;
  
  const tenantId = localStorage.getItem('tenant_id') || '00000000-0000-0000-0000-000000000000';
  const token = localStorage.getItem('auth_token');

  const headers = {
    'Content-Type': 'application/json',
    'X-Tenant-ID': tenantId,
    ...(token && { 'Authorization': `Bearer ${token}` }),
    ...options.headers
  };

  try {
    const response = await fetch(url, { ...options, headers });
    if (!response.ok) throw new Error(`HTTP error ${response.status}`);
    return await response.json();
  } catch (error) {
    console.warn(`Local Service ${serviceName} is not available. Falling back to mock dataset.`);
    // Custom fallbacks based on endpoint
    if (endpoint.startsWith('vehicles')) {
      return MOCK_VEHICLES;
    }
    if (endpoint.startsWith('drivers/safety-leaderboard')) {
      return MOCK_DRIVERS.map(d => ({
        driverId: d.id,
        overallScore: d.safetyScore,
        totalEvents: 4,
        totalTrips: 12
      }));
    }
    if (endpoint.startsWith('drivers')) {
      return MOCK_DRIVERS;
    }
    if (endpoint.startsWith('alerts') || endpoint.startsWith('alert_history') || endpoint.startsWith('alert-history')) {
      return MOCK_ALERTS;
    }
    if (endpoint.startsWith('fuel')) {
      return MOCK_FUEL_CURVES;
    }
    return [];
  }
};
