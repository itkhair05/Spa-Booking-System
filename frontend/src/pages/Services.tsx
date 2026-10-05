import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getServices, deleteService, updateService } from '../lib/api/services';
import {
  getServiceCategories,
  createServiceCategory,
  updateServiceCategory,
  deleteServiceCategory,
} from '../lib/api/serviceCategories';
import { formatCurrency } from '../lib/format';
import type { Service } from '../types/service';
import type { ServiceCategory } from '../types/serviceCategory';
import { ServiceForm } from './ServiceForm';
import { Scissors, Trash2, Plus, Sparkles, FolderTree, X, Edit2, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const Services = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingService, setEditingService] = useState<Service | undefined>(undefined);

  // Delete state
  const [deletingService, setDeletingService] = useState<Service | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Category modal state
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [newCatName, setNewCatName] = useState('');
  const [newCatDesc, setNewCatDesc] = useState('');
  const [categoryError, setCategoryError] = useState<string | null>(null);
  const [editingCatId, setEditingCatId] = useState<number | null>(null);
  const [editCatName, setEditCatName] = useState('');

  const fetchServices = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getServices();
      setServices(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Không thể tải dữ liệu dịch vụ.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchCategories = useCallback(async () => {
    if (!isOwner) return;
    setCategoryLoading(true);
    try {
      const cats = await getServiceCategories();
      setCategories(cats);
    } catch {
      // ignore
    } finally {
      setCategoryLoading(false);
    }
  }, [isOwner]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchServices();
  }, [fetchServices]);

  const handleOpenForm = (service?: Service) => {
    if (!isOwner) return;
    setEditingService(service);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingService(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchServices();
  };

  const handleDeleteClick = (service: Service) => {
    if (!isOwner) return;
    setDeleteError(null);
    setDeletingService(service);
  };

  const handleConfirmDelete = async () => {
    if (!deletingService) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteService(deletingService.id);
      setDeletingService(null);
      fetchServices();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setDeleteError(errorObj.response?.data?.message || 'Không thể xóa dịch vụ.');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleToggleFeatured = async (service: Service) => {
    if (!isOwner) return;
    try {
      await updateService(service.id, {
        name: service.name,
        durationMinutes: service.durationMinutes,
        price: service.price,
        isActive: service.isActive,
        isFeatured: !service.isFeatured,
      });
      await fetchServices();
    } catch {
      // ignore
    }
  };

  // Category Modal Handlers
  const handleOpenCategories = () => {
    setIsCategoryModalOpen(true);
    setCategoryError(null);
    fetchCategories();
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCatName.trim()) return;
    setCategoryError(null);
    try {
      await createServiceCategory({
        name: newCatName.trim(),
        description: newCatDesc.trim() || undefined,
        displayOrder: categories.length + 1,
        isActive: true,
      });
      setNewCatName('');
      setNewCatDesc('');
      await fetchCategories();
      await fetchServices();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setCategoryError(errObj.response?.data?.message || 'Không thể tạo danh mục.');
    }
  };

  const handleSaveEditCategory = async (id: number) => {
    if (!editCatName.trim()) return;
    setCategoryError(null);
    try {
      await updateServiceCategory(id, {
        name: editCatName.trim(),
      });
      setEditingCatId(null);
      await fetchCategories();
      await fetchServices();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setCategoryError(errObj.response?.data?.message || 'Không thể cập nhật danh mục.');
    }
  };

  const handleToggleCategoryActive = async (cat: ServiceCategory) => {
    try {
      await updateServiceCategory(cat.id, {
        isActive: !cat.isActive,
      });
      await fetchCategories();
    } catch {
      // ignore
    }
  };

  const handleDeleteCategory = async (id: number) => {
    setCategoryError(null);
    try {
      await deleteServiceCategory(id);
      await fetchCategories();
      await fetchServices();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setCategoryError(errObj.response?.data?.message || 'Không thể xóa danh mục đang có dịch vụ liên kết.');
    }
  };

  if (isFormOpen && isOwner) {
    return (
      <AppShell title="Dịch vụ">
        <PageHeader title="Quản lý Dịch vụ" />
        <div className="max-w-2xl mx-auto">
          <ServiceForm
            service={editingService}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Dịch vụ">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader
          title="Quản lý Dịch vụ TIKEY SPA"
          description="Danh mục dịch vụ, quy trình liệu trình và phân loại danh mục."
        />
        {isOwner && (
          <div className="flex items-center gap-2.5">
            <Button variant="secondary" onClick={handleOpenCategories}>
              <FolderTree size={16} />
              Quản lý danh mục
            </Button>
            <Button onClick={() => handleOpenForm()}>
              <Plus size={16} />
              Thêm dịch vụ
            </Button>
          </div>
        )}
      </div>

      {!isOwner && (
        <Alert tone="info" className="mb-6">
          Chỉ chủ cơ sở có thể thêm hoặc chỉnh sửa dịch vụ.
        </Alert>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse" aria-busy="true">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <Card key={i}>
              <CardContent className="h-32 bg-[var(--color-neutral-100)] rounded-xl" />
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchServices} />
      )}

      {!isLoading && !error && services.length === 0 && (
        <EmptyState
          icon={<Scissors size={22} />}
          title="Chưa có dịch vụ"
          description={
            isOwner
              ? 'Thêm dịch vụ đầu tiên để khách hàng có thể đặt lịch.'
              : 'Hiện chưa có dịch vụ nào. Vui lòng liên hệ chủ cơ sở để được cập nhật.'
          }
          action={
            isOwner ? (
              <Button onClick={() => handleOpenForm()}>
                <Plus size={16} />
                Thêm dịch vụ
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && !error && services.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {services.map((service) => (
            <Card key={service.id} className="overflow-hidden flex flex-col justify-between border-[#e7e2d8] hover:border-[#c6d8c9] transition-all">
              {service.imageUrl && (
                <div className="h-36 w-full overflow-hidden bg-stone-100">
                  <img
                    src={service.imageUrl}
                    alt={service.name}
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                  />
                </div>
              )}
              <CardContent className="p-4 sm:p-5 flex flex-col h-full grow">
                <div className="flex items-center justify-between gap-2 mb-2">
                  <span className="text-[11px] font-semibold text-[#566f5c] px-2 py-0.5 rounded-full bg-[#f2f6f3] border border-[#c6d8c9]/60">
                    {service.categoryName || 'Chưa phân loại'}
                  </span>
                  <Badge tone={service.isActive ? 'success' : 'neutral'}>
                    {service.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}
                  </Badge>
                </div>

                <div className="flex justify-between items-start gap-2 mb-1.5">
                  <h4 className="font-semibold text-base text-[var(--color-neutral-900)] leading-snug">{service.name}</h4>
                </div>

                {service.description && (
                  <p className="text-xs text-[var(--color-neutral-500)] mb-3 grow line-clamp-2 leading-relaxed">
                    {service.description}
                  </p>
                )}

                {/* Featured Service Control / Indicator */}
                <div className="py-2 border-t border-stone-100 flex items-center justify-between gap-2">
                  <span className="text-[11px] text-stone-500 font-medium">Dịch vụ nổi bật:</span>
                  {isOwner ? (
                    <button
                      type="button"
                      onClick={() => handleToggleFeatured(service)}
                      className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-0.5 rounded-full border transition-colors cursor-pointer ${
                        service.isFeatured
                          ? 'bg-amber-50 text-amber-800 border-amber-200 hover:bg-amber-100'
                          : 'bg-stone-100 text-stone-500 border-stone-200 hover:bg-stone-200'
                      }`}
                      title="Nhấn để bật/tắt hiển thị ở Dịch vụ nổi bật"
                    >
                      <Sparkles size={11} className={service.isFeatured ? 'text-amber-500' : 'text-stone-400'} />
                      <span>{service.isFeatured ? 'Đang nổi bật' : 'Không nổi bật'}</span>
                    </button>
                  ) : (
                    <span className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-0.5 rounded-full border ${
                      service.isFeatured ? 'bg-amber-50 text-amber-800 border-amber-200' : 'bg-stone-100 text-stone-500 border-stone-200'
                    }`}>
                      <Sparkles size={11} className={service.isFeatured ? 'text-amber-500' : 'text-stone-400'} />
                      <span>{service.isFeatured ? 'Nổi bật' : 'Bình thường'}</span>
                    </span>
                  )}
                </div>

                <div className="mt-auto flex items-center justify-between gap-2 pt-3 border-t border-[var(--color-neutral-100)]">
                  <div className="text-sm">
                    <span className="font-semibold text-[var(--color-brand-600)]">{formatCurrency(service.price)}</span>
                    <span className="text-[var(--color-neutral-400)] mx-1.5" aria-hidden="true">·</span>
                    <span className="text-[var(--color-neutral-600)]">{service.durationMinutes} phút</span>
                  </div>

                  {isOwner && (
                    <div className="flex gap-2">
                      <Button variant="secondary" size="sm" onClick={() => handleOpenForm(service)}>
                        Chỉnh sửa
                      </Button>
                      <Button
                        variant="danger-outline"
                        size="sm"
                        onClick={() => handleDeleteClick(service)}
                        aria-label={`Xóa dịch vụ ${service.name}`}
                      >
                        <Trash2 size={16} aria-hidden="true" />
                      </Button>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Category Management Modal */}
      {isCategoryModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs animate-in fade-in duration-200"
        >
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-xl border border-stone-200 relative max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-stone-100">
              <div className="flex items-center gap-2">
                <FolderTree className="w-5 h-5 text-[#465d4c]" />
                <h3 className="font-serif-title font-semibold text-lg text-stone-900">
                  Quản lý Danh mục Dịch vụ
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsCategoryModalOpen(false)}
                className="p-1.5 text-stone-400 hover:text-stone-700 rounded-full hover:bg-stone-100"
              >
                <X size={18} />
              </button>
            </div>

            {categoryError && (
              <Alert tone="error" className="my-3">
                {categoryError}
              </Alert>
            )}

            {/* Add new category form */}
            <form onSubmit={handleCreateCategory} className="py-4 border-b border-stone-100 space-y-2">
              <div className="flex gap-2">
                <input
                  type="text"
                  required
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  placeholder="Tên danh mục mới (VD: Chăm sóc da)..."
                  className="flex-1 px-3 py-2 text-xs rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
                />
                <Button type="submit" size="sm">
                  <Plus size={14} />
                  Thêm
                </Button>
              </div>
              <input
                type="text"
                value={newCatDesc}
                onChange={(e) => setNewCatDesc(e.target.value)}
                placeholder="Mô tả danh mục (không bắt buộc)..."
                className="w-full px-3 py-1.5 text-xs rounded-xl border border-stone-200 bg-stone-50 focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
              />
            </form>

            {/* Categories list */}
            <div className="overflow-y-auto space-y-2 py-3 flex-1">
              {categoryLoading ? (
                <p className="text-xs text-stone-400 text-center py-4">Đang tải danh mục...</p>
              ) : categories.length === 0 ? (
                <p className="text-xs text-stone-400 text-center py-4">Chưa có danh mục nào.</p>
              ) : (
                categories.map((cat) => (
                  <div
                    key={cat.id}
                    className="flex items-center justify-between gap-2 p-3 rounded-2xl bg-stone-50 border border-stone-200/80 text-xs"
                  >
                    {editingCatId === cat.id ? (
                      <div className="flex items-center gap-2 flex-1">
                        <input
                          type="text"
                          value={editCatName}
                          onChange={(e) => setEditCatName(e.target.value)}
                          className="flex-1 px-2.5 py-1 text-xs rounded-lg border border-stone-300 bg-white"
                        />
                        <button
                          type="button"
                          onClick={() => handleSaveEditCategory(cat.id)}
                          className="p-1 text-emerald-700 hover:bg-emerald-50 rounded"
                          title="Lưu"
                        >
                          <CheckCircle2 size={16} />
                        </button>
                        <button
                          type="button"
                          onClick={() => setEditingCatId(null)}
                          className="p-1 text-stone-400 hover:bg-stone-200 rounded"
                          title="Hủy"
                        >
                          <X size={16} />
                        </button>
                      </div>
                    ) : (
                      <>
                        <div className="min-w-0">
                          <p className="font-semibold text-stone-800">{cat.name}</p>
                          {cat.description && (
                            <p className="text-[11px] text-stone-500 line-clamp-1">{cat.description}</p>
                          )}
                        </div>

                        <div className="flex items-center gap-1.5 shrink-0">
                          <button
                            type="button"
                            onClick={() => handleToggleCategoryActive(cat)}
                            className={`px-2 py-0.5 rounded text-[10px] font-semibold border ${
                              cat.isActive
                                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                                : 'bg-stone-100 text-stone-500 border-stone-200'
                            }`}
                          >
                            {cat.isActive ? 'Hoạt động' : 'Tạm ẩn'}
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setEditingCatId(cat.id);
                              setEditCatName(cat.name);
                            }}
                            className="p-1 text-stone-500 hover:text-stone-800 rounded"
                            title="Sửa tên"
                          >
                            <Edit2 size={13} />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteCategory(cat.id)}
                            className="p-1 text-rose-500 hover:text-rose-700 rounded"
                            title="Xóa danh mục"
                          >
                            <Trash2 size={13} />
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                ))
              )}
            </div>

            <div className="pt-3 border-t border-stone-100 flex justify-end">
              <Button variant="secondary" size="sm" onClick={() => setIsCategoryModalOpen(false)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={deletingService !== null}
        title="Xóa dịch vụ?"
        description={
          <>
            Dịch vụ <strong>{deletingService?.name}</strong> sẽ bị xóa vĩnh viễn. Thao tác này không
            thể hoàn tác và sẽ thất bại nếu dịch vụ đang được dùng trong lịch hẹn.
          </>
        }
        confirmLabel="Xóa dịch vụ"
        cancelLabel="Giữ lại"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingService(null)}
        busy={isDeleting}
        error={deleteError}
      />
    </AppShell>
  );
};

export default Services;
