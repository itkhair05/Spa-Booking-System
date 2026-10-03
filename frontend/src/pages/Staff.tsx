import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { getStaff, deleteStaff } from '../lib/api/staff';
import type { Staff as StaffType } from '../types/staff';
import { StaffForm } from './StaffForm';
import { AlertCircle, UserRound, Trash2 } from 'lucide-react';
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
  const [deletingId, setDeletingId] = useState<number | null>(null);

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

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && deletingId !== null) {
        setDeletingId(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [deletingId]);

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

  const handleDeleteClick = (id: number) => {
    if (!isOwner) return;
    setDeletingId(id);
  };

  const confirmDelete = async () => {
    if (!deletingId) return;
    try {
      await deleteStaff(deletingId);
      setDeletingId(null);
      fetchStaff();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      alert(errorObj.response?.data?.message || 'Không thể xóa nhân viên.');
      setDeletingId(null);
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
        <PageHeader title="Quản lý Nhân viên" description="Quản lý đội ngũ nhân viên của bạn." />
        {isOwner && (
          <Button onClick={() => handleOpenForm()}>Thêm nhân viên</Button>
        )}
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse" aria-busy="true">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-32 bg-[var(--color-neutral-100)] rounded-xl">
                <div />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)]" role="alert">
          <AlertCircle className="text-[var(--color-error)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Không thể tải dữ liệu</h3>
          <p className="text-[var(--color-neutral-500)] mb-6 text-center max-w-md">{error}</p>
          <Button onClick={fetchStaff}>Thử lại</Button>
        </div>
      )}

      {!isLoading && !error && staffList.length === 0 && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)] text-center">
          <UserRound className="text-[var(--color-neutral-400)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Chưa có nhân viên</h3>
          <p className="text-[var(--color-neutral-500)] mb-6">
            {isOwner ? "Thêm nhân viên đầu tiên." : "Hiện chưa có nhân viên nào."}
          </p>
          {isOwner && (
            <Button onClick={() => handleOpenForm()}>Thêm nhân viên</Button>
          )}
        </div>
      )}

      {!isLoading && !error && staffList.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {staffList.map((member) => (
            <Card key={member.id}>
              <CardContent className="p-4 sm:p-6 flex flex-col h-full relative group">
                <div className="flex justify-between items-start mb-2">
                  <h4 className="font-semibold text-lg text-[var(--color-neutral-900)]">{member.name}</h4>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${member.isActive ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-50 text-gray-600 border-gray-200'}`}>
                    {member.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}
                  </span>
                </div>
                
                <div className="text-sm text-[var(--color-neutral-600)] space-y-1 mb-4 flex-grow">
                  {member.email && (
                    <p>
                      <strong>Email:</strong> {member.email}
                    </p>
                  )}
                  {member.phone && (
                    <p>
                      <strong>Số điện thoại:</strong> {member.phone}
                    </p>
                  )}
                </div>
                
                <div className="mt-auto flex items-center justify-end pt-4 border-t border-[var(--color-neutral-100)]">
                  {isOwner && (
                    <div className="flex gap-2 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
                      <Button variant="secondary" size="sm" onClick={() => handleOpenForm(member)}>
                        Chỉnh sửa
                      </Button>
                      <Button variant="danger" size="sm" onClick={() => handleDeleteClick(member.id)} aria-label="Xóa nhân viên">
                        <Trash2 size={16} />
                      </Button>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Delete Confirmation UI */}
      {deletingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog" aria-modal="true" aria-labelledby="delete-dialog-title">
          <div className="bg-white rounded-xl shadow-xl p-6 max-w-sm w-full animate-in fade-in zoom-in duration-200">
            <h3 id="delete-dialog-title" className="text-lg font-bold text-[var(--color-neutral-900)] mb-2">Xóa nhân viên?</h3>
            <p className="text-sm text-[var(--color-neutral-500)] mb-6">
              Bạn có chắc chắn muốn xóa nhân viên này không? Hành động này không thể hoàn tác và có thể thất bại nếu nhân viên đã có lịch hẹn.
            </p>
            <div className="flex justify-end gap-3">
              <Button variant="secondary" onClick={() => setDeletingId(null)}>Hủy</Button>
              <Button variant="danger" onClick={confirmDelete}>Xóa</Button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
};

export default Staff;
