import { useState, useEffect, useCallback, useMemo } from 'react';
import AppShell from '../components/AppShell';
import { useAuth } from '../app/auth/useAuth';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getBookings, getBookingById, updateBookingStatus, assignBookingStaff } from '../lib/api/bookings';
import { getStaff } from '../lib/api/staff';
import type { Booking, BookingDetail, BookingStatus } from '../types/booking';
import type { Staff } from '../types/staff';
import { BookingForm } from './BookingForm';
import { formatCurrency, formatTimeRange } from '../lib/format';
import type { BadgeTone } from '../components/ui/Badge';
import {
  Calendar,
  Clock,
  Plus,
  UserRound,
  Search,
  UserCheck,
  Eye,
  X,
  Phone,
  Mail,
  AlertCircle,
  Copy,
  Check
} from 'lucide-react';

const STATUS_CONFIG: Record<BookingStatus, { label: string; tone: BadgeTone }> = {
  PENDING: { label: 'Chờ xác nhận', tone: 'warning' },
  CONFIRMED: { label: 'Đã xác nhận', tone: 'info' },
  COMPLETED: { label: 'Đã hoàn thành', tone: 'success' },
  CANCELLED: { label: 'Đã hủy', tone: 'neutral' },
};

type FilterStatus = 'ALL' | BookingStatus;

