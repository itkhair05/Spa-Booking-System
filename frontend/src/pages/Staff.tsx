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
import { getStaff, deleteStaff, updateStaff, createStaffAccount } from '../lib/api/staff';
import type { Staff as StaffType } from '../types/staff';
import { StaffForm } from './StaffForm';
import { UserRound, Trash2, Plus, KeyRound, ShieldAlert, Check, Phone, Mail } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const Staff = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER') ?? false;

  const [staffList, setStaffList] = useState<StaffType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state (add / edit staff profile)
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffType | undefined>(undefined);

  // Account creation state
  const [accountTarget, setAccountTarget] = useState<StaffType | null>(null);
  const [accountUsername, setAccountUsername] = useState('');
  const [accountPassword, setAccountPassword] = useState('');
  const [isCreatingAccount, setIsCreatingAccount] = useState(false);
  const [accountError, setAccountError] = useState<string | null>(null);
  const [accountSuccess, setAccountSuccess] = useState<string | null>(null);

  // Toggle active confirmation
  const [toggleTarget, setToggleTarget] = useState<StaffType | null>(null);
  const [isToggling, setIsToggling] = useState(false);

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

  // Staff Account creation
  const handleOpenAccountModal = (member: StaffType) => {
    setAccountTarget(member);
    setAccountUsername(member.email || '');
    setAccountPassword('');
    setAccountError(null);
  };

  const handleCreateAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountTarget || !accountUsername.trim() || !accountPassword) return;

    setIsCreatingAccount(true);
    setAccountError(null);
    try {
      await createStaffAccount(accountTarget.id, {
        username: accountUsername.trim(),
        password: accountPassword,
      });
      setAccountTarget(null);
      setAccountSuccess(`Đã tạo tài khoản thành công cho nhân viên ${accountTarget.name}.`);
      setTimeout(() => setAccountSuccess(null), 4000);
      await fetchStaff();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      if (axiosErr.response?.status === 409) {
        setAccountError('Tên đăng nhập này đã tồn tại trong hệ thống. Vui lòng chọn tên khác.');
      } else {
        setAccountError(axiosErr.response?.data?.message || 'Không thể tạo tài khoản cho nhân viên.');
      }
    } finally {
      setIsCreatingAccount(false);
    }
  };

  // Toggle active/inactive
  const handleConfirmToggleActive = async () => {
    if (!toggleTarget) return;
    setIsToggling(true);
    try {
      await updateStaff(toggleTarget.id, {
        name: toggleTarget.name,
        phone: toggleTarget.phone || undefined,
        email: toggleTarget.email || undefined,
        isActive: !toggleTarget.isActive,
      });
      setToggleTarget(null);
      await fetchStaff();
    } catch {
      // Ignore
    } finally {
      setIsToggling(false);
    }
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
        <PageHeader title={editingStaff ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên mới'} />
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
          title="Đội ngũ Nhân viên TIKEY SPA"
          description="Quản lý thông tin kỹ thuật viên, trạng thái hoạt động và tài khoản đăng nhập."
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
          Chỉ chủ cơ sở có thể thêm, chỉnh sửa hoặc cấp tài khoản nhân viên.
        </Alert>
      )}

      {accountSuccess && (
        <Alert tone="success" className="mb-6">
          {accountSuccess}
        </Alert>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse" aria-busy="true">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <Card key={i}>
              <CardContent className="h-32 bg-stone-100 rounded-xl" />
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
            <Card key={member.id} className="border-[#e7e2d8] hover:border-[#c6d8c9] transition-all flex flex-col justify-between">
              <CardContent className="p-5 flex flex-col h-full">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-12 h-12 rounded-full overflow-hidden border border-[#c6d8c9] bg-stone-100 shrink-0 flex items-center justify-center">
                    {member.avatarUrl ? (
                      <img
                        src={member.avatarUrl}
                        alt={member.name}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center bg-[#f2f6f3] text-[#465d4c] font-semibold text-sm">
                        {member.name.charAt(0).toUpperCase()}
                      </div>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex justify-between items-start gap-1">
                      <h4 className="font-semibold text-base text-stone-900 truncate">{member.name}</h4>
                      <Badge tone={member.isActive ? 'success' : 'neutral'}>
                        {member.isActive ? 'Hoạt động' : 'Đã vô hiệu hóa'}
                      </Badge>
                    </div>
                    <span className="text-xs text-stone-500">Kỹ thuật viên</span>
                  </div>
                </div>

                <div className="text-xs text-stone-600 space-y-2 mb-4 grow">
                  {member.phone && (
                    <p className="flex items-center gap-1.5">
                      <Phone size={13} className="text-stone-400" />
                      <span>{member.phone}</span>
                    </p>
                  )}
                  {member.email && (
                    <p className="flex items-center gap-1.5 break-all">
                      <Mail size={13} className="text-stone-400" />
                      <span>{member.email}</span>
                    </p>
                  )}

                  {/* Account Status Badge */}
                  <div className="pt-2 border-t border-stone-100 flex items-center justify-between gap-2">
                    <span className="text-[11px] font-medium text-stone-500">Tài khoản đăng nhập:</span>
                    {member.username ? (
                      member.accountEnabled === false ? (
                        <span
                          className="inline-flex items-center gap-1 text-[11px] font-medium text-stone-500 bg-stone-100 border border-stone-200 px-2 py-0.5 rounded-full"
                          title={`${member.username} — Đã vô hiệu hóa`}
                        >
                          <span className="max-w-[110px] truncate">{member.username}</span>
                          <span className="text-stone-400">— Đã vô hiệu hóa</span>
                        </span>
                      ) : (
                        <span
                          className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#465d4c] bg-[#edf7f2] border border-[#b7e4c7] px-2 py-0.5 rounded-full"
                          title={member.username}
                        >
                          <Check size={11} />
                          <span className="max-w-[150px] truncate">{member.username}</span>
                        </span>
                      )
                    ) : (
                      <span className="text-[11px] text-stone-400">Chưa cấp</span>
                    )}
                  </div>
                </div>

                {isOwner && (
                  <div className="mt-auto flex flex-col gap-2 pt-3 border-t border-stone-100">
                    {!member.username && (
                      <Button
                        variant="secondary"
                        size="sm"
                        className="w-full text-xs justify-center"
                        onClick={() => handleOpenAccountModal(member)}
                      >
                        <KeyRound size={13} className="mr-1.5 text-stone-500" />
                        Cấp tài khoản đăng nhập
                      </Button>
                    )}

                    <div className="flex items-center justify-between gap-2">
                      <button
                        type="button"
                        onClick={() => setToggleTarget(member)}
                        className={`text-xs px-2.5 py-1.5 rounded-lg border font-medium transition-colors ${
                          member.isActive
                            ? 'border-stone-200 text-stone-600 hover:bg-stone-50'
                            : 'border-emerald-200 text-emerald-800 bg-emerald-50 hover:bg-emerald-100'
                        }`}
                      >
                        {member.isActive ? 'Vô hiệu hóa' : 'Kích hoạt lại'}
                      </button>

                      <div className="flex items-center gap-1.5">
                        <Button variant="secondary" size="sm" onClick={() => handleOpenForm(member)}>
                          Sửa
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleDeleteClick(member)}
                          aria-label={`Xóa nhân viên ${member.name}`}
                        >
                          <Trash2 size={14} aria-hidden="true" />
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create Staff Account Modal */}
      {accountTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-md w-full p-6 shadow-xl animate-in fade-in zoom-in-95 duration-200">
            <h3 className="font-serif-title font-semibold text-lg text-stone-900 mb-1">
              Cấp tài khoản đăng nhập cho nhân viên
            </h3>
            <p className="text-xs text-stone-500 mb-4">
              Nhân viên <strong>{accountTarget.name}</strong> sẽ dùng tài khoản này để đăng nhập vào Cổng nhân viên TIKEY SPA.
            </p>

            {accountError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs mb-4 flex items-start gap-2">
                <ShieldAlert size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>{accountError}</span>
              </div>
            )}

            <form onSubmit={handleCreateAccount} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                  Tên đăng nhập / Email
                </label>
                <input
                  type="text"
                  required
                  value={accountUsername}
                  onChange={(e) => setAccountUsername(e.target.value)}
                  placeholder="VD: staff@tikey.local hoặc hoa@gmail.com"
                  className="w-full h-11 px-3 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c]"
                  disabled={isCreatingAccount}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                  Mật khẩu khởi tạo
                </label>
                <input
                  type="password"
                  required
                  value={accountPassword}
                  onChange={(e) => setAccountPassword(e.target.value)}
                  placeholder="Mật khẩu tối thiểu 6 ký tự"
                  minLength={6}
                  className="w-full h-11 px-3 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c]"
                  disabled={isCreatingAccount}
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-stone-100">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setAccountTarget(null)}
                  disabled={isCreatingAccount}
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  disabled={isCreatingAccount || !accountUsername.trim() || !accountPassword}
                >
                  {isCreatingAccount ? 'Đang tạo...' : 'Tạo tài khoản'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Toggle Active Confirmation Dialog (Vô hiệu hóa / Kích hoạt lại) */}
      <ConfirmDialog
        open={toggleTarget !== null}
        title={toggleTarget?.isActive ? 'Vô hiệu hóa nhân viên?' : 'Kích hoạt lại nhân viên?'}
        description={
          toggleTarget && (
            <>
              {toggleTarget.isActive ? (
                <span>
                  Nhân viên <strong>{toggleTarget.name}</strong> sẽ được chuyển sang trạng thái <strong>Đã vô hiệu hóa</strong>.
                  Tài khoản đăng nhập liên kết của nhân viên này sẽ bị tạm khóa và không thể nhận thêm lịch hẹn mới.
                  Toàn bộ lịch sử đặt hẹn trước đây vẫn được bảo lưu và bạn có thể kích hoạt lại bất kỳ lúc nào.
                </span>
              ) : (
                <span>
                  Kích hoạt lại nhân viên <strong>{toggleTarget.name}</strong> để họ có thể tiếp tục đăng nhập
                  và nhận lịch hẹn từ khách hàng.
                </span>
              )}
            </>
          )
        }
        confirmLabel={toggleTarget?.isActive ? 'Vô hiệu hóa' : 'Kích hoạt'}
        cancelLabel="Đóng"
        tone="primary"
        onConfirm={handleConfirmToggleActive}
        onCancel={() => setToggleTarget(null)}
        busy={isToggling}
      />

      {/* Delete / Archive Staff Confirmation Dialog (Xóa) */}
      <ConfirmDialog
        open={deletingMember !== null}
        title="Xóa nhân viên?"
        description={
          <>
            Nhân viên <strong>{deletingMember?.name}</strong> sẽ bị xóa khỏi danh sách quản lý nhân viên.
            Toàn bộ lịch sử đặt hẹn và dữ liệu doanh thu trước đây vẫn được lưu trữ nguyên vẹn để đối soát.
            Hành động này không thể hoàn tác.
          </>
        }
        confirmLabel="Xóa nhân viên"
        cancelLabel="Đóng"
        tone="danger"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingMember(null)}
        busy={isDeleting}
        error={deleteError}
      />
    </AppShell>
  );
};

export default Staff;
