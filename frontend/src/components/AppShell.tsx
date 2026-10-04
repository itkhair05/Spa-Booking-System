import { useEffect, useRef, useState } from 'react';
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
  Settings as SettingsIcon,
  ExternalLink,
  MessageSquare,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

interface AppShellProps {
  children: ReactNode;
  /** Page title shown in the top header bar */
  title: string;
}

const formatRole = (role: string) => {
  if (role === 'ROLE_OWNER') return 'Chủ cơ sở';
  if (role === 'ROLE_STAFF') return 'Nhân viên';
  return role
    .replace(/^ROLE_/, '')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
};

/**
 * Authenticated application shell for TIKEY SPA.
 * Renders role-aware navigation (OWNER vs STAFF) + top header + scrollable content.
 */
const AppShell = ({ children, title }: AppShellProps) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);

  const isOwner = user?.roles?.includes('ROLE_OWNER') ?? false;

  const navItems: NavItem[] = isOwner
    ? [
        { to: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
        { to: '/bookings', label: 'Quản lý lịch hẹn', icon: Calendar },
        { to: '/customers', label: 'Khách hàng', icon: Users },
        { to: '/services', label: 'Dịch vụ', icon: Scissors },
        { to: '/staff', label: 'Nhân viên', icon: UserRound },
        { to: '/feedback', label: 'Phản hồi khách', icon: MessageSquare },
        { to: '/settings', label: 'Hồ sơ & Cài đặt', icon: SettingsIcon },
      ]
    : [
        { to: '/dashboard', label: 'Lịch làm việc', icon: LayoutDashboard },
        { to: '/bookings', label: 'Lịch của tôi', icon: Calendar },
        { to: '/settings', label: 'Tài khoản', icon: SettingsIcon },
      ];

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const toggleSidebar = () => setIsSidebarOpen((prev) => !prev);

  const closeSidebar = () => setIsSidebarOpen(false);

  // Escape closes the mobile sidebar and returns focus to the menu button.
  useEffect(() => {
    if (!isSidebarOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        closeSidebar();
        menuButtonRef.current?.focus();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isSidebarOpen]);

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">
        Bỏ qua điều hướng
      </a>

      {/* Mobile Sidebar Overlay */}
      {isSidebarOpen && (
        <div
          className="sidebar-overlay"
          onClick={closeSidebar}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <aside
        id="app-sidebar"
        className={`sidebar ${isSidebarOpen ? 'sidebar--open' : ''}`}
        aria-label="Điều hướng chính"
      >
        <div className="sidebar-brand">
          <span className="sidebar-brand-icon" aria-hidden="true">
            <Sparkles size={22} className="text-[#465d4c]" />
          </span>
          <div className="flex flex-col min-w-0 flex-1">
            <span className="font-serif-title font-bold text-lg text-[var(--color-champagne-500)] tracking-tight leading-tight truncate">
              TIKEY SPA
            </span>
            <span className="text-[10px] uppercase font-semibold tracking-wider text-stone-500">
              {isOwner ? 'Quản trị cơ sở' : 'Cổng nhân viên'}
            </span>
          </div>
          {/* Mobile close button inside sidebar */}
          <button
            type="button"
            className="sidebar-close-btn lg:hidden"
            onClick={closeSidebar}
            aria-label="Đóng menu"
          >
            <X size={22} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {navItems.map(({ to, label, icon: Icon }) => {
            return (
              <NavLink
                key={to}
                to={to}
                onClick={closeSidebar}
                className={({ isActive }) =>
                  ['sidebar-nav-link', isActive ? 'sidebar-nav-link--active' : ''].join(' ').trim()
                }
              >
                <span className="sidebar-nav-icon" aria-hidden="true">
                  <Icon size={19} />
                </span>
                <span>{label}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* Public view shortcut for previewing customer booking experience */}
        <div className="px-3 pt-2 pb-1 border-t border-[var(--color-neutral-200)] mt-auto">
          <a
            href="/spas/tikey-spa"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-between px-3 py-2 text-xs font-medium text-stone-500 hover:text-stone-900 hover:bg-stone-100 rounded-xl transition-colors"
          >
            <span className="flex items-center gap-2">
              <ExternalLink size={14} className="text-stone-400" />
              <span>Xem trang đặt lịch</span>
            </span>
            <span className="text-[10px] bg-stone-200 text-stone-700 px-1.5 py-0.5 rounded font-mono">Web</span>
          </a>
        </div>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <span className="sidebar-user-avatar" aria-hidden="true">
              {user?.username?.charAt(0).toUpperCase() ?? '?'}
            </span>
            <div className="sidebar-user-info">
              <span className="sidebar-user-name truncate">{user?.username ?? 'User'}</span>
              <span className="sidebar-user-role">
                {user?.roles?.map(formatRole).join(', ') ?? ''}
              </span>
            </div>
          </div>
          <button
            type="button"
            className="sidebar-logout-btn"
            onClick={handleLogout}
            aria-label="Đăng xuất"
          >
            <LogOut size={19} />
          </button>
        </div>
      </aside>

      {/* Main content area */}
      <div className="main-area">
        {/* Top header */}
        <header className="main-header">
          <button
            ref={menuButtonRef}
            type="button"
            className="mobile-menu-btn"
            onClick={toggleSidebar}
            aria-label="Mở menu"
            aria-expanded={isSidebarOpen}
            aria-controls="app-sidebar"
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
