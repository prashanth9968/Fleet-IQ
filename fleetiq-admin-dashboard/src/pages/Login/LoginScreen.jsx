import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../contexts/AppContext';
import { Truck } from 'lucide-react';
import '../../styles/components.css';

export const LoginScreen = () => {
  const [email, setEmail] = useState('admin@fleetiq.com');
  const [password, setPassword] = useState('password');
  const [slug, setSlug] = useState('omega-logistics');
  const [error, setError] = useState('');
  const { login } = useApp();
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!email || !password || !slug) {
      setError('Please fill in all fields.');
      return;
    }

    // Mock login authentication details matching standard seed data
    const token = 'mock-jwt-token-12345';
    const tenantDetails = {
      id: '00000000-0000-0000-0000-000000000000',
      slug: slug
    };
    const userDetails = {
      firstName: 'Admin',
      lastName: 'User',
      email: email
    };

    login(token, tenantDetails, userDetails);
    navigate('/');
  };

  return (
    <div style={{
      display: 'flex',
      minHeight: '100vh',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: 'var(--bg-primary)'
    }}>
      <div className="card" style={{ width: '420px', padding: '40px' }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{
            display: 'inline-flex',
            padding: '12px',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--bg-tertiary)',
            color: 'var(--accent)',
            marginBottom: '16px'
          }}>
            <Truck size={32} />
          </div>
          <h2 style={{ fontSize: '28px', fontWeight: '700' }}>Welcome to FleetIQ</h2>
          <p style={{ color: 'var(--text-tertiary)', fontSize: '14px', marginTop: '8px' }}>
            Enterprise multi-tenant fleet operations
          </p>
        </div>

        {error && (
          <div style={{
            backgroundColor: 'var(--danger-bg)',
            color: 'var(--danger)',
            padding: '12px 16px',
            borderRadius: 'var(--radius-sm)',
            fontSize: '14px',
            marginBottom: '20px',
            fontWeight: '500'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">Workspace Domain / Tenant Slug</label>
            <input
              type="text"
              className="input-field"
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              placeholder="e.g. omega-logistics"
            />
          </div>

          <div className="input-group">
            <label className="input-label">Email Address</label>
            <input
              type="email"
              className="input-field"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@fleetiq.com"
            />
          </div>

          <div className="input-group">
            <label className="input-label">Password</label>
            <input
              type="password"
              className="input-field"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '12px', padding: '12px' }}>
            Sign In to Workspace
          </button>
        </form>
      </div>
    </div>
  );
};
export default LoginScreen;
