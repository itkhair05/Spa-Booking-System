import { useState } from 'react';
import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../app/auth/useAuth';

interface NavItem {
  to: string;
  label: string;
  icon: string;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: '▦' },
  { to: '/bookings', label: 'Bookings', icon: '📅' },
  { to: '/customers', label: 'Customers', icon: '👤' },
  { to: '/services', label: 'Services', icon: '✂' },
  { to: '/staff', label: 'Staff', icon: '👥' },
];

interface AppShellProps {
  children: ReactNode;
  /** Page title shown in the top header bar */
  title: string;
}

/**
 * Authenticated application shell.
 * Renders a fixed sidebar (collapsible on mobile) + top header + scrollable main content area.
 */
const AppShell = ({ children, title }: AppShellProps) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const toggleSidebar = () => setIsSidebarOpen((prev) => !prev);

  return (
    <div className="app-shell">
      {/* Mobile Sidebar Overlay */}
      {isSidebarOpen && (
        <div
          className="sidebar-overlay"
          onClick={() => setIsSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <aside
        className={`sidebar ${isSidebarOpen ? 'sidebar--open' : ''}`}
        aria-label="Main navigation"
      >
        <div className="sidebar-brand">
          <span className="sidebar-brand-icon" aria-hidden="true">✦</span>
          <span className="sidebar-brand-name">Spa Booking</span>
          {/* Mobile close button inside sidebar */}
          <button
            type="button"
            className="sidebar-close-btn lg:hidden"
            onClick={() => setIsSidebarOpen(false)}
            aria-label="Close menu"
          >
            ✕
          </button>
        </div>

        <nav className="sidebar-nav">
          {NAV_ITEMS.map(({ to, label, icon }) => {
            // Very simple permission check: if staff, maybe hide some items in the future.
            // Currently, everyone sees all items as placeholders.
            return (
              <NavLink
                key={to}
                to={to}
                onClick={() => setIsSidebarOpen(false)}
                className={({ isActive }) =>
                  ['sidebar-nav-link', isActive ? 'sidebar-nav-link--active' : ''].join(' ').trim()
                }
              >
                <span className="sidebar-nav-icon" aria-hidden="true">{icon}</span>
                <span>{label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <span className="sidebar-user-avatar" aria-hidden="true">
              {user?.username?.charAt(0).toUpperCase() ?? '?'}
            </span>
            <div className="sidebar-user-info">
              <span className="sidebar-user-name">{user?.username ?? 'User'}</span>
              <span className="sidebar-user-role">
                {user?.roles?.map((r) => r.replace('ROLE_', '')).join(', ') ?? ''}
              </span>
            </div>
          </div>
          <button
            type="button"
            className="sidebar-logout-btn"
            onClick={handleLogout}
            aria-label="Log out"
          >
            ⏻
          </button>
        </div>
      </aside>

      {/* Main content area */}
      <div className="main-area">
        {/* Top header */}
        <header className="main-header">
          <button
            type="button"
            className="mobile-menu-btn"
            onClick={toggleSidebar}
            aria-label="Open menu"
            aria-expanded={isSidebarOpen}
          >
            ☰
          </button>
          <h1 className="main-header-title">{title}</h1>
        </header>

        {/* Page content */}
        <main className="main-content" id="main-content">
          <div className="content-container">{children}</div>
        </main>
      </div>
    </div>
  );
};

export default AppShell;
