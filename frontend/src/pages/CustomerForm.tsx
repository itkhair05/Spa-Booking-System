import { useState } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { createCustomer, updateCustomer } from '../lib/api/customers';
import { formatDateDMY } from '../lib/format';
import type { Customer, CreateCustomerRequest, UpdateCustomerRequest } from '../types/customer';

interface CustomerFormProps {
  customer?: Customer; // if undefined, it's create mode
  onSuccess: () => void;
  onCancel: () => void;
}

export const CustomerForm = ({ customer, onSuccess, onCancel }: CustomerFormProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [name, setName] = useState<string>(customer ? customer.name : '');
  const [phone, setPhone] = useState<string>(customer ? (customer.phone || '') : '');
  const [email, setEmail] = useState<string>(customer ? (customer.email || '') : '');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Vui lòng nhập tên khách hàng.');
      return;
    }

    const rawPhone = phone.trim();
    const normalizedPhone = rawPhone.replace(/[\s.-]/g, '');
    if (rawPhone && !/^0\d{9}$/.test(normalizedPhone)) {
      setError('Số điện thoại phải gồm đúng 10 chữ số.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      if (customer) {
        const data: UpdateCustomerRequest = {
          name: name.trim(),
          phone: normalizedPhone || undefined,
          email: email.trim() || undefined,
        };
        await updateCustomer(customer.id, data);
      } else {
        const data: CreateCustomerRequest = {
          name: name.trim(),
          phone: normalizedPhone || undefined,
          email: email.trim() || undefined,
        };
        await createCustomer(data);
      }
      onSuccess();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Có lỗi xảy ra khi lưu khách hàng.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {customer ? 'Chỉnh sửa khách hàng' : 'Thêm khách hàng'}
        </h3>
        
        {error && (
          <Alert tone="error" className="mb-4">
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Input 
            type="text" 
            label="Tên khách hàng" 
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            minLength={2}
            maxLength={100}
            placeholder="VD: Nguyễn Văn A"
          />
          <Input 
            type="tel" 
            label="Số điện thoại" 
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="VD: 0912345678"
          />
          <Input 
            type="email" 
            label="Email" 
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="VD: nguyen@example.com"
          />

          {customer?.lastVisit && (
            <div className="text-sm text-[var(--color-neutral-500)] mt-2">
              <span className="font-semibold text-[var(--color-neutral-700)]">Lần ghé gần nhất: </span>
              {formatDateDMY(customer.lastVisit)}
            </div>
          )}

          <div className="flex justify-end gap-3 mt-4">
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : (customer ? 'Lưu thay đổi' : 'Thêm khách hàng')}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
