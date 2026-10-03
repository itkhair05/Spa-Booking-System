import { useState } from 'react';
import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../app/auth/useAuth';
import {
  LayoutDashboard,
  Calendar,
  Users,
  Scissors,
  UserRound,
  LogOut,
  Menu,
  X,
  Sparkles,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/bookings', label: 'Bookings', icon: Calendar },
  { to: '/customers', label: 'Customers', icon: Users },
  { to: '/services', label: 'Services', icon: Scissors },
  { to: '/staff', label: 'Staff', icon: UserRound },
];

interface AppShellProps {
  children: ReactNode;
  /** Page title shown in the top header bar */
  title: string;
}

const formatRole = (role: string) =>
  role
    .replace(/^ROLE_/, '')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());

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
          <span className="sidebar-brand-icon" aria-hidden="true">
            <Sparkles size={24} />
          </span>
          <span className="sidebar-brand-name">Spa Booking</span>
          {/* Mobile close button inside sidebar */}
          <button
            type="button"
            className="sidebar-close-btn lg:hidden"
            onClick={() => setIsSidebarOpen(false)}
            aria-label="Close menu"
          >
            <X size={24} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => {
            return (
              <NavLink
                key={to}
                to={to}
                onClick={() => setIsSidebarOpen(false)}
                className={({ isActive }) =>
                  ['sidebar-nav-link', isActive ? 'sidebar-nav-link--active' : ''].join(' ').trim()
                }
              >
                <span className="sidebar-nav-icon" aria-hidden="true">
                  <Icon size={20} />
                </span>
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
                {user?.roles?.map(formatRole).join(', ') ?? ''}
              </span>
            </div>
          </div>
          <button
            type="button"
            className="sidebar-logout-btn"
            onClick={handleLogout}
            aria-label="Log out"
          >
            <LogOut size={20} />
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
            <Menu size={24} />
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
