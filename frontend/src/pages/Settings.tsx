import { useNavigate } from 'react-router-dom';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { useAuth } from '../app/auth/useAuth';
import { LogOut, UserRound } from 'lucide-react';

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
        description="Thông tin tài khoản đang đăng nhập và tùy chọn phiên làm việc."
      />

      <div className="max-w-3xl space-y-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-[var(--color-brand-50)] text-[var(--color-brand-600)] rounded-lg" aria-hidden="true">
                <UserRound size={22} />
              </div>
              <h3 className="text-lg font-semibold text-[var(--color-neutral-900)]">Thông tin tài khoản</h3>
            </div>

            <dl className="space-y-4">
              <div>
                <dt className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Tên đăng nhập</dt>
                <dd className="text-base text-[var(--color-neutral-900)] font-medium">
                  {user?.username || 'Không xác định'}
                </dd>
              </div>

              <div>
                <dt className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Vai trò</dt>
                <dd className="flex flex-wrap gap-2 mt-1">
                  {user?.roles?.length ? (
                    user.roles.map((role) => (
                      <Badge key={role} tone="info">
                        {formatRole(role)}
                      </Badge>
                    ))
                  ) : (
                    <span className="text-sm text-[var(--color-neutral-500)]">Chưa có vai trò</span>
                  )}
                </dd>
              </div>
            </dl>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Phiên đăng nhập</h3>
            <p className="text-sm text-[var(--color-neutral-500)] mb-5">
              Đăng xuất sẽ kết thúc phiên làm việc hiện tại trên thiết bị này. Bạn cần đăng nhập lại
              để tiếp tục quản lý lịch hẹn.
            </p>
            <Button variant="secondary" onClick={handleLogout}>
              <LogOut size={16} aria-hidden="true" />
              Đăng xuất
            </Button>
          </CardContent>
        </Card>
      </div>
    </AppShell>
  );
};

export default Settings;
