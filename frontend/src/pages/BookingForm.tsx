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
        setError('Failed to load related data for booking.');
      } finally {
        setIsLoadingData(false);
      }
    };
    fetchData();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerId || !serviceId || !staffId || !startTime || !endTime) {
      setError('All fields are required.');
      return;
    }

    if (new Date(startTime) >= new Date(endTime)) {
      setError('End time must be after start time.');
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
          startTime: new Date(startTime).toISOString(),
          endTime: new Date(endTime).toISOString(),
        };
        await updateBooking(booking.id, data);
      } else {
        const data: CreateBookingRequest = {
          customerId: Number(customerId),
          serviceId: Number(serviceId),
          staffId: Number(staffId),
          startTime: new Date(startTime).toISOString(),
          endTime: new Date(endTime).toISOString(),
        };
        await createBooking(data);
      }
      onSuccess();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'An error occurred while saving the booking.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingData) {
    return <div className="p-4 text-center text-sm text-[var(--color-neutral-500)]">Loading form data...</div>;
  }

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {booking ? 'Edit Booking' : 'New Booking'}
        </h3>
        
        {error && (
          <div className="mb-4 p-3 rounded-lg bg-[var(--color-error-bg)] border border-[var(--color-error-border)] text-[var(--color-error)] text-sm" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Select 
            label="Customer" 
            value={customerId} 
            onChange={(e) => setCustomerId(e.target.value)}
            options={customers.map(c => ({ value: c.id, label: c.name }))}
            required
          />
          <Select 
            label="Service" 
            value={serviceId} 
            onChange={(e) => setServiceId(e.target.value)}
            options={services.map(s => ({ value: s.id, label: `${s.name} (${s.durationMinutes} mins)` }))}
            required
          />
          <Select 
            label="Staff" 
            value={staffId} 
            onChange={(e) => setStaffId(e.target.value)}
            options={staffList.map(s => ({ value: s.id, label: s.name }))}
            required
          />
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input 
              type="datetime-local" 
              label="Start Time" 
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              required
            />
            <Input 
              type="datetime-local" 
              label="End Time" 
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              required
            />
          </div>

          <div className="flex justify-end gap-3 mt-4">
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving...' : (booking ? 'Save Changes' : 'Create Booking')}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
