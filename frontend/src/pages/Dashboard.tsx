import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import AppShell from '../components/AppShell';
import { useAuth } from '../app/auth/useAuth';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getDashboardMetrics } from '../lib/api/dashboard';
import type { DashboardMetrics } from '../types/dashboard';
import { formatCurrency } from '../lib/format';
import { CalendarDays, CalendarRange, Clock, Sparkles, Wallet } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface StatTileProps {
  icon: LucideIcon;
  label: string;
  value: string;
  valueClassName?: string;
}

const StatTile = ({ icon: Icon, label, value, valueClassName = 'text-[var(--color-neutral-900)]' }: StatTileProps) => (
  <div className="flex flex-col gap-2 bg-white p-5 sm:p-6">
    <div className="flex items-center gap-2 text-[var(--color-neutral-500)]">
      <Icon aria-hidden="true" size={18} />
      <span className="text-sm font-medium">{label}</span>
    </div>
    <p className={`text-3xl font-bold tabular-nums ${valueClassName}`}>{value}</p>
  </div>
);

const Dashboard = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER') ?? false;

  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchMetrics = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getDashboardMetrics();
      setMetrics(data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load dashboard metrics.'));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMetrics();
  }, [fetchMetrics]);

  const greeting = `Chào mừng trở lại, ${user?.username ?? 'bạn'}.`;

  // Loading State
  if (isLoading) {
    return (
      <AppShell title="Tổng quan">
        <PageHeader title="Tổng quan" description={`${greeting} Đang tải tổng quan...`} />
        <Card className="overflow-hidden" aria-busy="true">
          <CardContent
            className={`grid grid-cols-1 gap-px bg-[var(--color-neutral-200)] p-0 sm:grid-cols-2 ${
              isOwner ? 'lg:grid-cols-4' : 'lg:grid-cols-3'
            }`}
          >
            {Array.from({ length: isOwner ? 4 : 3 }, (_, i) => (
              <div key={i} className="flex animate-pulse flex-col gap-3 bg-white p-6">
                <div className="h-4 w-28 rounded bg-[var(--color-neutral-100)]" />
                <div className="h-8 w-16 rounded bg-[var(--color-neutral-100)]" />
              </div>
            ))}
          </CardContent>
        </Card>
      </AppShell>
    );
  }

  // Error State
  if (error) {
    return (
      <AppShell title="Tổng quan">
        <PageHeader title="Tổng quan" />
        <ErrorState
          message="Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại."
          onRetry={fetchMetrics}
        />
      </AppShell>
    );
  }

  if (!metrics) return null;

  const isEmpty =
    metrics.todayBookingCount === 0 &&
    metrics.upcomingBookingCount === 0 &&
    metrics.pendingBookingCount === 0 &&
    metrics.confirmedBookingCount === 0 &&
    metrics.todayExpectedRevenue === 0;

  return (
    <AppShell title="Tổng quan">
      <PageHeader
        title="Tổng quan"
        description={`${greeting} Dưới đây là hoạt động tại spa hôm nay.`}
      />

      {metrics.pendingBookingCount > 0 && (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[var(--color-warning-border)] bg-[var(--color-warning-bg)] px-4 py-3">
          <p className="text-sm font-medium text-[var(--color-neutral-800)]">
            Có <span className="font-bold tabular-nums">{metrics.pendingBookingCount}</span> lịch hẹn đang chờ xác nhận.
          </p>
          <Link
            to="/bookings"
            className="text-sm font-semibold text-[var(--color-brand-700)] underline-offset-2 hover:underline"
          >
            Xem lịch hẹn
          </Link>
        </div>
      )}

      {isEmpty ? (
        <EmptyState
          icon={<Sparkles aria-hidden="true" size={24} />}
          title="Chưa có hoạt động hôm nay"
          description="Khi khách đặt lịch hoặc bạn tạo lịch hẹn mới, số liệu sẽ xuất hiện tại đây."
          action={
            <Link
              to="/bookings"
              className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-[var(--color-brand-600)] px-4 py-2 text-sm font-medium whitespace-nowrap text-white transition-colors hover:bg-[var(--color-brand-700)]"
            >
              Tạo lịch hẹn
            </Link>
          }
        />
      ) : (
        <Card className="overflow-hidden">
          <CardContent
            className={`grid grid-cols-1 gap-px bg-[var(--color-neutral-200)] p-0 sm:grid-cols-2 ${
              isOwner ? 'lg:grid-cols-4' : 'lg:grid-cols-3'
            }`}
          >
            <StatTile
              icon={CalendarDays}
              label="Lịch hẹn hôm nay"
              value={String(metrics.todayBookingCount)}
            />
            <StatTile
              icon={Clock}
              label="Chờ xác nhận"
              value={String(metrics.pendingBookingCount)}
              valueClassName={metrics.pendingBookingCount > 0 ? 'text-[var(--color-warning)]' : undefined}
            />
            <StatTile
              icon={CalendarRange}
              label="Lịch sắp tới"
              value={String(metrics.upcomingBookingCount)}
            />
            {isOwner && (
              <StatTile
                icon={Wallet}
                label="Doanh thu dự kiến hôm nay"
                value={formatCurrency(metrics.todayExpectedRevenue)}
                valueClassName="text-[var(--color-brand-600)]"
              />
            )}
          </CardContent>
        </Card>
      )}
    </AppShell>
  );
};

export default Dashboard;
