import { useState, useEffect } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { createService, updateService, uploadServiceImage, deleteServiceImage } from '../lib/api/services';
import { getServiceCategories } from '../lib/api/serviceCategories';
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '../types/service';
import type { ServiceCategory } from '../types/serviceCategory';
import { Upload, Trash2, Image as ImageIcon, Sparkles, Plus, GripVertical } from 'lucide-react';

interface ServiceFormProps {
  service?: Service; // if undefined, it's create mode
  categories?: ServiceCategory[];
  onSuccess: () => void;
  onCancel: () => void;
}

export const ServiceForm = ({ service, categories: initialCategories, onSuccess, onCancel }: ServiceFormProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Categories
  const [categories, setCategories] = useState<ServiceCategory[]>(initialCategories || []);

  // Form state
  const [name, setName] = useState<string>(service ? service.name : '');
  const [description, setDescription] = useState<string>(service ? (service.description || '') : '');
  const [durationMinutes, setDurationMinutes] = useState<string>(service ? String(service.durationMinutes) : '60');
  const [price, setPrice] = useState<string>(service ? String(service.price) : '0');
  const [categoryId, setCategoryId] = useState<number | undefined>(service?.categoryId || undefined);
  const [isFeatured, setIsFeatured] = useState<boolean>(service ? (service.isFeatured ?? false) : false);
  const [isActive, setIsActive] = useState<boolean>(service ? service.isActive : true);

  // Process steps state
  const parseSteps = (raw?: string | null): string[] => {
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed;
    } catch {
      // If plain text separated by newlines
      return raw.split('\n').map(s => s.trim()).filter(Boolean);
    }
    return [];
  };

  const [steps, setSteps] = useState<string[]>(parseSteps(service?.processSteps));
  const [newStepText, setNewStepText] = useState('');

  // Image state
  const [currentImageUrl, setCurrentImageUrl] = useState<string | null>(service?.imageUrl || null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [deleteExistingImage, setDeleteExistingImage] = useState(false);

  useEffect(() => {
    if (!initialCategories || initialCategories.length === 0) {
      getServiceCategories()
        .then((cats) => setCategories(cats))
        .catch(() => {});
    }
  }, [initialCategories]);

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

  const handleAddStep = () => {
    if (!newStepText.trim()) return;
    const stepNumber = steps.length + 1;
    const cleanText = newStepText.trim().replace(/^\d+[.\s]*/, '');
    const formatted = `${stepNumber}. ${cleanText}`;
    setSteps([...steps, formatted]);
    setNewStepText('');
  };

  const handleRemoveStep = (index: number) => {
    const nextSteps = steps.filter((_, idx) => idx !== index);
    const renumbered = nextSteps.map((step, idx) => {
      const text = step.replace(/^\d+[.\s]*/, '');
      return `${idx + 1}. ${text}`;
    });
    setSteps(renumbered);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Vui lòng nhập tên dịch vụ.');
      return;
    }

    const duration = parseInt(durationMinutes, 10);
    if (isNaN(duration) || duration <= 0) {
      setError('Thời lượng dịch vụ phải là số nguyên dương.');
      return;
    }

    const priceVal = parseFloat(price);
    if (isNaN(priceVal) || priceVal < 0) {
      setError('Giá dịch vụ không được là số âm.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      let savedService: Service;
      const stepsPayload = steps.length > 0 ? JSON.stringify(steps) : undefined;

      if (service) {
        const data: UpdateServiceRequest = {
          name,
          description: description || undefined,
          durationMinutes: duration,
          price: priceVal,
          categoryId: categoryId || undefined,
          isFeatured,
          processSteps: stepsPayload,
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
          categoryId: categoryId || undefined,
          isFeatured,
          processSteps: stepsPayload,
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
          {/* Service Image Upload */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-2">
              Ảnh minh họa dịch vụ
            </label>
            <div className="flex items-center gap-4">
              <div className="w-24 h-24 rounded-2xl overflow-hidden bg-stone-100 border border-stone-200 flex items-center justify-center shrink-0">
                {imagePreview ? (
                  <img src={imagePreview} alt="Xem trước" className="w-full h-full object-cover" />
                ) : currentImageUrl ? (
                  <img src={currentImageUrl} alt="Dịch vụ" className="w-full h-full object-cover" />
                ) : (
                  <ImageIcon className="w-8 h-8 text-stone-400" />
                )}
              </div>
              <div className="flex flex-col gap-2">
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
                    <button
                      type="button"
                      onClick={handleRemoveImage}
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-rose-600 hover:bg-rose-50 text-xs font-semibold transition-colors"
                      disabled={isSubmitting}
                    >
                      <Trash2 size={13} />
                      <span>Xóa ảnh</span>
                    </button>
                  )}
                </div>
                <p className="text-[11px] text-stone-500">
                  Định dạng JPG, PNG hoặc WEBP (tối đa 5MB)
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
            minLength={2}
            maxLength={100}
            placeholder="VD: Chăm sóc da mặt chuyên sâu"
          />

          {/* Category selection */}
          <div>
            <label className="block text-xs font-semibold text-stone-700 mb-1">
              Danh mục dịch vụ
            </label>
            <select
              value={categoryId ?? ''}
              onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}
              className="w-full px-3.5 py-2 text-xs rounded-xl border border-stone-300 bg-white text-stone-800 focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
            >
              <option value="">-- Chưa phân loại danh mục --</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name} {!cat.isActive ? '(Tạm tắt)' : ''}
                </option>
              ))}
            </select>
          </div>

          <Input
            type="text"
            label="Mô tả tóm tắt"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Mô tả công dụng và trải nghiệm liệu trình..."
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              type="number"
              label="Thời lượng (phút)"
              required
              min={1}
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(e.target.value)}
              placeholder="VD: 60"
            />
            <Input
              type="number"
              label="Giá dịch vụ (VNĐ)"
              required
              min={0}
              step={1000}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder="VD: 300000"
            />
          </div>

          {/* Treatment / Process Steps Section */}
          <div className="pt-2 border-t border-stone-100">
            <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-2">
              Quy trình thực hiện ({steps.length} bước)
            </label>

            {steps.length > 0 && (
              <div className="space-y-2 mb-3">
                {steps.map((step, idx) => (
                  <div key={idx} className="flex items-center justify-between gap-2 p-2.5 rounded-xl bg-stone-50 border border-stone-200/80 text-xs text-stone-700">
                    <div className="flex items-center gap-2 min-w-0">
                      <GripVertical size={14} className="text-stone-400 shrink-0" />
                      <span className="truncate">{step}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRemoveStep(idx)}
                      className="p-1 rounded text-stone-400 hover:text-rose-600 hover:bg-stone-200 transition-colors shrink-0"
                      title="Xóa bước này"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div className="flex items-center gap-2">
              <input
                type="text"
                value={newStepText}
                onChange={(e) => setNewStepText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddStep();
                  }
                }}
                placeholder="Nhập tên bước thực hiện (VD: Tẩy tế bào chết enzyme)..."
                className="flex-1 px-3 py-2 text-xs rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
              />
              <button
                type="button"
                onClick={handleAddStep}
                className="inline-flex items-center gap-1 px-3 py-2 text-xs font-medium rounded-xl bg-stone-100 border border-stone-200 text-stone-700 hover:bg-stone-200 transition-colors shrink-0"
              >
                <Plus size={14} />
                <span>Thêm bước</span>
              </button>
            </div>
          </div>

          {/* Toggles */}
          <div className="space-y-3 pt-2 border-t border-stone-100">
            {/* Featured toggle */}
            <label className="flex items-start gap-2.5 cursor-pointer">
              <input
                type="checkbox"
                checked={isFeatured}
                onChange={(e) => setIsFeatured(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded border-stone-300 text-[#465d4c] focus:ring-[#465d4c]"
              />
              <div className="text-xs">
                <span className="font-semibold text-stone-800 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                  Dịch vụ nổi bật (Hiển thị trang chủ)
                </span>
                <span className="text-stone-500 block mt-0.5">
                  Bật để dịch vụ này xuất hiện trong mục &ldquo;Dịch vụ nổi bật&rdquo; trên trang chủ TIKEY SPA.
                </span>
              </div>
            </label>

            {/* Active toggle */}
            <label className="flex items-start gap-2.5 cursor-pointer">
              <input
                type="checkbox"
                checked={isActive}
                onChange={(e) => setIsActive(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded border-stone-300 text-[#465d4c] focus:ring-[#465d4c]"
              />
              <div className="text-xs">
                <span className="font-semibold text-stone-800">Dịch vụ đang hoạt động</span>
                <span className="text-stone-500 block mt-0.5">
                  Tắt nếu tạm ngừng phục vụ dịch vụ này (sẽ ẩn khỏi trang đặt lịch của khách hàng).
                </span>
              </div>
            </label>
          </div>

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
