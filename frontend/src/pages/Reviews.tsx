import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getReviews, publishReview, unpublishReview, updateReview, deleteReview } from '../lib/api/reviews';
import { formatDateDMY } from '../lib/format';
import type { Review } from '../types/review';
import { Star, Eye, EyeOff, Trash2, Edit2, X } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const ReviewsPage = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [reviews, setReviews] = useState<Review[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Edit review modal
  const [editingReview, setEditingReview] = useState<Review | null>(null);
  const [editComment, setEditComment] = useState('');
  const [editOrder, setEditOrder] = useState<number>(0);
  const [isUpdating, setIsUpdating] = useState(false);

  // Delete state
  const [deletingReview, setDeletingReview] = useState<Review | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const fetchReviews = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getReviews();
      setReviews(data);
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Không thể tải danh sách đánh giá.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchReviews();
  }, [fetchReviews]);

  const handleTogglePublish = async (review: Review) => {
    if (!isOwner) return;
    try {
      if (review.isPublished) {
        await unpublishReview(review.id);
      } else {
        await publishReview(review.id);
      }
      await fetchReviews();
    } catch {
      // ignore
    }
  };

  const handleOpenEdit = (review: Review) => {
    setEditingReview(review);
    setEditComment(review.comment);
    setEditOrder(review.displayOrder || 0);
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingReview) return;
    setIsUpdating(true);
    try {
      await updateReview(editingReview.id, {
        comment: editComment.trim(),
        displayOrder: editOrder,
      });
      setEditingReview(null);
      await fetchReviews();
    } catch {
      // ignore
    } finally {
      setIsUpdating(false);
    }
  };

  const handleDeleteClick = (review: Review) => {
    if (!isOwner) return;
    setDeleteError(null);
    setDeletingReview(review);
  };

  const handleConfirmDelete = async () => {
    if (!deletingReview) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteReview(deletingReview.id);
      setDeletingReview(null);
      fetchReviews();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setDeleteError(errObj.response?.data?.message || 'Không thể xóa đánh giá.');
    } finally {
      setIsDeleting(false);
    }
  };

  if (!isOwner) {
    return (
      <AppShell title="Đánh giá">
        <div className="max-w-2xl mx-auto py-16 text-center px-4">
          <div className="w-14 h-14 rounded-full bg-amber-50 text-amber-700 flex items-center justify-center mx-auto mb-4 border border-amber-200">
            <Star className="w-7 h-7" />
          </div>
          <h2 className="text-xl font-serif-title font-semibold text-stone-900 mb-2">Quyền truy cập hạn chế</h2>
          <p className="text-stone-600 text-sm leading-relaxed mb-6">
            Khu vực kiểm duyệt đánh giá khách hàng chỉ dành riêng cho Quản trị viên (OWNER).
          </p>
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Đánh giá hiển thị">
      <PageHeader
        title="Quản lý Đánh giá Hiển thị"
        description="Kiểm duyệt và quản lý các đánh giá hiển thị công khai trên website TIKEY SPA. Chỉ các đánh giá được phê duyệt mới xuất hiện với khách hàng."
      />

      <div className="bg-amber-50/70 border border-amber-200/80 rounded-2xl p-4 mb-6 text-xs text-amber-900 leading-relaxed">
        <strong>Chính sách minh bạch nội dung:</strong> TIKEY SPA không cho phép tạo đánh giá giả hay ngụy tạo thông tin khách hàng. Các đánh giá có sẵn từ showroom trải nghiệm được đánh dấu rõ ràng là <em>Minh họa</em>.
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-40 bg-stone-100 rounded-xl" />
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchReviews} />
      )}

      {!isLoading && !error && reviews.length === 0 && (
        <EmptyState
          icon={<Star size={22} />}
          title="Chưa có đánh giá nào"
          description="Hiện chưa có đánh giá nào từ khách hàng hoặc showroom."
        />
      )}

      {!isLoading && !error && reviews.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {reviews.map((r) => (
            <Card key={r.id} className="border-[#e7e2d8] hover:border-[#c6d8c9] transition-all flex flex-col justify-between">
              <CardContent className="p-5 flex flex-col h-full grow">
                <div className="flex items-center justify-between gap-2 mb-2">
                  <div className="flex items-center text-[#b8976c]">
                    {Array.from({ length: r.rating || 5 }).map((_, idx) => (
                      <Star key={idx} size={14} className="fill-current" />
                    ))}
                  </div>
                  <div className="flex items-center gap-1.5">
                    {r.isDemo && (
                      <span className="text-[10px] text-amber-800 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded font-medium">
                        Minh họa
                      </span>
                    )}
                    <Badge tone={r.isPublished ? 'success' : 'neutral'}>
                      {r.isPublished ? 'Đang hiển thị' : 'Đang ẩn'}
                    </Badge>
                  </div>
                </div>

                <div className="mb-2">
                  <h4 className="font-semibold text-stone-900 text-sm">{r.customerName}</h4>
                  {r.serviceName && (
                    <span className="text-xs text-[#566f5c] font-medium block truncate">
                      {r.serviceName}
                    </span>
                  )}
                </div>

                <p className="text-xs text-stone-600 italic leading-relaxed mb-4 grow">
                  &ldquo;{r.comment}&rdquo;
                </p>

                <div className="text-[11px] text-stone-400 mb-3">
                  Ngày gửi: {formatDateDMY(r.createdAt)} • Thứ tự: {r.displayOrder}
                </div>

                <div className="mt-auto pt-3 border-t border-stone-100 flex items-center justify-between gap-2">
                  <button
                    type="button"
                    onClick={() => handleTogglePublish(r)}
                    className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1 rounded-lg border transition-colors cursor-pointer ${
                      r.isPublished
                        ? 'bg-stone-50 text-stone-600 border-stone-200 hover:bg-stone-100'
                        : 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100'
                    }`}
                  >
                    {r.isPublished ? (
                      <>
                        <EyeOff size={12} />
                        <span>Ẩn khỏi web</span>
                      </>
                    ) : (
                      <>
                        <Eye size={12} />
                        <span>Duyệt hiển thị</span>
                      </>
                    )}
                  </button>

                  <div className="flex items-center gap-1.5">
                    <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(r)}>
                      <Edit2 size={13} />
                      Sửa
                    </Button>
                    <Button
                      variant="danger-outline"
                      size="sm"
                      onClick={() => handleDeleteClick(r)}
                      aria-label="Xóa đánh giá"
                    >
                      <Trash2 size={14} />
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Edit Review Modal */}
      {editingReview && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-xl border border-stone-200 relative">
            <div className="flex items-center justify-between pb-3 border-b border-stone-100">
              <h3 className="font-serif-title font-semibold text-base text-stone-900">
                Chỉnh sửa đánh giá của {editingReview.customerName}
              </h3>
              <button
                type="button"
                onClick={() => setEditingReview(null)}
                className="p-1.5 text-stone-400 hover:text-stone-700 rounded-full hover:bg-stone-100"
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSaveEdit} className="py-4 space-y-4 text-left">
              <div>
                <label className="block text-xs font-semibold text-stone-700 mb-1">
                  Nội dung đánh giá
                </label>
                <textarea
                  rows={4}
                  required
                  value={editComment}
                  onChange={(e) => setEditComment(e.target.value)}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-stone-700 mb-1">
                  Thứ tự hiển thị (ưu tiên số nhỏ hơn)
                </label>
                <input
                  type="number"
                  value={editOrder}
                  onChange={(e) => setEditOrder(Number(e.target.value))}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 border-t border-stone-100">
                <Button type="button" variant="secondary" size="sm" onClick={() => setEditingReview(null)}>
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={isUpdating}>
                  {isUpdating ? 'Đang lưu...' : 'Lưu thay đổi'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={deletingReview !== null}
        title="Xóa đánh giá này?"
        description="Đánh giá sẽ bị xóa hoàn toàn khỏi cơ sở dữ liệu."
        confirmLabel="Xóa đánh giá"
        cancelLabel="Giữ lại"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingReview(null)}
        busy={isDeleting}
        error={deleteError}
      />
    </AppShell>
  );
};

export default ReviewsPage;
