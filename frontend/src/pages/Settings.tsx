import { useNavigate } from 'react-router-dom';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { useAuth } from '../app/auth/useAuth';
import { LogOut, User, ShieldCheck } from 'lucide-react';

const formatRole = (role: string) => {
  if (role === 'ROLE_OWNER') return 'Chủ cơ sở';
  if (role === 'ROLE_STAFF') return 'Nhân viên';
  return role
    .replace(/^ROLE_/, '')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
};

const Settings = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <AppShell title="Cài đặt">
      <PageHeader 
        title="Cài đặt & Tài khoản" 
        description="Quản lý tùy chọn tài khoản và bảo mật của bạn." 
      />

      <div className="max-w-3xl space-y-6">
        {/* Account Information Card */}
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-[var(--color-brand-50)] text-[var(--color-brand-600)] rounded-lg">
                <User size={24} />
              </div>
              <h3 className="text-lg font-semibold text-[var(--color-neutral-900)]">Thông tin tài khoản</h3>
            </div>
            
            <div className="space-y-4">
              <div>
                <p className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Tên đăng nhập</p>
                <p className="text-base text-[var(--color-neutral-900)] font-medium">
                  {user?.username || 'Unknown User'}
                </p>
              </div>
              
              <div>
                <p className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Vai trò</p>
                <div className="flex flex-wrap gap-2 mt-1">
                  {user?.roles?.map((role) => (
                    <span 
                      key={role} 
                      className="px-2.5 py-1 rounded-full text-xs font-semibold bg-[var(--color-brand-50)] text-[var(--color-brand-700)] border border-[var(--color-brand-200)]"
                    >
                      {formatRole(role)}
                    </span>
                  )) || <span className="text-sm text-[var(--color-neutral-500)]">Chưa có vai trò</span>}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Security & Session Card */}
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-slate-50 text-slate-600 rounded-lg">
                <ShieldCheck size={24} />
              </div>
              <h3 className="text-lg font-semibold text-[var(--color-neutral-900)]">Bảo mật & Phiên đăng nhập</h3>
            </div>
            
            <div className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-4 rounded-lg bg-[var(--color-neutral-50)] border border-[var(--color-neutral-200)]">
                  <p className="text-xs font-semibold text-[var(--color-neutral-500)] uppercase tracking-wider mb-1">Xác thực</p>
                  <p className="text-sm text-[var(--color-neutral-900)] font-medium">JWT (JSON Web Token)</p>
                </div>
                <div className="p-4 rounded-lg bg-[var(--color-neutral-50)] border border-[var(--color-neutral-200)]">
                  <p className="text-xs font-semibold text-[var(--color-neutral-500)] uppercase tracking-wider mb-1">Mô hình phiên</p>
                  <p className="text-sm text-[var(--color-neutral-900)] font-medium">Stateless</p>
                </div>
              </div>

              <div className="pt-4 border-t border-[var(--color-neutral-200)]">
                <h4 className="text-sm font-semibold text-[var(--color-neutral-900)] mb-2">Kết thúc phiên</h4>
                <p className="text-sm text-[var(--color-neutral-500)] mb-4">
                  Đăng xuất sẽ kết thúc phiên hiện tại một cách an toàn và xóa dữ liệu xác thực của bạn khỏi thiết bị này.
                </p>
                <Button 
                  variant="danger" 
                  onClick={handleLogout}
                  className="flex items-center gap-2"
                >
                  <LogOut size={18} />
                  <span>Đăng xuất an toàn</span>
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </AppShell>
  );
};

export default Settings;
