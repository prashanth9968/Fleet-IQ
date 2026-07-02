import React, { createContext, useContext, useState, useEffect } from 'react';

const AppContext = createContext();

export const AppProvider = ({ children }) => {
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'dark');
  const [tenantId, setTenantId] = useState(() => localStorage.getItem('tenant_id') || '00000000-0000-0000-0000-000000000000');
  const [tenantSlug, setTenantSlug] = useState(() => localStorage.getItem('tenant_slug') || 'default-tenant');
  const [authToken, setAuthToken] = useState(() => localStorage.getItem('auth_token') || null);
  const [isAuthenticated, setIsAuthenticated] = useState(() => !!localStorage.getItem('auth_token'));
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user')) || null;
    } catch {
      return null;
    }
  });

  // Handle Theme switching
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  const login = (token, tenantDetails, userDetails) => {
    localStorage.setItem('auth_token', token);
    localStorage.setItem('tenant_id', tenantDetails.id);
    localStorage.setItem('tenant_slug', tenantDetails.slug);
    localStorage.setItem('user', JSON.stringify(userDetails));

    setAuthToken(token);
    setTenantId(tenantDetails.id);
    setTenantSlug(tenantDetails.slug);
    setUser(userDetails);
    setIsAuthenticated(true);
  };

  const logout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('tenant_id');
    localStorage.removeItem('tenant_slug');
    localStorage.removeItem('user');

    setAuthToken(null);
    setTenantId('00000000-0000-0000-0000-000000000000');
    setTenantSlug('default-tenant');
    setUser(null);
    setIsAuthenticated(false);
  };

  return (
    <AppContext.Provider value={{
      theme,
      toggleTheme,
      tenantId,
      setTenantId,
      tenantSlug,
      setTenantSlug,
      authToken,
      isAuthenticated,
      user,
      login,
      logout
    }}>
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => useContext(AppContext);
