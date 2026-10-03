import { useEffect, useState, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { useAuth } from '../app/auth/useAuth';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { getDashboardMetrics } from '../lib/api/dashboard';
import type { DashboardMetrics } from '../types/dashboard';
import { AlertCircle, Sparkles } from 'lucide-react';

const formatVND = (amount: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

const Dashboard = () => {
  const { user } = useAuth();
  
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

  // Loading State
  if (isLoading) {
    return (
      <AppShell title="Tổng quan">
        <PageHeader 
          title="Tổng quan" 
          description={`Chào mừng trở lại, ${user?.username ?? 'User'}. Đang tải tổng quan...`}
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 animate-pulse" aria-busy="true">
          {[1, 2, 3, 4].map((i) => (
            <Card key={i}>
              <CardContent className="h-[120px] bg-[var(--color-neutral-100)] rounded-xl">
                <div />
              </CardContent>
            </Card>
          ))}
        </div>
      </AppShell>
    );
  }

  // Error State
  if (error) {
    return (
      <AppShell title="Tổng quan">
        <PageHeader title="Tổng quan" />
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)]" role="alert">
          <AlertCircle className="text-[var(--color-error)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Không thể tải dữ liệu</h3>
          <p className="text-[var(--color-neutral-500)] mb-6 text-center max-w-md">
            Không thể kết nối đến máy chủ. Vui lòng thử lại sau.
          </p>
          <Button onClick={fetchMetrics}>Thử lại</Button>
        </div>
      </AppShell>
    );
  }

  // Ensure metrics exist (should always be true here unless fetch failed, but TS needs it)
  if (!metrics) return null;

  // Determine if empty (all zeros)
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
        description={`Chào mừng trở lại, ${user?.username ?? 'User'}. Dưới đây là hoạt động tại spa của bạn hôm nay.`}
      />
      
      {isEmpty ? (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)] text-center">
          <Sparkles className="text-[var(--color-neutral-400)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Chưa có dữ liệu</h3>
          <p className="text-[var(--color-neutral-500)]">
            Hiện chưa có lịch hẹn hoặc doanh thu nào để hiển thị.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card>
            <CardContent className="p-6">
              <p className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Lịch hẹn hôm nay</p>
              <p className="text-3xl font-bold text-[var(--color-neutral-900)]">{metrics.todayBookingCount}</p>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <p className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Chờ xác nhận</p>
              <p className="text-3xl font-bold text-[var(--color-neutral-900)]">{metrics.pendingBookingCount}</p>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <p className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Lịch sắp tới</p>
              <p className="text-3xl font-bold text-[var(--color-neutral-900)]">{metrics.upcomingBookingCount}</p>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <p className="text-sm font-medium text-[var(--color-neutral-500)] mb-1">Doanh thu dự kiến hôm nay</p>
              <p className="text-3xl font-bold text-[var(--color-brand-600)]">{formatVND(metrics.todayExpectedRevenue)}</p>
            </CardContent>
          </Card>
        </div>
      )}
    </AppShell>
  );
};

export default Dashboard;
