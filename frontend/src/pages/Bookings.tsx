import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { getBookings, updateBookingStatus } from '../lib/api/bookings';
import type { Booking, BookingStatus } from '../types/booking';
import { BookingForm } from './BookingForm';
import { AlertCircle, Calendar } from 'lucide-react';

const formatVND = (amount: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
};

const Bookings = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingBooking, setEditingBooking] = useState<Booking | undefined>(undefined);

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

  const handleStatusChange = async (id: number, status: BookingStatus) => {
    try {
      await updateBookingStatus(id, { status });
      // update local state or fetch again
      fetchBookings();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      alert(errorObj.response?.data?.message || 'Không thể cập nhật trạng thái lịch hẹn.');
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
        <PageHeader title="Quản lý Lịch hẹn" description="Quản lý tất cả lịch hẹn tại spa của bạn." />
        <Button onClick={() => handleOpenForm()}>Tạo lịch hẹn</Button>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 animate-pulse" aria-busy="true">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-24 bg-[var(--color-neutral-100)] rounded-xl">
                <div />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)]" role="alert">
          <AlertCircle className="text-[var(--color-error)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Không thể tải dữ liệu</h3>
          <p className="text-[var(--color-neutral-500)] mb-6 text-center max-w-md">{error}</p>
          <Button onClick={fetchBookings}>Thử lại</Button>
        </div>
      )}

      {!isLoading && !error && bookings.length === 0 && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)] text-center">
          <Calendar className="text-[var(--color-neutral-400)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Chưa có lịch hẹn</h3>
          <p className="text-[var(--color-neutral-500)] mb-6">
            Hiện chưa có lịch hẹn nào để hiển thị.
          </p>
          <Button onClick={() => handleOpenForm()}>Tạo lịch hẹn</Button>
        </div>
      )}

      {!isLoading && !error && bookings.length > 0 && (
        <div className="grid grid-cols-1 gap-4">
          {bookings.map((booking) => (
            <Card key={booking.id}>
              <CardContent className="p-4 sm:p-6 flex flex-col md:flex-row gap-4 md:items-center justify-between">
                <div className="flex-1 space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-[var(--color-neutral-900)]">{booking.customerName}</span>
                    <span className="text-[var(--color-neutral-400)]">•</span>
                    <span className="text-[var(--color-brand-600)] font-medium">{booking.serviceName}</span>
                    <span className="text-[var(--color-neutral-400)]">•</span>
                    <span className="text-[var(--color-neutral-700)] text-sm">{formatVND(booking.price)}</span>
                  </div>
                  
                  <div className="text-sm text-[var(--color-neutral-600)]">
                    <p>
                      <strong>Thời gian:</strong> {formatDate(booking.startTime)} - {formatDate(booking.endTime)}
                    </p>
                    <p>
                      <strong>Nhân viên:</strong> {booking.staffName}
                    </p>
                    <p>
                      <strong>Trạng thái:</strong>{' '}
                      <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-[var(--color-neutral-100)] border border-[var(--color-neutral-200)]">
                        {booking.status === 'PENDING' ? 'Chờ xác nhận' : booking.status === 'CONFIRMED' ? 'Đã xác nhận' : booking.status === 'COMPLETED' ? 'Đã hoàn thành' : booking.status === 'CANCELLED' ? 'Đã hủy' : booking.status}
                      </span>
                    </p>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <Button variant="secondary" size="sm" onClick={() => handleOpenForm(booking)}>
                    Chỉnh sửa
                  </Button>
                  
                  {booking.status === 'PENDING' && (
                    <>
                      <Button size="sm" onClick={() => handleStatusChange(booking.id, 'CONFIRMED')}>
                        Xác nhận
                      </Button>
                      <Button variant="danger" size="sm" onClick={() => handleStatusChange(booking.id, 'CANCELLED')}>
                        Hủy
                      </Button>
                    </>
                  )}
                  
                  {booking.status === 'CONFIRMED' && (
                    <>
                      <Button size="sm" onClick={() => handleStatusChange(booking.id, 'COMPLETED')}>
                        Hoàn thành
                      </Button>
                      <Button variant="danger" size="sm" onClick={() => handleStatusChange(booking.id, 'CANCELLED')}>
                        Hủy
                      </Button>
                    </>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </AppShell>
  );
};

export default Bookings;
