import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import AppShell from '../components/AppShell';
import { useAuth } from '../app/auth/useAuth';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getDashboardMetrics } from '../lib/api/dashboard';
import { getBookings } from '../lib/api/bookings';
import type { DashboardMetrics } from '../types/dashboard';
import type { Booking, BookingStatus } from '../types/booking';
import { formatCurrency, formatTimeRange } from '../lib/format';
import {
  CalendarDays,
  CalendarRange,
  Clock,
  Sparkles,
  Wallet,
  Plus,
  ArrowRight,
  UserRound,
  Scissors,
  Calendar,
  TrendingUp
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface StatTileProps {
  icon: LucideIcon;
  label: string;
  value: string;
  valueClassName?: string;
  subtext?: string;
}

const STATUS_BADGES: Record<BookingStatus, { label: string; tone: 'warning' | 'info' | 'success' | 'neutral' }> = {
  PENDING: { label: 'Chờ xác nhận', tone: 'warning' },
  CONFIRMED: { label: 'Đã xác nhận', tone: 'info' },
  COMPLETED: { label: 'Đã hoàn thành', tone: 'success' },
  CANCELLED: { label: 'Đã hủy', tone: 'neutral' },
};

const StatTile = ({ icon: Icon, label, value, valueClassName = 'text-stone-900', subtext }: StatTileProps) => (
  <div className="flex flex-col gap-2 bg-white p-5 sm:p-6 transition-colors">
    <div className="flex items-center gap-2 text-stone-500">
      <div className="p-1.5 rounded-lg bg-stone-100 text-stone-600">
        <Icon aria-hidden="true" size={16} />
      </div>
      <span className="text-xs font-semibold uppercase tracking-wider">{label}</span>
    </div>
    <p className={`text-3xl font-bold font-mono tabular-nums ${valueClassName}`}>{value}</p>
    {subtext && <p className="text-xs text-stone-500">{subtext}</p>}
  </div>
);

const Dashboard = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER') ?? false;

  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [recentBookings, setRecentBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchDashboardData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [metricsData, bookingsData] = await Promise.all([
        getDashboardMetrics(),
        getBookings().catch(() => [] as Booking[]),
      ]);
      setMetrics(metricsData);
      // Sort upcoming / recent bookings
      const sorted = [...bookingsData].sort(
        (a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
      );
      setRecentBookings(sorted.slice(0, 5));
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Không thể tải dữ liệu tổng quan.'));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchDashboardData();
  }, [fetchDashboardData]);

  const greeting = isOwner
    ? `Chào mừng trở lại, ${user?.username ?? 'Chủ cơ sở'}.`
    : `Chào bạn, ${user?.username ?? 'Nhân viên'}.`;

  if (isLoading) {
    return (
      <AppShell title={isOwner ? 'Tổng quan cơ sở' : 'Lịch làm việc'}>
        <PageHeader title={isOwner ? 'Tổng quan cơ sở' : 'Lịch làm việc'} description={`${greeting} Đang tải dữ liệu...`} />
        <Card className="overflow-hidden mb-6" aria-busy="true">
          <CardContent className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-px bg-stone-200 p-0">
            {Array.from({ length: isOwner ? 4 : 3 }, (_, i) => (
              <div key={i} className="flex animate-pulse flex-col gap-3 bg-white p-6">
                <div className="h-4 w-28 rounded bg-stone-100" />
                <div className="h-8 w-16 rounded bg-stone-100" />
              </div>
            ))}
          </CardContent>
        </Card>
      </AppShell>
    );
  }

  if (error) {
    return (
      <AppShell title={isOwner ? 'Tổng quan cơ sở' : 'Lịch làm việc'}>
        <PageHeader title={isOwner ? 'Tổng quan cơ sở' : 'Lịch làm việc'} />
        <ErrorState
          message="Không thể kết nối đến máy chủ. Vui lòng kiểm tra và thử lại."
          onRetry={fetchDashboardData}
        />
      </AppShell>
    );
  }

  if (!metrics) return null;

  const totalActionable = (metrics.todayBookingCount || 0) + (metrics.pendingBookingCount || 0);

  return (
    <AppShell title={isOwner ? 'Tổng quan cơ sở' : 'Lịch làm việc'}>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <PageHeader
          title={isOwner ? 'Tổng quan TIKEY SPA' : 'Lịch làm việc cá nhân'}
          description={
            isOwner
              ? `${greeting} Dưới đây là tình hình hoạt động của toàn bộ cơ sở.`
              : `${greeting} Dưới đây là danh sách lịch hẹn được phân công cho bạn.`
          }
        />
        {isOwner && (
          <Link
            to="/bookings"
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#465d4c] text-white text-sm font-medium rounded-xl hover:bg-[#374a3c] transition-colors shadow-xs shrink-0"
          >
            <Plus size={16} />
            <span>Tạo lịch hẹn</span>
          </Link>
        )}
      </div>

      {/* Pending alert banner */}
      {metrics.pendingBookingCount > 0 && (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-amber-200 bg-amber-50/90 px-5 py-3.5 shadow-xs">
          <div className="flex items-center gap-3">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500 animate-ping" />
            <p className="text-sm font-medium text-amber-900">
              Có <span className="font-bold tabular-nums text-amber-950">{metrics.pendingBookingCount}</span> lịch hẹn đang ở trạng thái <strong className="underline">Chờ xác nhận</strong>.
            </p>
          </div>
          <Link
            to="/bookings"
            className="inline-flex items-center gap-1 text-xs font-semibold text-amber-900 hover:text-amber-950 underline underline-offset-4"
          >
            <span>Xử lý ngay</span>
            <ArrowRight size={14} />
          </Link>
        </div>
      )}

      {/* KPI Tiles */}
      <Card className="overflow-hidden mb-8 shadow-xs border-[#e7e2d8]">
        <CardContent
          className={`grid grid-cols-1 sm:grid-cols-2 gap-px bg-stone-200 p-0 ${
            isOwner ? 'lg:grid-cols-5' : 'lg:grid-cols-3'
          }`}
        >
          <StatTile
            icon={CalendarDays}
            label={isOwner ? 'Lịch hẹn hôm nay' : 'Lịch của bạn hôm nay'}
            value={String(metrics.todayBookingCount)}
            subtext="Cuộc hẹn trong ngày"
          />
          <StatTile
            icon={Clock}
            label="Chờ xác nhận"
            value={String(metrics.pendingBookingCount)}
            valueClassName={metrics.pendingBookingCount > 0 ? 'text-amber-700' : 'text-stone-900'}
            subtext={isOwner ? 'Cần xử lý phê duyệt' : 'Lịch được giao chờ duyệt'}
          />
          <StatTile
            icon={CalendarRange}
            label={isOwner ? 'Lịch sắp tới' : 'Cuộc hẹn sắp tới'}
            value={String(metrics.upcomingBookingCount)}
            subtext="Các ngày tiếp theo"
          />
          {isOwner && (
            <>
              <StatTile
                icon={Wallet}
                label="Doanh thu thực tế"
                value={formatCurrency(metrics.todayCompletedRevenue ?? 0)}
                valueClassName="text-[#465d4c]"
                subtext="Đã hoàn thành hôm nay"
              />
              <StatTile
                icon={TrendingUp}
                label="Doanh thu dự kiến"
                value={formatCurrency(metrics.todayExpectedRevenue ?? 0)}
                valueClassName="text-amber-700"
                subtext="Đã xác nhận hôm nay"
              />
            </>
          )}
        </CardContent>
      </Card>

      {/* Main Operational Section: Today's Schedule & Quick Insights */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Schedule Timeline */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-serif-title font-semibold text-stone-900">
              {isOwner ? 'Lịch hẹn gần đây & hôm nay' : 'Cuộc hẹn gần nhất của bạn'}
            </h3>
            <Link
              to="/bookings"
              className="text-xs font-medium text-[#465d4c] hover:underline flex items-center gap-1"
            >
              <span>Xem tất cả</span>
              <ArrowRight size={13} />
            </Link>
          </div>

          {recentBookings.length === 0 ? (
            <Card className="border-[#e7e2d8]">
              <CardContent className="py-12">
                <EmptyState
                  icon={<Calendar aria-hidden="true" size={24} />}
                  title={isOwner ? 'Chưa có lịch hẹn nào' : 'Bạn chưa có lịch hẹn nào được phân công'}
                  description={
                    isOwner
                      ? 'Lịch hẹn đặt từ khách hàng hoặc do bạn tạo sẽ xuất hiện tại đây.'
                      : 'Khi chủ cơ sở phân công cuộc hẹn cho bạn, lịch sẽ hiển thị tại đây.'
                  }
                  action={
                    isOwner ? (
                      <Link
                        to="/bookings"
                        className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-[#465d4c] px-4 py-2 text-sm font-medium text-white hover:bg-[#374a3c] transition-colors"
                      >
                        <Plus size={16} />
                        Tạo lịch hẹn mới
                      </Link>
                    ) : undefined
                  }
                />
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-3">
              {recentBookings.map((b) => {
                const badge = STATUS_BADGES[b.status] || { label: b.status, tone: 'neutral' };
                return (
                  <div
                    key={b.id}
                    className="p-4 rounded-2xl bg-white border border-[#e7e2d8] hover:border-[#c6d8c9] shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3 transition-colors"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2.5 flex-wrap">
                        <span className="font-semibold text-stone-900 text-sm">{b.customerName}</span>
                        <Badge tone={badge.tone}>{badge.label}</Badge>
                        {b.bookingCode && (
                          <span className="text-[11px] font-mono text-stone-400">
                            {b.bookingCode}
                          </span>
                        )}
                      </div>
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-stone-600 mt-1.5">
                        <span className="flex items-center gap-1 text-[#465d4c] font-medium">
                          <Scissors size={13} />
                          {b.serviceName} ({formatCurrency(b.price)})
                        </span>
                        <span className="flex items-center gap-1 text-stone-500">
                          <Clock size={13} />
                          {formatTimeRange(b.startTime, b.endTime)}
                        </span>
                        {isOwner && (
                          <span className="flex items-center gap-1 text-stone-500">
                            <UserRound size={13} />
                            {b.staffName}
                          </span>
                        )}
                      </div>
                    </div>

                    <Link
                      to="/bookings"
                      className="inline-flex items-center gap-1 text-xs font-medium text-stone-500 hover:text-stone-900 px-3 py-1.5 rounded-lg bg-stone-50 hover:bg-stone-100 self-start sm:self-center shrink-0 transition-colors"
                    >
                      <span>Chi tiết</span>
                      <ArrowRight size={13} />
                    </Link>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Col: Quick Actions & Status Summary */}
        <div className="space-y-6">
          {/* Status Breakdown card */}
          <Card className="border-[#e7e2d8]">
            <CardContent className="p-5">
              <h4 className="text-sm font-semibold text-stone-900 mb-3">
                {isOwner ? 'Phân bổ trạng thái hôm nay' : 'Trạng thái công việc'}
              </h4>
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="flex items-center gap-1.5 text-stone-600">
                    <span className="w-2 h-2 rounded-full bg-amber-500" />
                    Chờ xác nhận
                  </span>
                  <span className="font-mono font-semibold text-stone-900">{metrics.pendingBookingCount}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="flex items-center gap-1.5 text-stone-600">
                    <span className="w-2 h-2 rounded-full bg-[#465d4c]" />
                    Đã xác nhận
                  </span>
                  <span className="font-mono font-semibold text-stone-900">{metrics.confirmedBookingCount}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="flex items-center gap-1.5 text-stone-600">
                    <span className="w-2 h-2 rounded-full bg-stone-400" />
                    Lịch hẹn hôm nay
                  </span>
                  <span className="font-mono font-semibold text-stone-900">{metrics.todayBookingCount}</span>
                </div>
              </div>

              {/* Progress bar visual */}
              <div className="w-full h-2 rounded-full bg-stone-100 mt-4 overflow-hidden flex">
                {metrics.pendingBookingCount > 0 && (
                  <div
                    style={{ width: `${Math.min(100, (metrics.pendingBookingCount / (totalActionable || 1)) * 100)}%` }}
                    className="bg-amber-500 h-full"
                    title={`Chờ xác nhận: ${metrics.pendingBookingCount}`}
                  />
                )}
                {metrics.confirmedBookingCount > 0 && (
                  <div
                    style={{ width: `${Math.min(100, (metrics.confirmedBookingCount / (totalActionable || 1)) * 100)}%` }}
                    className="bg-[#465d4c] h-full"
                    title={`Đã xác nhận: ${metrics.confirmedBookingCount}`}
                  />
                )}
              </div>
            </CardContent>
          </Card>

          {/* Quick links card */}
          <Card className="border-[#e7e2d8]">
            <CardContent className="p-5 space-y-3">
              <h4 className="text-sm font-semibold text-stone-900 mb-2">Thao tác nhanh</h4>
              <div className="flex flex-col gap-2">
                <Link
                  to="/bookings"
                  className="flex items-center justify-between p-3 rounded-xl bg-stone-50 hover:bg-stone-100 text-stone-800 text-xs font-medium transition-colors"
                >
                  <span className="flex items-center gap-2">
                    <Calendar size={15} className="text-[#465d4c]" />
                    <span>{isOwner ? 'Quản lý toàn bộ lịch hẹn' : 'Xem lịch của tôi'}</span>
                  </span>
                  <ArrowRight size={13} className="text-stone-400" />
                </Link>

                {isOwner && (
                  <>
                    <Link
                      to="/services"
                      className="flex items-center justify-between p-3 rounded-xl bg-stone-50 hover:bg-stone-100 text-stone-800 text-xs font-medium transition-colors"
                    >
                      <span className="flex items-center gap-2">
                        <Scissors size={15} className="text-[#465d4c]" />
                        <span>Danh mục dịch vụ</span>
                      </span>
                      <ArrowRight size={13} className="text-stone-400" />
                    </Link>

                    <Link
                      to="/staff"
                      className="flex items-center justify-between p-3 rounded-xl bg-stone-50 hover:bg-stone-100 text-stone-800 text-xs font-medium transition-colors"
                    >
                      <span className="flex items-center gap-2">
                        <UserRound size={15} className="text-[#465d4c]" />
                        <span>Đội ngũ nhân viên</span>
                      </span>
                      <ArrowRight size={13} className="text-stone-400" />
                    </Link>
                  </>
                )}

                <a
                  href="/spas/demo-spa"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-between p-3 rounded-xl bg-[#f2f6f3] text-[#374a3c] hover:bg-[#e2ece4] text-xs font-medium transition-colors"
                >
                  <span className="flex items-center gap-2">
                    <Sparkles size={15} className="text-[#566f5c]" />
                    <span>Xem website đặt lịch (Khách hàng)</span>
                  </span>
                  <span className="text-[10px] bg-white text-stone-700 px-1.5 py-0.5 rounded font-mono shadow-2xs">Mở</span>
                </a>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </AppShell>
  );
};

export default Dashboard;
