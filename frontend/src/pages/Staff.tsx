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
import { getStaff, deleteStaff } from '../lib/api/staff';
import type { Staff as StaffType } from '../types/staff';
import { StaffForm } from './StaffForm';
import { UserRound, Trash2, Plus } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const Staff = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [staffList, setStaffList] = useState<StaffType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffType | undefined>(undefined);

  // Delete state
  const [deletingMember, setDeletingMember] = useState<StaffType | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const fetchStaff = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getStaff();
      setStaffList(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Không thể tải dữ liệu nhân viên.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchStaff();
  }, [fetchStaff]);

  const handleOpenForm = (staff?: StaffType) => {
    if (!isOwner) return;
    setEditingStaff(staff);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingStaff(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchStaff();
  };

  const handleDeleteClick = (member: StaffType) => {
    if (!isOwner) return;
    setDeleteError(null);
    setDeletingMember(member);
  };

  const handleConfirmDelete = async () => {
    if (!deletingMember) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteStaff(deletingMember.id);
      setDeletingMember(null);
      fetchStaff();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setDeleteError(errorObj.response?.data?.message || 'Không thể xóa nhân viên.');
    } finally {
      setIsDeleting(false);
    }
  };

  if (isFormOpen && isOwner) {
    return (
      <AppShell title="Nhân viên">
        <PageHeader title="Quản lý Nhân viên" />
        <div className="max-w-2xl mx-auto">
          <StaffForm
            staff={editingStaff}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Nhân viên">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader
          title="Quản lý Nhân viên"
          description="Đội ngũ nhân viên đang làm việc tại spa."
        />
        {isOwner && (
          <Button onClick={() => handleOpenForm()}>
            <Plus size={16} />
            Thêm nhân viên
          </Button>
        )}
      </div>

      {!isOwner && (
        <Alert tone="info" className="mb-6">
          Chỉ chủ cơ sở có thể thêm hoặc chỉnh sửa nhân viên.
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
        <ErrorState message={error} onRetry={fetchStaff} />
      )}

      {!isLoading && !error && staffList.length === 0 && (
        <EmptyState
          icon={<UserRound size={22} />}
          title="Chưa có nhân viên"
          description={
            isOwner
              ? 'Thêm nhân viên đầu tiên để phân công lịch hẹn.'
              : 'Hiện chưa có nhân viên nào. Vui lòng liên hệ chủ cơ sở để được cập nhật.'
          }
          action={
            isOwner ? (
              <Button onClick={() => handleOpenForm()}>
                <Plus size={16} />
                Thêm nhân viên
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && !error && staffList.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {staffList.map((member) => (
            <Card key={member.id}>
              <CardContent className="p-4 sm:p-5 flex flex-col h-full">
                <div className="flex justify-between items-start gap-2 mb-2">
                  <h4 className="font-semibold text-base text-[var(--color-neutral-900)]">{member.name}</h4>
                  <Badge tone={member.isActive ? 'success' : 'neutral'}>
                    {member.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}
                  </Badge>
                </div>

                <div className="text-sm text-[var(--color-neutral-600)] space-y-1 mb-4 grow">
                  {member.phone && (
                    <p>
                      <span className="text-[var(--color-neutral-500)]">Số điện thoại: </span>
                      {member.phone}
                    </p>
                  )}
                  {member.email && (
                    <p className="break-all">
                      <span className="text-[var(--color-neutral-500)]">Email: </span>
                      {member.email}
                    </p>
                  )}
                </div>

                {isOwner && (
                  <div className="mt-auto flex items-center justify-end gap-2 pt-3 border-t border-[var(--color-neutral-100)]">
                    <Button variant="secondary" size="sm" onClick={() => handleOpenForm(member)}>
                      Chỉnh sửa
                    </Button>
                    <Button
                      variant="danger-outline"
                      size="sm"
                      onClick={() => handleDeleteClick(member)}
                      aria-label={`Xóa nhân viên ${member.name}`}
                    >
                      <Trash2 size={16} aria-hidden="true" />
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={deletingMember !== null}
        title="Xóa nhân viên?"
        description={
          <>
            Nhân viên <strong>{deletingMember?.name}</strong> sẽ bị xóa vĩnh viễn. Thao tác này
            không thể hoàn tác và sẽ thất bại nếu nhân viên vẫn còn lịch hẹn liên quan.
          </>
        }
        confirmLabel="Xóa nhân viên"
        cancelLabel="Giữ lại"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingMember(null)}
        busy={isDeleting}
        error={deleteError}
      />
    </AppShell>
  );
};

export default Staff;