const Bookings = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER') ?? false;

  const [bookings, setBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & search
  const [statusFilter, setStatusFilter] = useState<FilterStatus>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingBooking, setEditingBooking] = useState<Booking | undefined>(undefined);

  // Booking Detail Modal
  const [detailBooking, setDetailBooking] = useState<BookingDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);

  // Assignment Modal
  const [assigningBooking, setAssigningBooking] = useState<Booking | null>(null);
  const [activeStaffList, setActiveStaffList] = useState<Staff[]>([]);
  const [selectedStaffId, setSelectedStaffId] = useState<number | null>(null);
  const [assignSubmitting, setAssignSubmitting] = useState(false);
  const [assignError, setAssignError] = useState<string | null>(null);

  // In-flight status action
  const [busy, setBusy] = useState<{ id: number; status: BookingStatus } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Cancel confirmation
  const [cancelTarget, setCancelTarget] = useState<Booking | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const fetchBookings = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getBookings();
      setBookings(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Không thể tải dữ liệu lịch hẹn.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchBookings();
  }, [fetchBookings]);

  // Load staff for assignment if owner
  const loadStaffForAssignment = async () => {
    try {
      const allStaff = await getStaff();
      setActiveStaffList(allStaff.filter((s) => s.isActive));
    } catch {
      setActiveStaffList([]);
    }
  };

  const handleOpenForm = (booking?: Booking) => {
    setEditingBooking(booking);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingBooking(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchBookings();
  };

  const handleOpenDetail = async (id: number) => {
    setDetailLoading(true);
    setDetailBooking(null);
    try {
      const detail = await getBookingById(id);
      setDetailBooking(detail);
    } catch {
      // Fallback
    } finally {
      setDetailLoading(false);
    }
  };

  const handleOpenAssign = async (booking: Booking) => {
    setAssigningBooking(booking);
    setSelectedStaffId(booking.staffId || null);
    setAssignError(null);
    await loadStaffForAssignment();
  };

  const handleConfirmAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assigningBooking || !selectedStaffId) return;

    setAssignSubmitting(true);
    setAssignError(null);
    try {
      await assignBookingStaff(assigningBooking.id, selectedStaffId);
      setAssigningBooking(null);
      await fetchBookings();
      // If detail modal is open, refresh it as well
      if (detailBooking && detailBooking.id === assigningBooking.id) {
        handleOpenDetail(detailBooking.id);
      }
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number; data?: { message?: string } } };
      if (axiosErr.response?.status === 409) {
        setAssignError('Kỹ thuật viên này đã có lịch hẹn trùng giờ trong khoảng thời gian đã chọn.');
      } else if (axiosErr.response?.status === 400) {
        setAssignError(axiosErr.response.data?.message || 'Không thể phân công: kỹ thuật viên không hợp lệ hoặc lịch đã kết thúc.');
      } else {
        setAssignError('Không thể phân công nhân viên. Vui lòng thử lại sau.');
      }
    } finally {
      setAssignSubmitting(false);
    }
  };

  const handleStatusChange = async (id: number, status: BookingStatus): Promise<boolean> => {
    setBusy({ id, status });
    setActionError(null);
    try {
      await updateBookingStatus(id, { status });
      await fetchBookings();
      if (detailBooking && detailBooking.id === id) {
        handleOpenDetail(id);
      }
      return true;
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      const message = errorObj.response?.data?.message || 'Không thể cập nhật trạng thái lịch hẹn.';
      if (status === 'CANCELLED') {
        setCancelError(message);
      } else {
        setActionError(message);
      }
      return false;
    } finally {
      setBusy(null);
    }
  };

  const handleConfirmCancel = async () => {
    if (!cancelTarget) return;
    const ok = await handleStatusChange(cancelTarget.id, 'CANCELLED');
    if (ok) {
      setCancelTarget(null);
      setCancelError(null);
    }
  };

  const handleCopyCode = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2000);
    } catch {
      setCopiedCode(false);
    }
  };

  // Filter & Search logic
  const filteredBookings = useMemo(() => {
    return bookings.filter((b) => {
      const matchesStatus = statusFilter === 'ALL' || b.status === statusFilter;
      const query = searchQuery.trim().toLowerCase();
      if (!query) return matchesStatus;

      const matchesQuery =
        b.customerName?.toLowerCase().includes(query) ||
        b.serviceName?.toLowerCase().includes(query) ||
        b.staffName?.toLowerCase().includes(query) ||
        (b.bookingCode && b.bookingCode.toLowerCase().includes(query));

      return matchesStatus && matchesQuery;
    });
  }, [bookings, statusFilter, searchQuery]);

  if (isFormOpen) {
    return (
      <AppShell title={isOwner ? 'Quản lý lịch hẹn' : 'Lịch của tôi'}>
        <PageHeader title={editingBooking ? 'Chỉnh sửa lịch hẹn' : 'Tạo lịch hẹn mới'} />
        <div className="max-w-2xl mx-auto">
          <BookingForm
            booking={editingBooking}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title={isOwner ? 'Quản lý lịch hẹn' : 'Lịch của tôi'}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader
          title={isOwner ? 'Quản lý Lịch hẹn' : 'Lịch hẹn của tôi'}
          description={
            isOwner
              ? 'Theo dõi, phê duyệt và điều phối lịch hẹn tại TIKEY SPA.'
              : 'Danh sách các cuộc hẹn chăm sóc khách hàng được phân công cho bạn.'
          }
        />
        {isOwner && (
          <Button onClick={() => handleOpenForm()}>
            <Plus aria-hidden="true" size={16} />
            Tạo lịch hẹn
          </Button>
        )}
      </div>

      {actionError && (
        <Alert
          className="mb-6"
          actions={
            <Button variant="secondary" size="sm" onClick={() => setActionError(null)}>
              Đóng
            </Button>
          }
        >
          {actionError}
        </Alert>
      )}

      {/* Filter Tabs & Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 mb-6">
        {/* Status Tabs */}
        <div className="flex items-center gap-1 p-1 bg-stone-100 rounded-xl overflow-x-auto">
          {(['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'] as FilterStatus[]).map((st) => {
            const label = st === 'ALL' ? 'Tất cả' : STATUS_CONFIG[st].label;
            const count = st === 'ALL' ? bookings.length : bookings.filter((b) => b.status === st).length;
            const active = statusFilter === st;

            return (
              <button
                key={st}
                type="button"
                onClick={() => setStatusFilter(st)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 ${
                  active ? 'bg-white text-stone-900 shadow-2xs font-semibold' : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                <span>{label}</span>
                <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${active ? 'bg-stone-100 text-stone-700' : 'text-stone-400'}`}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-64">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-stone-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo tên, mã, dịch vụ..."
            className="w-full h-9 pl-9 pr-3 text-xs rounded-xl bg-white border border-stone-200 text-stone-900 focus:outline-none focus:border-[#465d4c] focus:ring-1 focus:ring-[#465d4c]"
          />
        </div>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-3 animate-pulse" aria-busy="true">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-24 bg-stone-100 rounded-xl" />
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchBookings} />
      )}

      {!isLoading && !error && filteredBookings.length === 0 && (
        <EmptyState
          icon={<Calendar aria-hidden="true" size={24} />}
          title={searchQuery ? 'Không tìm thấy kết quả' : 'Chưa có lịch hẹn'}
          description={
            searchQuery
              ? 'Thử thay đổi từ khóa hoặc bộ lọc trạng thái để tìm kiếm.'
              : isOwner
              ? 'Lịch hẹn mới từ khách hoặc do bạn tạo sẽ được hiển thị tại đây.'
              : 'Bạn chưa có lịch hẹn nào trong danh mục này.'
          }
          action={
            isOwner ? (
              <Button onClick={() => handleOpenForm()}>
                <Plus aria-hidden="true" size={16} />
                Tạo lịch hẹn
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && !error && filteredBookings.length > 0 && (
        <div className="grid grid-cols-1 gap-3.5">
          {filteredBookings.map((booking) => {
            const status = STATUS_CONFIG[booking.status];
            const isBusy = busy?.id === booking.id;
            return (
              <Card key={booking.id} className="border-[#e7e2d8] hover:border-[#c6d8c9] transition-all">
                <CardContent className="flex flex-col gap-4 p-4 sm:p-5 lg:flex-row lg:items-center lg:justify-between">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1">
                      <span className="font-semibold text-stone-900 text-sm sm:text-base">
                        {booking.customerName}
                      </span>
                      <Badge tone={status.tone}>{status.label}</Badge>
                      {booking.bookingCode && (
                        <span className="text-[11px] font-mono text-stone-500 bg-stone-100 px-2 py-0.5 rounded border border-stone-200">
                          {booking.bookingCode}
                        </span>
                      )}
                    </div>

                    <p className="mt-1 text-xs sm:text-sm font-medium text-[#465d4c]">
                      {booking.serviceName}
                      <span className="font-normal text-stone-500">
                        {' '}· {formatCurrency(booking.price)}
                      </span>
                    </p>

                    <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-stone-600">
                      <span className="inline-flex items-center gap-1.5">
                        <Clock aria-hidden="true" size={14} className="text-stone-400" />
                        {formatTimeRange(booking.startTime, booking.endTime)}
                      </span>
                      <span className="inline-flex items-center gap-1.5">
                        <UserRound aria-hidden="true" size={14} className="text-stone-400" />
                        <span>KTV: <strong className="text-stone-800">{booking.staffName}</strong></span>
                      </span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex flex-wrap items-center gap-2 lg:shrink-0 pt-2 lg:pt-0 border-t lg:border-t-0 border-stone-100">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleOpenDetail(booking.id)}
                    >
                      <Eye size={14} className="mr-1" />
                      Chi tiết
                    </Button>

                    {isOwner && (
                      <>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenAssign(booking)}
                          disabled={isBusy || booking.status === 'COMPLETED' || booking.status === 'CANCELLED'}
                          title={booking.status === 'COMPLETED' || booking.status === 'CANCELLED' ? 'Không thể phân công lịch đã kết thúc' : 'Phân công nhân viên'}
                        >
                          <UserCheck size={14} className="mr-1" />
                          Phân công
                        </Button>

                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenForm(booking)}
                          disabled={isBusy}
                        >
                          Sửa
                        </Button>
                      </>
                    )}

                    {booking.status === 'PENDING' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleStatusChange(booking.id, 'CONFIRMED')}
                          disabled={isBusy}
                          aria-busy={busy?.id === booking.id && busy.status === 'CONFIRMED'}
                        >
                          {busy?.id === booking.id && busy.status === 'CONFIRMED' ? 'Đang duyệt...' : 'Xác nhận'}
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => {
                            setCancelError(null);
                            setCancelTarget(booking);
                          }}
                          disabled={isBusy}
                        >
                          Hủy
                        </Button>
                      </>
                    )}

                    {booking.status === 'CONFIRMED' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleStatusChange(booking.id, 'COMPLETED')}
                          disabled={isBusy}
                          aria-busy={busy?.id === booking.id && busy.status === 'COMPLETED'}
                        >
                          {busy?.id === booking.id && busy.status === 'COMPLETED' ? 'Đang lưu...' : 'Hoàn thành'}
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => {
                            setCancelError(null);
                            setCancelTarget(booking);
                          }}
                          disabled={isBusy}
                        >
                          Hủy
                        </Button>
                      </>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Booking Detail Modal */}
      {(detailBooking || detailLoading) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-lg w-full p-6 shadow-xl relative animate-in fade-in zoom-in-95 duration-200">
            <button
              type="button"
              onClick={() => setDetailBooking(null)}
              className="absolute right-4 top-4 text-stone-400 hover:text-stone-700 p-1.5 rounded-lg"
              aria-label="Đóng"
            >
              <X size={18} />
            </button>

            {detailLoading ? (
              <div className="py-12 text-center text-sm text-stone-500">Đang tải chi tiết cuộc hẹn...</div>
            ) : detailBooking && (
              <div className="space-y-5">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-serif-title font-semibold text-lg text-stone-900">
                      Chi tiết lịch hẹn
                    </h3>
                    <Badge tone={STATUS_CONFIG[detailBooking.status].tone}>
                      {STATUS_CONFIG[detailBooking.status].label}
                    </Badge>
                  </div>
                  {detailBooking.bookingCode && (
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-xs font-mono text-stone-500">Mã: {detailBooking.bookingCode}</span>
                      <button
                        type="button"
                        onClick={() => handleCopyCode(detailBooking.bookingCode!)}
                        className="text-stone-400 hover:text-stone-700 p-1"
                        title="Sao chép mã"
                      >
                        {copiedCode ? <Check size={12} className="text-emerald-600" /> : <Copy size={12} />}
                      </button>
                    </div>
                  )}
                </div>

                {/* Customer Information */}
                <div className="bg-stone-50 p-4 rounded-xl border border-stone-100 space-y-2">
                  <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider">Thông tin khách hàng</span>
                  <p className="font-semibold text-stone-900 text-sm">{detailBooking.customerName}</p>
                  {detailBooking.customerPhone && (
                    <p className="text-xs text-stone-600 flex items-center gap-1.5">
                      <Phone size={13} className="text-stone-400" />
                      <a href={`tel:${detailBooking.customerPhone}`} className="hover:underline">{detailBooking.customerPhone}</a>
                    </p>
                  )}
                  {detailBooking.customerEmail && (
                    <p className="text-xs text-stone-600 flex items-center gap-1.5">
                      <Mail size={13} className="text-stone-400" />
                      <span>{detailBooking.customerEmail}</span>
                    </p>
                  )}
                </div>

                {/* Service & Staff */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 bg-stone-50 rounded-xl border border-stone-100">
                    <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider">Dịch vụ</span>
                    <p className="font-semibold text-stone-900 text-xs mt-1">{detailBooking.serviceName}</p>
                    <p className="text-xs text-[#465d4c] font-medium mt-0.5">{formatCurrency(detailBooking.price)}</p>
                  </div>

                  <div className="p-3 bg-stone-50 rounded-xl border border-stone-100">
                    <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider">Kỹ thuật viên</span>
                    <p className="font-semibold text-stone-900 text-xs mt-1">{detailBooking.staffName}</p>
                    {detailBooking.staffPhone && (
                      <p className="text-[11px] text-stone-500 mt-0.5">{detailBooking.staffPhone}</p>
                    )}
                  </div>
                </div>

                {/* Time & Schedule */}
                <div className="p-3 bg-stone-50 rounded-xl border border-stone-100 flex items-center gap-2.5 text-xs text-stone-700">
                  <Clock size={15} className="text-[#465d4c]" />
                  <span>Thời gian: <strong>{formatTimeRange(detailBooking.startTime, detailBooking.endTime)}</strong></span>
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <Button variant="secondary" onClick={() => setDetailBooking(null)}>
                    Đóng
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Staff Assignment Modal (OWNER ONLY) */}
      {assigningBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-md w-full p-6 shadow-xl animate-in fade-in zoom-in-95 duration-200">
            <h3 className="font-serif-title font-semibold text-lg text-stone-900 mb-1">
              Phân công nhân viên phụ trách
            </h3>
            <p className="text-xs text-stone-500 mb-4">
              Lịch hẹn của <strong>{assigningBooking.customerName}</strong> ({assigningBooking.serviceName}, {formatTimeRange(assigningBooking.startTime, assigningBooking.endTime)})
            </p>

            {assignError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs mb-4 flex items-start gap-2">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>{assignError}</span>
              </div>
            )}

            <form onSubmit={handleConfirmAssign} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                  Chọn kỹ thuật viên còn hoạt động
                </label>
                <select
                  value={selectedStaffId || ''}
                  onChange={(e) => setSelectedStaffId(Number(e.target.value))}
                  className="w-full h-11 px-3 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c]"
                  required
                >
                  <option value="" disabled>-- Chọn kỹ thuật viên --</option>
                  {activeStaffList.map((st) => (
                    <option key={st.id} value={st.id}>
                      {st.name} {st.phone ? `(${st.phone})` : ''}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-stone-400 mt-1">
                  Hệ thống sẽ tự động kiểm tra trùng giờ trước khi hoàn tất phân công.
                </p>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-stone-100">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setAssigningBooking(null)}
                  disabled={assignSubmitting}
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  disabled={assignSubmitting || !selectedStaffId}
                >
                  {assignSubmitting ? 'Đang phân công...' : 'Lưu phân công'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel Confirmation Dialog */}
      <ConfirmDialog
        open={cancelTarget !== null}
        title="Hủy lịch hẹn?"
        description={
          cancelTarget && (
            <>
              Lịch hẹn của <strong className="text-stone-900">{cancelTarget.customerName}</strong>{' '}
              ({cancelTarget.serviceName}, {formatTimeRange(cancelTarget.startTime, cancelTarget.endTime)}) sẽ
              được chuyển sang trạng thái <strong className="text-stone-900">Đã hủy</strong>.
              Thao tác này không thể hoàn tác.
            </>
          )
        }
        confirmLabel="Hủy lịch hẹn"
        cancelLabel="Giữ lịch hẹn"
        onConfirm={handleConfirmCancel}
        onCancel={() => {
          setCancelTarget(null);
          setCancelError(null);
        }}
        busy={busy !== null && cancelTarget !== null && busy.id === cancelTarget.id}
        error={cancelError}
      />
    </AppShell>
  );
};

export default Bookings;
