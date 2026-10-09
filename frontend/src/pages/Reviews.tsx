import { useState, useEffect, useCallback, useMemo } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getReviews, publishReview, unpublishReview, deleteReview } from '../lib/api/reviews';
import { formatDateDMY } from '../lib/format';
import type { Review } from '../types/review';
import { ReviewForm } from './ReviewForm';
import RestrictedAccess from '../components/RestrictedAccess';
import { Star, Eye, EyeOff, Trash2, Edit2, Plus, Search, Sparkles } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

type FilterTab = 'ALL' | 'PUBLISHED' | 'UNPUBLISHED' | 'DEMO';

const ReviewsPage = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [reviews, setReviews] = useState<Review[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state (matches Articles.tsx pattern)
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingReview, setEditingReview] = useState<Review | undefined>(undefined);

  // Delete state
  const [deletingReview, setDeletingReview] = useState<Review | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Filter & Search state
  const [filterTab, setFilterTab] = useState<FilterTab>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

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
    if (isOwner) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchReviews();
    }
  }, [fetchReviews, isOwner]);

  const handleOpenForm = (review?: Review) => {
    if (!isOwner) return;
    setEditingReview(review);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingReview(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchReviews();
  };

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

  // Filter & Search logic
  const filteredReviews = useMemo(() => {
    return reviews.filter((r) => {
      let matchesTab = true;
      if (filterTab === 'PUBLISHED') matchesTab = r.isPublished;
      else if (filterTab === 'UNPUBLISHED') matchesTab = !r.isPublished;
      else if (filterTab === 'DEMO') matchesTab = r.isDemo;

      const query = searchQuery.trim().toLowerCase();
      if (!query) return matchesTab;

      const matchesQuery =
        r.customerName?.toLowerCase().includes(query) ||
        r.comment?.toLowerCase().includes(query) ||
        (r.serviceName && r.serviceName.toLowerCase().includes(query));

      return matchesTab && matchesQuery;
    });
  }, [reviews, filterTab, searchQuery]);

  if (!isOwner) {
    return (
      <RestrictedAccess
        shellTitle="Đánh giá"
        icon={<Star className="w-7 h-7" />}
        message="Khu vực kiểm duyệt và quản lý đánh giá khách hàng chỉ dành riêng cho Quản trị viên (OWNER)."
      />
    );
  }

  if (isFormOpen && isOwner) {
    return (
      <AppShell title="Đánh giá hiển thị">
        <PageHeader title={editingReview ? 'Chỉnh sửa đánh giá' : 'Thêm đánh giá mới'} />
        <div className="max-w-2xl mx-auto">
          <ReviewForm
            review={editingReview}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  const tabCounts = {
    ALL: reviews.length,
    PUBLISHED: reviews.filter((r) => r.isPublished).length,
    UNPUBLISHED: reviews.filter((r) => !r.isPublished).length,
    DEMO: reviews.filter((r) => r.isDemo).length,
  };

  return (
    <AppShell title="Đánh giá hiển thị">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader
          title="Quản lý Đánh giá Hiển thị"
          description="Kiểm duyệt, quản lý và thêm các đánh giá thực tế từ khách hàng trải nghiệm tại TIKEY SPA."
        />
        <Button onClick={() => handleOpenForm()}>
          <Plus size={16} />
          Thêm đánh giá mới
        </Button>
      </div>

      {/* Transparency Note */}
      <div className="bg-amber-50/70 border border-amber-200/80 rounded-2xl p-4 mb-6 text-xs text-amber-900 leading-relaxed flex items-start gap-3">
        <Sparkles size={18} className="text-amber-600 shrink-0 mt-0.5" />
        <div>
          <strong>Chính sách minh bạch nội dung:</strong> TIKEY SPA luôn tôn trọng trải nghiệm chân thực của khách hàng. Các phản hồi mẫu từ showroom trải nghiệm được đánh dấu rõ ràng là <em>Minh họa</em> để phân biệt với đánh giá trực tiếp.
        </div>
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 mb-6">
        {/* Status Tabs */}
        <div className="flex items-center gap-1 p-1 bg-stone-100 rounded-xl overflow-x-auto scrollbar-none">
          {(
            [
              { key: 'ALL', label: 'Tất cả' },
              { key: 'PUBLISHED', label: 'Đang hiển thị' },
              { key: 'UNPUBLISHED', label: 'Đang ẩn' },
              { key: 'DEMO', label: 'Minh họa' },
            ] as const
          ).map((t) => {
            const active = filterTab === t.key;
            return (
              <button
                key={t.key}
                type="button"
                onClick={() => setFilterTab(t.key)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 cursor-pointer ${
                  active
                    ? 'bg-white text-stone-900 shadow-2xs font-semibold'
                    : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                <span>{t.label}</span>
                <span
                  className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                    active ? 'bg-stone-100 text-stone-700' : 'text-stone-400'
                  }`}
                >
                  {tabCounts[t.key]}
                </span>
              </button>
            );
          })}
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-72">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-stone-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo tên khách, dịch vụ, nội dung..."
            className="w-full h-9 pl-9 pr-3 text-xs rounded-xl bg-white border border-stone-200 text-stone-900 placeholder:text-stone-400 focus:outline-none focus:border-[#465d4c] focus:ring-1 focus:ring-[#465d4c]"
          />
        </div>
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-44 bg-stone-100 rounded-xl" />
            </Card>
          ))}
        </div>
      )}

      {/* Error State */}
      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchReviews} />
      )}

      {/* Empty State - No reviews at all */}
      {!isLoading && !error && reviews.length === 0 && (
        <EmptyState
          icon={<Star size={22} />}
          title="Chưa có đánh giá nào"
          description="Tạo đánh giá đầu tiên từ khách hàng hoặc showroom để hiển thị trên website."
          action={
            <Button onClick={() => handleOpenForm()}>
              <Plus size={16} />
              Thêm đánh giá mới
            </Button>
          }
        />
      )}

      {/* Empty State - Filter or Search returned 0 */}
      {!isLoading && !error && reviews.length > 0 && filteredReviews.length === 0 && (
        <div className="p-8 text-center bg-stone-50 rounded-2xl border border-stone-200/80 my-4">
          <Search size={28} className="text-stone-300 mx-auto mb-2" />
          <h4 className="text-sm font-semibold text-stone-800 mb-1">Không tìm thấy đánh giá phù hợp</h4>
          <p className="text-xs text-stone-500 mb-3">
            {searchQuery
              ? `Không có đánh giá nào khớp với từ khóa "${searchQuery}".`
              : 'Không có đánh giá nào trong bộ lọc này.'}
          </p>
          {(searchQuery || filterTab !== 'ALL') && (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                setSearchQuery('');
                setFilterTab('ALL');
              }}
            >
              Xóa bộ lọc
            </Button>
          )}
        </div>
      )}

      {/* Reviews Cards Grid */}
      {!isLoading && !error && filteredReviews.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredReviews.map((r) => (
            <Card
              key={r.id}
              className="border-[#e7e2d8] hover:border-[#c6d8c9] transition-all flex flex-col justify-between"
            >
              <CardContent className="p-5 flex flex-col h-full grow">
                {/* Header: Stars & Badges */}
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div className="flex items-center text-[#b8976c]">
                    {Array.from({ length: 5 }).map((_, idx) => (
                      <Star
                        key={idx}
                        size={15}
                        className={
                          idx < (r.rating || 5)
                            ? 'fill-amber-400 stroke-amber-400'
                            : 'stroke-stone-300'
                        }
                      />
                    ))}
                  </div>
                  <div className="flex items-center gap-1.5 flex-wrap justify-end">
                    {r.isDemo && (
                      <Badge tone="warning">
                        Minh họa
                      </Badge>
                    )}
                    <Badge tone={r.isPublished ? 'success' : 'neutral'}>
                      {r.isPublished ? 'Đang hiển thị' : 'Đang ẩn'}
                    </Badge>
                  </div>
                </div>

                {/* Customer & Service */}
                <div className="mb-2">
                  <h4 className="font-semibold text-stone-900 text-sm leading-snug">
                    {r.customerName}
                  </h4>
                  {r.serviceName && (
                    <span className="text-xs text-[#566f5c] font-medium block truncate mt-0.5">
                      {r.serviceName}
                    </span>
                  )}
                </div>

                {/* Comment */}
                <p className="text-xs text-stone-600 italic leading-relaxed mb-4 grow">
                  &ldquo;{r.comment}&rdquo;
                </p>

                {/* Metadata */}
                <div className="text-[11px] text-stone-400 mb-3 flex items-center justify-between">
                  <span>Ngày gửi: {formatDateDMY(r.createdAt)}</span>
                  <span>Thứ tự: {r.displayOrder}</span>
                </div>

                {/* Actions */}
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
                        <span>Gỡ hiển thị</span>
                      </>
                    ) : (
                      <>
                        <Eye size={12} />
                        <span>Duyệt hiển thị</span>
                      </>
                    )}
                  </button>

                  <div className="flex items-center gap-1.5">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleOpenForm(r)}
                    >
                      <Edit2 size={13} />
                      Sửa
                    </Button>
                    <Button
                      variant="danger-outline"
                      size="sm"
                      onClick={() => handleDeleteClick(r)}
                      aria-label={`Xóa đánh giá của ${r.customerName}`}
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

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deletingReview !== null}
        title="Xóa đánh giá này?"
        description={
          <>
            Đánh giá của <strong>{deletingReview?.customerName}</strong> sẽ bị xóa hoàn toàn khỏi hệ thống.
          </>
        }
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
