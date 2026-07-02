import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useApp } from '../contexts/AppContext';
import { 
  LayoutDashboard, 
  Map, 
  Truck, 
  Droplet, 
  AlertTriangle, 
  Users, 
  Terminal, 
  Sun, 
  Moon, 
  LogOut,
  HeartPulse,
  Wrench,
  BarChart3
} from 'lucide-react';
import '../styles/components.css';

export const DashboardLayout = ({ children }) => {
  const { theme, toggleTheme, tenantSlug, logout } = useApp();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { name: 'Overview', path: '/', icon: LayoutDashboard },
    { name: 'Live Map', path: '/tracking', icon: Map },
    { name: 'Vehicles', path: '/vehicles', icon: Truck },
    { name: 'Fuel Monitoring', path: '/fuel', icon: Droplet },
    { name: 'Vehicle Health', path: '/health', icon: HeartPulse },
    { name: 'Maintenance', path: '/maintenance', icon: Wrench },
    { name: 'Reports & KPIs', path: '/reports', icon: BarChart3 },
    { name: 'Alerts', path: '/alerts', icon: AlertTriangle },
    { name: 'Drivers', path: '/drivers', icon: Users },
    { name: 'Developer Tools', path: '/developer-tools', icon: Terminal }
  ];

  return (
    <div className="dashboard-layout">
      {/* Sidebar Navigation */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <Truck size={28} style={{ color: 'var(--accent)' }} />
          <span>FleetIQ</span>
        </div>
        
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
              end={item.path === '/'}
            >
              <item.icon size={18} />
              <span>{item.name}</span>
            </NavLink>
          ))}
        </nav>

        <div style={{ marginTop: 'auto', borderTop: '1px solid var(--border-color)', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <button 
            onClick={toggleTheme} 
            className="sidebar-link" 
            style={{ background: 'none', border: 'none', width: '100%', cursor: 'pointer', textAlign: 'left' }}
          >
            {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
            <span>{theme === 'light' ? 'Dark Mode' : 'Light Mode'}</span>
          </button>
          
          <button 
            onClick={handleLogout} 
            className="sidebar-link" 
            style={{ background: 'none', border: 'none', width: '100%', cursor: 'pointer', textAlign: 'left', color: 'var(--danger)' }}
          >
            <LogOut size={18} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Panel Content */}
      <div style={{ display: 'flex', flexDirection: 'column', flex: 1, height: '100vh', overflow: 'hidden' }}>
        <header style={{
          height: '70px',
          backgroundColor: 'var(--bg-secondary)',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 40px'
        }}>
          <div>
            <span style={{ fontSize: '13px', color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Tenant Workspace</span>
            <h2 style={{ fontSize: '18px', fontWeight: '600', textTransform: 'capitalize' }}>{tenantSlug}</h2>
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '50%',
              backgroundColor: 'var(--bg-tertiary)',
              color: 'var(--text-primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: '600',
              fontSize: '14px'
            }}>
              AD
            </div>
          </div>
        </header>

        <main style={{ flex: 1, overflowY: 'auto', padding: '40px', backgroundColor: 'var(--bg-primary)' }}>
          {children}
        </main>
      </div>
    </div>
  );
};
