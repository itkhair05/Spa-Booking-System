import { useState } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { createService, updateService, uploadServiceImage, deleteServiceImage } from '../lib/api/services';
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '../types/service';
import { Upload, Trash2, Image as ImageIcon } from 'lucide-react';

interface ServiceFormProps {
  service?: Service; // if undefined, it's create mode
  onSuccess: () => void;
  onCancel: () => void;
}

export const ServiceForm = ({ service, onSuccess, onCancel }: ServiceFormProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [name, setName] = useState<string>(service ? service.name : '');
  const [description, setDescription] = useState<string>(service ? (service.description || '') : '');
  const [durationMinutes, setDurationMinutes] = useState<string>(service ? String(service.durationMinutes) : '60');
  const [price, setPrice] = useState<string>(service ? String(service.price) : '0');
  const [isActive, setIsActive] = useState<boolean>(service ? service.isActive : true);

  // Image state
  const [currentImageUrl, setCurrentImageUrl] = useState<string | null>(service?.imageUrl || null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [deleteExistingImage, setDeleteExistingImage] = useState(false);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
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
    setImageFile(file);
    setImagePreview(URL.createObjectURL(file));
    setDeleteExistingImage(false);
    setError(null);
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    setImagePreview(null);
    if (currentImageUrl) {
      setDeleteExistingImage(true);
      setCurrentImageUrl(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !durationMinutes || !price) {
      setError('Vui lòng nhập tên, thời lượng và giá.');
      return;
    }

    const duration = parseInt(durationMinutes, 10);
    const priceVal = parseFloat(price);

    if (isNaN(duration) || duration < 1) {
      setError('Thời lượng phải ít nhất 1 phút.');
      return;
    }
    
    if (isNaN(priceVal) || priceVal < 0) {
      setError('Giá không được âm.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      let savedService: Service;
      if (service) {
        const data: UpdateServiceRequest = {
          name,
          description: description || undefined,
          durationMinutes: duration,
          price: priceVal,
          isActive,
        };
        savedService = await updateService(service.id, data);
        if (deleteExistingImage && !imageFile) {
          await deleteServiceImage(service.id);
        }
      } else {
        const data: CreateServiceRequest = {
          name,
          description: description || undefined,
          durationMinutes: duration,
          price: priceVal,
          isActive,
        };
        savedService = await createService(data);
      }

      if (imageFile) {
        await uploadServiceImage(savedService.id, imageFile);
      }

      onSuccess();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Có lỗi xảy ra khi lưu dịch vụ.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {service ? 'Chỉnh sửa dịch vụ' : 'Thêm dịch vụ mới'}
        </h3>
        
        {error && (
          <Alert tone="error" className="mb-4">
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Service Demo Image Preview & Upload */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-2">
              Ảnh minh họa dịch vụ
            </label>
            <div className="flex items-center gap-4">
              <div className="w-24 h-20 rounded-xl overflow-hidden bg-stone-100 border border-[#c6d8c9] flex items-center justify-center shrink-0">
                {imagePreview ? (
                  <img src={imagePreview} alt="Xem trước ảnh" className="w-full h-full object-cover" />
                ) : currentImageUrl ? (
                  <img src={currentImageUrl} alt={name || 'Ảnh dịch vụ'} className="w-full h-full object-cover" />
                ) : (
                  <ImageIcon className="w-7 h-7 text-stone-400" />
                )}
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <label className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-stone-100 hover:bg-stone-200 text-stone-800 text-xs font-semibold cursor-pointer transition-colors border border-stone-200">
                    <Upload size={13} />
                    <span>{imagePreview || currentImageUrl ? 'Thay ảnh' : 'Tải ảnh lên'}</span>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      className="hidden"
                      onChange={handleImageChange}
                      disabled={isSubmitting}
                    />
                  </label>
                  {(imagePreview || currentImageUrl) && (
                    <Button
                      type="button"
                      size="sm"
                      variant="secondary"
                      onClick={handleRemoveImage}
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
            label="Tên dịch vụ" 
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            placeholder="VD: Massage cổ vai gáy"
          />
          <Input 
            type="text" 
            label="Mô tả" 
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Mô tả thêm (không bắt buộc)"
          />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input 
              type="number" 
              label="Thời lượng (phút)" 
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(e.target.value)}
              required
              min="1"
            />
            <Input 
              type="number" 
              label="Giá (VNĐ)" 
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
              min="0"
              step="1000"
            />
          </div>
          
          <label className="flex items-center gap-2 text-sm font-semibold text-[var(--color-neutral-700)] mt-2 cursor-pointer">
            <input 
              type="checkbox" 
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="w-4 h-4 rounded border-[var(--color-neutral-300)] text-[var(--color-brand-600)] focus:ring-[var(--color-brand-500)]"
            />
            Dịch vụ đang hoạt động
          </label>

          <div className="flex justify-end gap-3 mt-4">
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : (service ? 'Lưu thay đổi' : 'Thêm dịch vụ')}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
