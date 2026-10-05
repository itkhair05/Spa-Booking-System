import { useState } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { createStaff, updateStaff, uploadStaffAvatar, deleteStaffAvatar } from '../lib/api/staff';
import type { Staff, CreateStaffRequest, UpdateStaffRequest } from '../types/staff';
import { Upload, Trash2, Camera } from 'lucide-react';

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
  const [showOnWebsite, setShowOnWebsite] = useState<boolean>(staff ? (staff.showOnWebsite ?? true) : true);

  // Avatar state
  const [currentAvatarUrl, setCurrentAvatarUrl] = useState<string | null>(staff?.avatarUrl || null);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [deleteExistingAvatar, setDeleteExistingAvatar] = useState(false);

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setError('Chỉ chấp nhận ảnh định dạng JPG, PNG hoặc WEBP.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('Dung lượng ảnh không được vượt quá 5MB.');
      return;
    }
    setAvatarFile(file);
    setAvatarPreview(URL.createObjectURL(file));
    setDeleteExistingAvatar(false);
    setError(null);
  };

  const handleRemoveAvatar = () => {
    setAvatarFile(null);
    setAvatarPreview(null);
    if (currentAvatarUrl) {
      setDeleteExistingAvatar(true);
      setCurrentAvatarUrl(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Vui lòng nhập họ và tên.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      let savedStaff: Staff;
      if (staff) {
        const data: UpdateStaffRequest = {
          name,
          phone: phone || undefined,
          email: email || undefined,
          isActive,
          showOnWebsite,
        };
        savedStaff = await updateStaff(staff.id, data);
        if (deleteExistingAvatar && !avatarFile) {
          await deleteStaffAvatar(staff.id);
        }
      } else {
        const data: CreateStaffRequest = {
          name,
          phone: phone || undefined,
          email: email || undefined,
          isActive,
          showOnWebsite,
        };
        savedStaff = await createStaff(data);
      }

      if (avatarFile) {
        await uploadStaffAvatar(savedStaff.id, avatarFile);
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
          <Alert tone="error" className="mb-4">
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Avatar upload & preview */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-2">
              Ảnh đại diện nhân viên
            </label>
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-full overflow-hidden bg-stone-100 border border-[#c6d8c9] flex items-center justify-center shrink-0">
                {avatarPreview ? (
                  <img src={avatarPreview} alt="Xem trước" className="w-full h-full object-cover" />
                ) : currentAvatarUrl ? (
                  <img src={currentAvatarUrl} alt={name || 'Avatar'} className="w-full h-full object-cover" />
                ) : (
                  <Camera className="w-6 h-6 text-stone-400" />
                )}
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <label className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-stone-100 hover:bg-stone-200 text-stone-800 text-xs font-semibold cursor-pointer transition-colors border border-stone-200">
                    <Upload size={13} />
                    <span>{avatarPreview || currentAvatarUrl ? 'Thay ảnh' : 'Tải ảnh lên'}</span>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      className="hidden"
                      onChange={handleAvatarChange}
                      disabled={isSubmitting}
                    />
                  </label>
                  {(avatarPreview || currentAvatarUrl) && (
                    <Button
                      type="button"
                      size="sm"
                      variant="secondary"
                      onClick={handleRemoveAvatar}
                      disabled={isSubmitting}
                      className="text-rose-700 hover:bg-rose-50 border-rose-200 text-xs py-1.5 h-auto"
                    >
                      <Trash2 size={13} className="mr-1 text-rose-600" />
                      <span>Gỡ ảnh</span>
                    </Button>
                  )}
                </div>
                <p className="text-[11px] text-stone-500">
                  Định dạng: JPG, PNG, WEBP (tối đa 5MB).
                </p>
              </div>
            </div>
          </div>

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
              checked={showOnWebsite}
              onChange={(e) => setShowOnWebsite(e.target.checked)}
              className="w-4 h-4 rounded border-[var(--color-neutral-300)] text-[var(--color-brand-600)] focus:ring-[var(--color-brand-500)]"
            />
            Hiển thị trên website (Đội ngũ công khai)
          </label>

          <label className="flex items-center gap-2 text-sm font-semibold text-[var(--color-neutral-700)] mt-1 cursor-pointer">
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
