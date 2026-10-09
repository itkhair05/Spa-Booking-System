import { useState } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { createReview, updateReview } from '../lib/api/reviews';
import type { Review, CreateReviewRequest, UpdateReviewRequest } from '../types/review';
import { Star } from 'lucide-react';

interface ReviewFormProps {
  review?: Review;
  onSuccess: () => void;
  onCancel: () => void;
}

const RATING_LABELS: Record<number, string> = {
  1: '1 sao — Chưa hài lòng',
  2: '2 sao — Tạm chấp nhận',
  3: '3 sao — Bình thường',
  4: '4 sao — Hài lòng',
  5: '5 sao — Rất tuyệt vời',
};

export const ReviewForm = ({ review, onSuccess, onCancel }: ReviewFormProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [customerName, setCustomerName] = useState(review ? review.customerName : '');
  const [rating, setRating] = useState<number>(review ? review.rating : 5);
  const [serviceName, setServiceName] = useState(review ? (review.serviceName || '') : '');
  const [comment, setComment] = useState(review ? review.comment : '');
  const [displayOrder, setDisplayOrder] = useState<number>(review ? review.displayOrder : 0);
  const [isPublished, setIsPublished] = useState<boolean>(review ? review.isPublished : false);
  const [isDemo, setIsDemo] = useState<boolean>(review ? review.isDemo : false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerName.trim()) {
      setError('Vui lòng nhập tên khách hàng.');
      return;
    }
    if (!comment.trim()) {
      setError('Vui lòng nhập nội dung đánh giá.');
      return;
    }
    if (rating < 1 || rating > 5) {
      setError('Số sao đánh giá phải từ 1 đến 5.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      if (review) {
        const updateData: UpdateReviewRequest = {
          customerName: customerName.trim(),
          rating,
          serviceName: serviceName.trim() || undefined,
          comment: comment.trim(),
          displayOrder,
          isPublished,
          isDemo,
        };
        await updateReview(review.id, updateData);
      } else {
        const createData: CreateReviewRequest = {
          customerName: customerName.trim(),
          rating,
          serviceName: serviceName.trim() || undefined,
          comment: comment.trim(),
          displayOrder,
          isPublished,
          isDemo,
        };
        await createReview(createData);
      }

      onSuccess();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Có lỗi xảy ra khi lưu đánh giá.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {review ? 'Chỉnh sửa đánh giá' : 'Thêm đánh giá mới'}
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
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            required
            placeholder="VD: Chị Minh Anh, Anh Tuấn Hùng"
          />

          {/* Rating selector */}
          <div>
            <label className="block text-[0.8125rem] font-semibold text-[var(--color-neutral-700)] mb-1.5">
              Đánh giá (Số sao) <span className="text-red-500">*</span>
            </label>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1">
                {[1, 2, 3, 4, 5].map((starValue) => {
                  const isActive = starValue <= rating;
                  return (
                    <button
                      key={starValue}
                      type="button"
                      onClick={() => setRating(starValue)}
                      className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                        isActive
                          ? 'text-amber-400 hover:text-amber-500 hover:bg-amber-50'
                          : 'text-stone-300 hover:text-stone-400 hover:bg-stone-50'
                      }`}
                      aria-label={`${starValue} sao`}
                    >
                      <Star
                        size={22}
                        className={isActive ? 'fill-amber-400 stroke-amber-400' : 'stroke-stone-300'}
                      />
                    </button>
                  );
                })}
              </div>
              <span className="text-xs font-medium text-stone-600">
                {RATING_LABELS[rating] || `${rating} sao`}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              type="text"
              label="Dịch vụ liên quan (tùy chọn)"
              value={serviceName}
              onChange={(e) => setServiceName(e.target.value)}
              placeholder="VD: Massage Body Thụy Điển (60p)"
            />

            <Input
              type="number"
              label="Thứ tự hiển thị"
              value={displayOrder}
              onChange={(e) => setDisplayOrder(Number(e.target.value))}
              hint="Số nhỏ hơn sẽ được ưu tiên hiển thị trước (VD: 0, 1, 2...)"
            />
          </div>

          <div>
            <label className="block text-[0.8125rem] font-semibold text-[var(--color-neutral-700)] mb-1">
              Nội dung nhận xét <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={4}
              required
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Chia sẻ chi tiết trải nghiệm của khách hàng..."
              className="w-full px-3.5 py-2.5 text-xs sm:text-sm rounded-xl border border-stone-300 bg-white text-stone-900 placeholder:text-stone-400 focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
            />
          </div>

          {/* Transparency & Demo flag */}
          <div className="bg-amber-50/60 border border-amber-200/70 rounded-xl p-3.5">
            <label className="flex items-start gap-2.5 text-xs font-semibold text-stone-800 cursor-pointer">
              <input
                type="checkbox"
                checked={isDemo}
                onChange={(e) => setIsDemo(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded border-stone-300 text-[#465d4c] focus:ring-[#465d4c]"
              />
              <div>
                <span>Đánh dấu là đánh giá mẫu / Showroom (minh họa)</span>
                <p className="text-[11px] font-normal text-stone-500 mt-0.5">
                  Đảm bảo chính sách minh bạch nội dung của TIKEY SPA. Các đánh giá mẫu sẽ được gắn thẻ &ldquo;Minh họa&rdquo; rõ ràng để khách hàng nhận biết.
                </p>
              </div>
            </label>
          </div>

          <div className="pt-2 border-t border-stone-100 flex items-center justify-between">
            <label className="flex items-center gap-2 text-xs font-semibold text-stone-700 cursor-pointer">
              <input
                type="checkbox"
                checked={isPublished}
                onChange={(e) => setIsPublished(e.target.checked)}
                className="w-4 h-4 rounded border-stone-300 text-[#465d4c] focus:ring-[#465d4c]"
              />
              Xuất bản hiển thị ngay trên website (Công khai)
            </label>

            <div className="flex gap-2">
              <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
                Hủy
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Đang lưu...' : (review ? 'Lưu thay đổi' : 'Tạo đánh giá')}
              </Button>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
