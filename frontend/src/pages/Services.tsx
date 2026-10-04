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
import { getServices, deleteService } from '../lib/api/services';
import { formatCurrency } from '../lib/format';
import type { Service } from '../types/service';
import { ServiceForm } from './ServiceForm';
import { Scissors, Trash2, Plus } from 'lucide-react';
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
          title="Quản lý Dịch vụ"
          description="Danh mục dịch vụ và liệu trình tại spa."
        />
        {isOwner && (
          <Button onClick={() => handleOpenForm()}>
            <Plus size={16} />
            Thêm dịch vụ
          </Button>
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
              <CardContent className="h-32 bg-[var(--color-neutral-100)] rounded-xl">
                <div />
              </CardContent>
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
            <Card key={service.id}>
              <CardContent className="p-4 sm:p-5 flex flex-col h-full">
                <div className="flex justify-between items-start gap-2 mb-2">
                  <h4 className="font-semibold text-base text-[var(--color-neutral-900)]">{service.name}</h4>
                  <Badge tone={service.isActive ? 'success' : 'neutral'}>
                    {service.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}
                  </Badge>
                </div>

                {service.description && (
                  <p className="text-sm text-[var(--color-neutral-500)] mb-4 grow line-clamp-2">
                    {service.description}
                  </p>
                )}

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
