import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppProvider, useApp } from './contexts/AppContext';
import { DashboardLayout } from './layouts/DashboardLayout';
import { LoginScreen } from './pages/Login/LoginScreen';
import { OverviewDashboard } from './pages/Dashboard/OverviewDashboard';
import { VehicleList } from './pages/Vehicles/VehicleList';
import { LiveMap } from './pages/Tracking/LiveMap';
import { FuelDashboard } from './pages/Fuel/FuelDashboard';
import { AlertsPanel } from './pages/Alerts/AlertsPanel';
import { DriverProfiles } from './pages/Drivers/DriverProfiles';
import { HealthPanel } from './pages/Health/HealthPanel';
import { MaintenanceDashboard } from './pages/Maintenance/MaintenanceDashboard';
import { ReportsDashboard } from './pages/Reports/ReportsDashboard';
import { SimulatorPanel } from './pages/DeveloperTools/SimulatorPanel';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useApp();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
};

function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginScreen />} />
          
          <Route path="/" element={
            <ProtectedRoute>
              <DashboardLayout>
                <OverviewDashboard />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/vehicles" element={
            <ProtectedRoute>
              <DashboardLayout>
                <VehicleList />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/tracking" element={
            <ProtectedRoute>
              <DashboardLayout>
                <LiveMap />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/fuel" element={
            <ProtectedRoute>
              <DashboardLayout>
                <FuelDashboard />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/alerts" element={
            <ProtectedRoute>
              <DashboardLayout>
                <AlertsPanel />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/drivers" element={
            <ProtectedRoute>
              <DashboardLayout>
                <DriverProfiles />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/health" element={
            <ProtectedRoute>
              <DashboardLayout>
                <HealthPanel />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/maintenance" element={
            <ProtectedRoute>
              <DashboardLayout>
                <MaintenanceDashboard />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/reports" element={
            <ProtectedRoute>
              <DashboardLayout>
                <ReportsDashboard />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="/developer-tools" element={
            <ProtectedRoute>
              <DashboardLayout>
                <SimulatorPanel />
              </DashboardLayout>
            </ProtectedRoute>
          } />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AppProvider>
  );
}

export default App;
