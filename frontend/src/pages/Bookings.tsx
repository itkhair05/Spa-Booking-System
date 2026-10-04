import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getBookings, updateBookingStatus } from '../lib/api/bookings';
import type { Booking, BookingStatus } from '../types/booking';
import { BookingForm } from './BookingForm';
import { formatCurrency, formatTimeRange } from '../lib/format';
import type { BadgeTone } from '../components/ui/Badge';
import { Calendar, Clock, Plus, UserRound } from 'lucide-react';

const STATUS_CONFIG: Record<BookingStatus, { label: string; tone: BadgeTone }> = {
  PENDING: { label: 'Chờ xác nhận', tone: 'warning' },
  CONFIRMED: { label: 'Đã xác nhận', tone: 'info' },
  COMPLETED: { label: 'Đã hoàn thành', tone: 'success' },
  CANCELLED: { label: 'Đã hủy', tone: 'neutral' },
};

const Bookings = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingBooking, setEditingBooking] = useState<Booking | undefined>(undefined);

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

  const handleStatusChange = async (id: number, status: BookingStatus): Promise<boolean> => {
    setBusy({ id, status });
    setActionError(null);
    try {
      await updateBookingStatus(id, { status });
      await fetchBookings();
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

  if (isFormOpen) {
    return (
      <AppShell title="Lịch hẹn">
        <PageHeader title="Quản lý Lịch hẹn" />
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
    <AppShell title="Lịch hẹn">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader title="Quản lý Lịch hẹn" description="Theo dõi và xử lý toàn bộ lịch hẹn tại spa." />
        <Button onClick={() => handleOpenForm()}>
          <Plus aria-hidden="true" size={16} />
          Tạo lịch hẹn
        </Button>
      </div>

      {actionError && (
        <Alert className="mb-6" actions={
          <Button variant="secondary" size="sm" onClick={() => setActionError(null)}>
            Đóng
          </Button>
        }>
          {actionError}
        </Alert>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 animate-pulse" aria-busy="true">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-28 bg-[var(--color-neutral-100)] rounded-xl">
                <div />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchBookings} />
      )}

      {!isLoading && !error && bookings.length === 0 && (
        <EmptyState
          icon={<Calendar aria-hidden="true" size={24} />}
          title="Chưa có lịch hẹn"
          description="Lịch hẹn mới từ khách hoặc do bạn tạo sẽ được hiển thị tại đây."
          action={
            <Button onClick={() => handleOpenForm()}>
              <Plus aria-hidden="true" size={16} />
              Tạo lịch hẹn
            </Button>
          }
        />
      )}

      {!isLoading && !error && bookings.length > 0 && (
        <div className="grid grid-cols-1 gap-4">
          {bookings.map((booking) => {
            const status = STATUS_CONFIG[booking.status];
            const isBusy = busy?.id === booking.id;
            return (
              <Card key={booking.id}>
                <CardContent className="flex flex-col gap-4 p-4 sm:p-5 lg:flex-row lg:items-center lg:justify-between">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
                      <span className="font-semibold text-[var(--color-neutral-900)]">
                        {booking.customerName}
                      </span>
                      <Badge tone={status.tone}>{status.label}</Badge>
                    </div>

                    <p className="mt-1 text-sm font-medium text-[var(--color-brand-700)]">
                      {booking.serviceName}
                      <span className="font-normal text-[var(--color-neutral-500)]">
                        {' '}
                        · {formatCurrency(booking.price)}
                      </span>
                    </p>

                    <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm text-[var(--color-neutral-600)]">
                      <span className="inline-flex items-center gap-1.5">
                        <Clock aria-hidden="true" size={15} className="text-[var(--color-neutral-400)]" />
                        {formatTimeRange(booking.startTime, booking.endTime)}
                      </span>
                      <span className="inline-flex items-center gap-1.5">
                        <UserRound aria-hidden="true" size={15} className="text-[var(--color-neutral-400)]" />
                        {booking.staffName}
                      </span>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 lg:shrink-0">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleOpenForm(booking)}
                      disabled={isBusy}
                    >
                      Chỉnh sửa
                    </Button>

                    {booking.status === 'PENDING' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleStatusChange(booking.id, 'CONFIRMED')}
                          disabled={isBusy}
                          aria-busy={busy?.id === booking.id && busy.status === 'CONFIRMED'}
                        >
                          {busy?.id === booking.id && busy.status === 'CONFIRMED' ? 'Đang xử lý...' : 'Xác nhận'}
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
                          {busy?.id === booking.id && busy.status === 'COMPLETED' ? 'Đang xử lý...' : 'Hoàn thành'}
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

      <ConfirmDialog
        open={cancelTarget !== null}
        title="Hủy lịch hẹn?"
        description={
          cancelTarget && (
            <>
              Lịch hẹn của <strong className="text-[var(--color-neutral-800)]">{cancelTarget.customerName}</strong>{' '}
              ({cancelTarget.serviceName}, {formatTimeRange(cancelTarget.startTime, cancelTarget.endTime)}) sẽ
              được chuyển sang trạng thái <strong className="text-[var(--color-neutral-800)]">Đã hủy</strong>.
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
