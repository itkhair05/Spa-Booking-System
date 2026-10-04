import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getCustomers, deleteCustomer } from '../lib/api/customers';
import { formatDateDMY } from '../lib/format';
import type { Customer } from '../types/customer';
import { CustomerForm } from './CustomerForm';
import { Users, Trash2, Plus } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const Customers = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | undefined>(undefined);

  // Delete state
  const [deletingCustomer, setDeletingCustomer] = useState<Customer | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const fetchCustomers = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getCustomers();
      setCustomers(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Không thể tải dữ liệu khách hàng.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOwner) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchCustomers();
    }
  }, [fetchCustomers, isOwner]);

  if (!isOwner) {
    return (
      <AppShell title="Khách hàng">
        <div className="max-w-2xl mx-auto py-16 text-center px-4">
          <div className="w-14 h-14 rounded-full bg-amber-50 text-amber-700 flex items-center justify-center mx-auto mb-4 border border-amber-200">
            <Users className="w-7 h-7" />
          </div>
          <h2 className="text-xl font-serif-title font-semibold text-stone-900 mb-2">Quyền truy cập dành cho Chủ cơ sở</h2>
          <p className="text-stone-600 text-sm leading-relaxed mb-6">
            Khu vực quản lý danh bạ khách hàng toàn cơ sở chỉ dành cho vai trò Quản trị viên (OWNER).
            Nhân viên (STAFF) có thể xem thông tin liên hệ của khách hàng trực tiếp trên từng lịch hẹn được phân công.
          </p>
        </div>
      </AppShell>
    );
  }

  const handleOpenForm = (customer?: Customer) => {
    setEditingCustomer(customer);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingCustomer(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchCustomers();
  };

  const handleDeleteClick = (customer: Customer) => {
    if (!isOwner) return;
    setDeleteError(null);
    setDeletingCustomer(customer);
  };

  const handleConfirmDelete = async () => {
    if (!deletingCustomer) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteCustomer(deletingCustomer.id);
      setDeletingCustomer(null);
      fetchCustomers();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setDeleteError(errorObj.response?.data?.message || 'Không thể xóa khách hàng.');
    } finally {
      setIsDeleting(false);
    }
  };

  if (isFormOpen) {
    return (
      <AppShell title="Khách hàng">
        <PageHeader title="Quản lý Khách hàng" />
        <div className="max-w-2xl mx-auto">
          <CustomerForm
            customer={editingCustomer}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Khách hàng">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader
          title="Quản lý Khách hàng"
          description="Danh sách khách hàng đã từng sử dụng dịch vụ tại spa."
        />
        <Button onClick={() => handleOpenForm()}>
          <Plus size={16} />
          Thêm khách hàng
        </Button>
      </div>

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
        <ErrorState message={error} onRetry={fetchCustomers} />
      )}

      {!isLoading && !error && customers.length === 0 && (
        <EmptyState
          icon={<Users size={22} />}
          title="Chưa có khách hàng"
          description="Thêm khách hàng đầu tiên để bắt đầu tạo lịch hẹn cho họ."
          action={
            <Button onClick={() => handleOpenForm()}>
              <Plus size={16} />
              Thêm khách hàng
            </Button>
          }
        />
      )}

      {!isLoading && !error && customers.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {customers.map((customer) => (
            <Card key={customer.id}>
              <CardContent className="p-4 sm:p-5 flex flex-col h-full">
                <div className="flex justify-between items-start gap-2 mb-2">
                  <h4 className="font-semibold text-base text-[var(--color-neutral-900)]">{customer.name}</h4>
                  {customer.isActive !== undefined && (
                    <Badge tone={customer.isActive ? 'success' : 'neutral'}>
                      {customer.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}
                    </Badge>
                  )}
                </div>

                <div className="text-sm text-[var(--color-neutral-600)] space-y-1 mb-4 grow">
                  {customer.phone && (
                    <p>
                      <span className="text-[var(--color-neutral-500)]">Số điện thoại: </span>
                      {customer.phone}
                    </p>
                  )}
                  {customer.email && (
                    <p className="break-all">
                      <span className="text-[var(--color-neutral-500)]">Email: </span>
                      {customer.email}
                    </p>
                  )}
                  {customer.lastVisit && (
                    <p>
                      <span className="text-[var(--color-neutral-500)]">Lần ghé gần nhất: </span>
                      {formatDateDMY(customer.lastVisit)}
                    </p>
                  )}
                </div>

                <div className="mt-auto flex items-center justify-end gap-2 pt-3 border-t border-[var(--color-neutral-100)]">
                  <Button variant="secondary" size="sm" onClick={() => handleOpenForm(customer)}>
                    Chỉnh sửa
                  </Button>
                  {isOwner && (
                    <Button
                      variant="danger-outline"
                      size="sm"
                      onClick={() => handleDeleteClick(customer)}
                      aria-label={`Xóa khách hàng ${customer.name}`}
                    >
                      <Trash2 size={16} aria-hidden="true" />
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={deletingCustomer !== null}
        title="Xóa khách hàng?"
        description={
          <>
            Khách hàng <strong>{deletingCustomer?.name}</strong> sẽ bị xóa vĩnh viễn. Thao tác này
            không thể hoàn tác và sẽ thất bại nếu khách hàng vẫn còn lịch hẹn liên quan.
          </>
        }
        confirmLabel="Xóa khách hàng"
        cancelLabel="Giữ lại"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingCustomer(null)}
        busy={isDeleting}
        error={deleteError}
      />
    </AppShell>
  );
};

export default Customers;
