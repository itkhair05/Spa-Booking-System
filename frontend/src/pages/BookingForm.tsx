import { useState, useEffect } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { getCustomers } from '../lib/api/customers';
import { getServices } from '../lib/api/services';
import { getStaff } from '../lib/api/staff';
import { createBooking, updateBooking } from '../lib/api/bookings';
import type { Customer } from '../types/customer';
import type { Service } from '../types/service';
import type { Staff } from '../types/staff';
import type { Booking, CreateBookingRequest, UpdateBookingRequest } from '../types/booking';

interface BookingFormProps {
  booking?: Booking; // if undefined, it's create mode
  onSuccess: () => void;
  onCancel: () => void;
}

// The backend stores wall-clock LocalDateTime. Sending a UTC ISO string would
// shift the displayed time by the browser's offset.
const toLocalDateTime = (value: string) => (value.length === 16 ? `${value}:00` : value);

export const BookingForm = ({ booking, onSuccess, onCancel }: BookingFormProps) => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [services, setServices] = useState<Service[]>([]);
  const [staffList, setStaffList] = useState<Staff[]>([]);
  
  const [isLoadingData, setIsLoadingData] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [customerId, setCustomerId] = useState<string>(booking ? String(booking.customerId) : '');
  const [serviceId, setServiceId] = useState<string>(booking ? String(booking.serviceId) : '');
  const [staffId, setStaffId] = useState<string>(booking ? String(booking.staffId) : '');
  const [startTime, setStartTime] = useState<string>(booking ? booking.startTime.slice(0, 16) : '');
  const [endTime, setEndTime] = useState<string>(booking ? booking.endTime.slice(0, 16) : '');

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [custData, servData, stData] = await Promise.all([
          getCustomers(),
          getServices(),
          getStaff()
        ]);
        setCustomers(custData);
        setServices(servData);
        setStaffList(stData);
      } catch {
        setError('Không thể tải dữ liệu liên quan để tạo lịch hẹn.');
      } finally {
        setIsLoadingData(false);
      }
    };
    fetchData();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerId || !serviceId || !staffId || !startTime || !endTime) {
      setError('Vui lòng nhập đầy đủ thông tin.');
      return;
    }

    if (new Date(startTime) >= new Date(endTime)) {
      setError('Thời gian kết thúc phải sau thời gian bắt đầu.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      if (booking) {
        const data: UpdateBookingRequest = {
          customerId: Number(customerId),
          serviceId: Number(serviceId),
          staffId: Number(staffId),
          startTime: toLocalDateTime(startTime),
          endTime: toLocalDateTime(endTime),
        };
        await updateBooking(booking.id, data);
      } else {
        const data: CreateBookingRequest = {
          customerId: Number(customerId),
          serviceId: Number(serviceId),
          staffId: Number(staffId),
          startTime: toLocalDateTime(startTime),
          endTime: toLocalDateTime(endTime),
        };
        await createBooking(data);
      }
      onSuccess();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Có lỗi xảy ra khi lưu lịch hẹn.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingData) {
    return <div className="p-4 text-center text-sm text-[var(--color-neutral-500)]">Đang tải dữ liệu...</div>;
  }

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {booking ? 'Chỉnh sửa lịch hẹn' : 'Tạo lịch hẹn'}
        </h3>
        
        {error && (
          <div className="mb-4 p-3 rounded-lg bg-[var(--color-error-bg)] border border-[var(--color-error-border)] text-[var(--color-error)] text-sm" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Select 
            label="Khách hàng" 
            value={customerId} 
            onChange={(e) => setCustomerId(e.target.value)}
            options={customers.map(c => ({ value: c.id, label: c.name }))}
            required
          />
          <Select 
            label="Dịch vụ" 
            value={serviceId} 
            onChange={(e) => setServiceId(e.target.value)}
            options={services.map(s => ({ value: s.id, label: `${s.name} (${s.durationMinutes} phút)` }))}
            required
          />
          <Select 
            label="Nhân viên" 
            value={staffId} 
            onChange={(e) => setStaffId(e.target.value)}
            options={staffList.map(s => ({ value: s.id, label: s.name }))}
            required
          />
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input 
              type="datetime-local" 
              label="Thời gian bắt đầu" 
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              required
            />
            <Input 
              type="datetime-local" 
              label="Thời gian kết thúc" 
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              required
            />
          </div>

          <div className="flex justify-end gap-3 mt-4">
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : (booking ? 'Lưu thay đổi' : 'Tạo lịch hẹn')}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
