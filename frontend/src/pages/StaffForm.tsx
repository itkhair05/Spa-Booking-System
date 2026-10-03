import { useState } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { createStaff, updateStaff } from '../lib/api/staff';
import type { Staff, CreateStaffRequest, UpdateStaffRequest } from '../types/staff';

interface StaffFormProps {
  staff?: Staff; // if undefined, it's create mode
  onSuccess: () => void;
  onCancel: () => void;
}

export const StaffForm = ({ staff, onSuccess, onCancel }: StaffFormProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [name, setName] = useState<string>(staff ? staff.name : '');
  const [phone, setPhone] = useState<string>(staff ? (staff.phone || '') : '');
  const [email, setEmail] = useState<string>(staff ? (staff.email || '') : '');
  const [isActive, setIsActive] = useState<boolean>(staff ? staff.isActive : true);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Vui lòng nhập họ và tên.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      if (staff) {
        const data: UpdateStaffRequest = {
          name,
          phone: phone || undefined,
          email: email || undefined,
          isActive,
        };
        await updateStaff(staff.id, data);
      } else {
        const data: CreateStaffRequest = {
          name,
          phone: phone || undefined,
          email: email || undefined,
          isActive,
        };
        await createStaff(data);
      }
      onSuccess();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Có lỗi xảy ra khi lưu nhân viên.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {staff ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên'}
        </h3>
        
        {error && (
          <div className="mb-4 p-3 rounded-lg bg-[var(--color-error-bg)] border border-[var(--color-error-border)] text-[var(--color-error)] text-sm" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Input 
            type="text" 
            label="Họ và tên" 
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
          
          <label className="flex items-center gap-2 text-sm font-semibold text-[var(--color-neutral-700)] mt-2 cursor-pointer">
            <input 
              type="checkbox" 
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="w-4 h-4 rounded border-[var(--color-neutral-300)] text-[var(--color-brand-600)] focus:ring-[var(--color-brand-500)]"
            />
            Đang hoạt động
          </label>

          <div className="flex justify-end gap-3 mt-4">
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : (staff ? 'Lưu thay đổi' : 'Thêm nhân viên')}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
