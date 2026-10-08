import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { useAuth } from '../app/auth/useAuth';
import { getBusinessProfile, updateBusinessProfile } from '../lib/api/businessProfile';
import { getMyProfile, updateMyProfile, uploadMyAvatar, deleteMyAvatar } from '../lib/api/staff';
import { changePassword } from '../lib/api/auth';
import { resolveMediaUrl } from '../lib/api/apiConfig';
import type { BusinessProfileResponse } from '../types/businessProfile';
import type { Staff } from '../types/staff';
import {
  LogOut,
  UserRound,
  Building2,
  ExternalLink,
  Save,
  Loader2,
  KeyRound,
  Camera,
  Trash2,
  Upload,
  CheckCircle2,
  ShieldCheck
} from 'lucide-react';

const formatRole = (role: string) => {
  if (role === 'ROLE_OWNER') return 'Chủ cơ sở';
  if (role === 'ROLE_STAFF') return 'Nhân viên';
  return role
    .replace(/^ROLE_/, '')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
};

const Settings = () => {
  const { user, logout, updateUser } = useAuth();
  const navigate = useNavigate();
  const isOwner = user?.roles?.includes('ROLE_OWNER') ?? false;

  // Business profile state (OWNER)
  const [profile, setProfile] = useState<BusinessProfileResponse | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);

  // Profile form inputs (OWNER)
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');

  // Staff Avatar state (STAFF)
  const [staffProfile, setStaffProfile] = useState<Staff | null>(null);
  const [avatarLoading, setAvatarLoading] = useState(false);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [avatarMessage, setAvatarMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Staff Self-Profile form inputs (STAFF)
  const [staffName, setStaffName] = useState('');
  const [staffPhone, setStaffPhone] = useState('');
  const [staffEmail, setStaffEmail] = useState('');
  const [staffSaving, setStaffSaving] = useState(false);
  const [staffSuccess, setStaffSuccess] = useState<string | null>(null);
  const [staffError, setStaffError] = useState<string | null>(null);

  // Password change state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  useEffect(() => {
    if (isOwner) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setProfileLoading(true);
      getBusinessProfile()
        .then((data) => {
          setProfile(data);
          setName(data.name || '');
          setPhone(data.phone || '');
          setEmail(data.email || '');
          setAddress(data.address || '');
        })
        .catch(() => {
          setProfileError('Không thể tải thông tin hồ sơ cơ sở.');
        })
        .finally(() => {
          setProfileLoading(false);
        });
    } else {
      // Fetch STAFF's own profile and avatar
      getMyProfile()
        .then((data) => {
          setStaffProfile(data);
          setStaffName(data.name || '');
          setStaffPhone(data.phone || '');
          setStaffEmail(data.email || data.username || '');
        })
        .catch(() => {
          // If staff profile fetch fails, handle gracefully
        });
    }
  }, [isOwner]);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setProfileSaving(true);
    setProfileSuccess(null);
    setProfileError(null);

    try {
      const updated = await updateBusinessProfile({
        name: name.trim(),
        phone: phone.trim() || undefined,
        email: email.trim() || undefined,
        address: address.trim() || undefined,
      });
      setProfile(updated);
      setProfileSuccess('Đã cập nhật thông tin hồ sơ TIKEY SPA thành công.');
      setTimeout(() => setProfileSuccess(null), 3500);
    } catch {
      setProfileError('Đã xảy ra lỗi khi cập nhật hồ sơ. Vui lòng thử lại sau.');
    } finally {
      setProfileSaving(false);
    }
  };

  const handleAvatarFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setAvatarMessage({ type: 'error', text: 'Chỉ chấp nhận ảnh định dạng JPG, PNG hoặc WEBP.' });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setAvatarMessage({ type: 'error', text: 'Kích thước file ảnh không được vượt quá 5MB.' });
      return;
    }
    setAvatarFile(file);
    setAvatarPreview(URL.createObjectURL(file));
    setAvatarMessage(null);
  };

  const handleSaveAvatar = async () => {
    if (!avatarFile) return;
    setAvatarLoading(true);
    setAvatarMessage(null);
    try {
      const updated = await uploadMyAvatar(avatarFile);
      setStaffProfile(updated);
      setAvatarFile(null);
      setAvatarPreview(null);
      setAvatarMessage({ type: 'success', text: 'Cập nhật ảnh đại diện thành công.' });
      setTimeout(() => setAvatarMessage(null), 3500);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setAvatarMessage({ type: 'error', text: errorObj.response?.data?.message || 'Không thể tải lên ảnh đại diện.' });
    } finally {
      setAvatarLoading(false);
    }
  };

  const handleDeleteAvatar = async () => {
    setAvatarLoading(true);
    setAvatarMessage(null);
    try {
      await deleteMyAvatar();
      setStaffProfile((prev) => (prev ? { ...prev, avatarUrl: null } : null));
      setAvatarFile(null);
      setAvatarPreview(null);
      setAvatarMessage({ type: 'success', text: 'Đã xóa ảnh đại diện.' });
      setTimeout(() => setAvatarMessage(null), 3500);
    } catch {
      setAvatarMessage({ type: 'error', text: 'Không thể xóa ảnh đại diện.' });
    } finally {
      setAvatarLoading(false);
    }
  };

  const handleSaveStaffProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!staffName.trim()) {
      setStaffError('Vui lòng nhập họ và tên.');
      return;
    }
    if (!staffEmail.trim()) {
      setStaffError('Vui lòng nhập email đăng nhập.');
      return;
    }

    setStaffSaving(true);
    setStaffSuccess(null);
    setStaffError(null);

    try {
      const updated = await updateMyProfile({
        name: staffName.trim(),
        phone: staffPhone.trim() || undefined,
        email: staffEmail.trim() || undefined,
      });
      setStaffProfile(updated);
      if (updated.username && updateUser && user) {
        updateUser(
          { username: updated.username, roles: user.roles },
          updated.accessToken || undefined
        );
      }
      setStaffSuccess('Đã cập nhật thông tin hồ sơ cá nhân thành công.');
      setTimeout(() => setStaffSuccess(null), 3500);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setStaffError(errorObj.response?.data?.message || 'Không thể cập nhật hồ sơ cá nhân.');
    } finally {
      setStaffSaving(false);
    }
  };


  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordSuccess(null);
    setPasswordError(null);

    if (!currentPassword) {
      setPasswordError('Vui lòng nhập mật khẩu hiện tại.');
      return;
    }
    if (newPassword.length < 6) {
      setPasswordError('Mật khẩu mới phải có ít nhất 6 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Mật khẩu xác nhận không khớp với mật khẩu mới.');
      return;
    }

    setPasswordLoading(true);
    try {
      await changePassword({ currentPassword, newPassword, confirmPassword });
      setPasswordSuccess('Đổi mật khẩu thành công! Mật khẩu mới đã được cập nhật.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => setPasswordSuccess(null), 4000);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setPasswordError(errorObj.response?.data?.message || 'Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.');
    } finally {
      setPasswordLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <AppShell title={isOwner ? 'Hồ sơ & Cài đặt' : 'Cài đặt tài khoản'}>
      <PageHeader
        title={isOwner ? 'Hồ sơ & Cài đặt TIKEY SPA' : 'Thông tin tài khoản cá nhân'}
        description={
          isOwner
            ? 'Quản lý thông tin hiển thị của spa trên trang đặt lịch công khai và phiên làm việc.'
            : 'Thông tin tài khoản kỹ thuật viên, ảnh đại diện và bảo mật cá nhân.'
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Primary Brand Profile (OWNER) or Staff Avatar & Details (STAFF) */}
        <div className="lg:col-span-7 space-y-6">
          {/* OWNER ONLY: Business Profile Management */}
          {isOwner && (
            <Card className="border-[#e7e2d8] shadow-xs">
              <CardContent className="p-6">
                <div className="flex items-center justify-between gap-3 mb-5 pb-4 border-b border-stone-100">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 bg-[#f2f6f3] text-[#465d4c] rounded-xl border border-[#c6d8c9]" aria-hidden="true">
                      <Building2 size={20} />
                    </div>
                    <div>
                      <h3 className="font-serif-title font-semibold text-lg text-stone-900">
                        Hồ sơ thương hiệu TIKEY SPA
                      </h3>
                      <p className="text-xs text-stone-500">
                        Thông tin hiển thị công khai cho khách hàng khi đặt lịch trực tuyến.
                      </p>
                    </div>
                  </div>

                  {profile?.slug && (
                    <a
                      href={`/spas/${profile.slug}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="hidden sm:inline-flex items-center gap-1.5 text-xs text-[#465d4c] hover:underline font-medium"
                    >
                      <span>Xem website</span>
                      <ExternalLink size={13} />
                    </a>
                  )}
                </div>

                {profileSuccess && (
                  <Alert tone="success" className="mb-4">
                    {profileSuccess}
                  </Alert>
                )}

                {profileError && (
                  <Alert tone="error" className="mb-4">
                    {profileError}
                  </Alert>
                )}

                {profileLoading ? (
                  <div className="py-8 text-center text-sm text-stone-500 animate-pulse">
                    Đang tải thông tin hồ sơ...
                  </div>
                ) : (
                  <form onSubmit={handleSaveProfile} className="space-y-4">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                        Tên Spa / Cơ sở
                      </label>
                      <input
                        type="text"
                        required
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="TIKEY SPA"
                        className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                        disabled={profileSaving}
                      />
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                          Số điện thoại Hotline
                        </label>
                        <input
                          type="text"
                          value={phone}
                          onChange={(e) => setPhone(e.target.value)}
                          placeholder="VD: 0908888777"
                          className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                          disabled={profileSaving}
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                          Email liên hệ
                        </label>
                        <input
                          type="email"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          placeholder="contact@tikeyspa.local"
                          className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                          disabled={profileSaving}
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                        Địa chỉ cơ sở
                      </label>
                      <input
                        type="text"
                        value={address}
                        onChange={(e) => setAddress(e.target.value)}
                        placeholder="VD: 789 Bến Vân Đồn, Quận 4, TP. Hồ Chí Minh"
                        className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                        disabled={profileSaving}
                      />
                    </div>

                    {profile && (
                      <div className="pt-2 text-xs text-stone-400 flex flex-wrap gap-x-4 gap-y-1">
                        <span>Múi giờ hệ thống: <strong className="text-stone-600">{profile.timezone}</strong></span>
                        <span>Slug URL: <strong className="text-stone-600">/spas/{profile.slug}</strong></span>
                      </div>
                    )}

                    <div className="flex justify-end pt-3">
                      <Button type="submit" disabled={profileSaving || !name.trim()}>
                        {profileSaving ? (
                          <>
                            <Loader2 size={15} className="mr-1.5 animate-spin" />
                            <span>Đang lưu...</span>
                          </>
                        ) : (
                          <>
                            <Save size={15} className="mr-1.5" />
                            <span>Lưu thay đổi hồ sơ</span>
                          </>
                        )}
                      </Button>
                    </div>
                  </form>
                )}
              </CardContent>
            </Card>
          )}

          {/* STAFF ONLY: Avatar Profile Photo & Details */}
          {!isOwner && (
            <>
              <Card className="border-[#e7e2d8] shadow-xs">
                <CardContent className="p-6">
                  <div className="flex items-center gap-3 mb-5 pb-4 border-b border-stone-100">
                    <div className="p-2.5 bg-[#f2f6f3] text-[#465d4c] rounded-xl border border-[#c6d8c9]" aria-hidden="true">
                      <Camera size={20} />
                    </div>
                    <div>
                      <h3 className="font-serif-title font-semibold text-lg text-stone-900">
                        Ảnh đại diện nhân viên
                      </h3>
                      <p className="text-xs text-stone-500">
                        Ảnh đại diện sẽ xuất hiện trên trang đặt lịch công khai của TIKEY SPA để khách hàng nhận diện.
                      </p>
                    </div>
                  </div>

                  {avatarMessage && (
                    <Alert tone={avatarMessage.type} className="mb-4">
                      {avatarMessage.text}
                    </Alert>
                  )}

                  <div className="flex flex-col sm:flex-row items-center gap-6 py-2">
                    {/* Avatar Display / Preview */}
                    <div className="relative group shrink-0">
                      <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-[#c6d8c9] shadow-sm bg-stone-100 flex items-center justify-center">
                        {avatarPreview ? (
                          <img
                            src={avatarPreview}
                            alt="Xem trước ảnh đại diện"
                            className="w-full h-full object-cover"
                          />
                        ) : staffProfile?.avatarUrl ? (
                          <img
                            src={resolveMediaUrl(staffProfile.avatarUrl)}
                            alt={staffProfile.name || 'Ảnh đại diện'}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center bg-[#f2f6f3] text-[#465d4c] font-serif-title font-semibold text-2xl">
                            {staffProfile?.name?.charAt(0).toUpperCase() || user?.username?.charAt(0).toUpperCase() || 'S'}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Upload & Action Controls */}
                    <div className="flex-1 space-y-3 text-center sm:text-left">
                      <div>
                        <p className="text-xs text-stone-600 mb-2">
                          Định dạng hỗ trợ: <strong>JPG, PNG, WEBP</strong>. Dung lượng tối đa: <strong>5MB</strong>.
                        </p>
                        <label className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-stone-100 hover:bg-stone-200 text-stone-800 text-xs font-semibold cursor-pointer transition-colors border border-stone-200">
                          <Upload size={14} />
                          <span>{avatarPreview ? 'Chọn ảnh khác' : 'Tải ảnh mới'}</span>
                          <input
                            type="file"
                            accept="image/jpeg,image/png,image/webp"
                            className="hidden"
                            onChange={handleAvatarFileSelect}
                            disabled={avatarLoading}
                          />
                        </label>
                      </div>

                      <div className="flex flex-wrap items-center gap-2 justify-center sm:justify-start">
                        {avatarPreview && (
                          <Button
                            size="sm"
                            onClick={handleSaveAvatar}
                            disabled={avatarLoading}
                            className="bg-[#465d4c] text-white hover:bg-[#374a3c]"
                          >
                            {avatarLoading ? (
                              <>
                                <Loader2 size={13} className="mr-1 animate-spin" />
                                <span>Đang lưu...</span>
                              </>
                            ) : (
                              <>
                                <CheckCircle2 size={13} className="mr-1" />
                                <span>Lưu ảnh đại diện</span>
                              </>
                            )}
                          </Button>
                        )}

                        {(staffProfile?.avatarUrl || avatarPreview) && (
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={handleDeleteAvatar}
                            disabled={avatarLoading}
                            className="text-rose-700 hover:bg-rose-50 border-rose-200"
                          >
                            <Trash2 size={13} className="mr-1 text-rose-600" />
                            <span>Xóa ảnh</span>
                          </Button>
                        )}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* STAFF Personal Details Form */}
              <Card className="border-[#e7e2d8] shadow-xs">
                <CardContent className="p-6">
                  <div className="flex items-center gap-3 mb-5 pb-4 border-b border-stone-100">
                    <div className="p-2.5 bg-[#f2f6f3] text-[#465d4c] rounded-xl border border-[#c6d8c9]" aria-hidden="true">
                      <UserRound size={20} />
                    </div>
                    <div>
                      <h3 className="font-serif-title font-semibold text-lg text-stone-900">
                        Hồ sơ cá nhân & liên hệ
                      </h3>
                      <p className="text-xs text-stone-500">
                        Cập nhật họ tên, số điện thoại và email đăng nhập tài khoản của bạn.
                      </p>
                    </div>
                  </div>

                  {staffSuccess && (
                    <Alert tone="success" className="mb-4">
                      {staffSuccess}
                    </Alert>
                  )}

                  {staffError && (
                    <Alert tone="error" className="mb-4">
                      {staffError}
                    </Alert>
                  )}

                  <form onSubmit={handleSaveStaffProfile} className="space-y-4">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                        Họ và tên
                      </label>
                      <input
                        type="text"
                        required
                        value={staffName}
                        onChange={(e) => setStaffName(e.target.value)}
                        placeholder="VD: Trần Linh"
                        className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                        disabled={staffSaving}
                      />
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                          Số điện thoại
                        </label>
                        <input
                          type="text"
                          value={staffPhone}
                          onChange={(e) => setStaffPhone(e.target.value)}
                          placeholder="VD: 0987654321"
                          className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                          disabled={staffSaving}
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                          Email / Tên đăng nhập
                        </label>
                        <input
                          type="email"
                          required
                          value={staffEmail}
                          onChange={(e) => setStaffEmail(e.target.value)}
                          placeholder="linh@tikeyspa.vn"
                          className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                          disabled={staffSaving}
                        />
                      </div>
                    </div>

                    <div className="flex justify-end pt-3">
                      <Button type="submit" disabled={staffSaving || !staffName.trim() || !staffEmail.trim()}>
                        {staffSaving ? (
                          <>
                            <Loader2 size={15} className="mr-1.5 animate-spin" />
                            <span>Đang lưu...</span>
                          </>
                        ) : (
                          <>
                            <Save size={15} className="mr-1.5" />
                            <span>Lưu thay đổi</span>
                          </>
                        )}
                      </Button>
                    </div>
                  </form>
                </CardContent>
              </Card>
            </>
          )}
        </div>

        {/* Right Column: Account, Security, Password & Session */}
        <div className="lg:col-span-5 space-y-6">
          {/* User Account Details */}
          <Card className="border-[#e7e2d8] shadow-xs">
            <CardContent className="p-6">
              <div className="flex items-center gap-3 mb-5 pb-4 border-b border-stone-100">
                <div className="p-2.5 bg-[#f2f6f3] text-[#465d4c] rounded-xl border border-[#c6d8c9]" aria-hidden="true">
                  <UserRound size={20} />
                </div>
                <div>
                  <h3 className="font-serif-title font-semibold text-lg text-stone-900">
                    {isOwner ? 'Tài khoản quản trị' : 'Tài khoản nhân viên'}
                  </h3>
                  <p className="text-xs text-stone-500">
                    Thông tin phiên làm việc và bảo mật tài khoản.
                  </p>
                </div>
              </div>

              <dl className="space-y-4">
                <div>
                  <dt className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-1">
                    Tên đăng nhập
                  </dt>
                  <dd className="text-base text-stone-900 font-semibold font-mono bg-stone-50 px-3.5 py-2 rounded-xl border border-stone-200/80">
                    {user?.username || 'Không xác định'}
                  </dd>
                </div>

                <div className="grid grid-cols-2 gap-3 pt-1">
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-1">
                      Vai trò
                    </dt>
                    <dd className="mt-1">
                      {user?.roles?.length ? (
                        user.roles.map((role) => (
                          <Badge key={role} tone="info">
                            {formatRole(role)}
                          </Badge>
                        ))
                      ) : (
                        <span className="text-sm text-stone-500">Chưa có vai trò</span>
                      )}
                    </dd>
                  </div>

                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-1">
                      Trạng thái
                    </dt>
                    <dd className="mt-1">
                      <Badge tone="success">Đang hoạt động</Badge>
                    </dd>
                  </div>
                </div>

                <div className="pt-2 border-t border-stone-100">
                  <dt className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-1">
                    Cơ sở trực thuộc
                  </dt>
                  <dd className="text-xs font-medium text-stone-700">
                    TIKEY SPA &amp; Wellness Center
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>

          {/* Change Password */}
          <Card className="border-[#e7e2d8] shadow-xs">
            <CardContent className="p-6">
              <div className="flex items-center gap-3 mb-5 pb-4 border-b border-stone-100">
                <div className="p-2.5 bg-[#f2f6f3] text-[#465d4c] rounded-xl border border-[#c6d8c9]" aria-hidden="true">
                  <KeyRound size={20} />
                </div>
                <div>
                  <h3 className="font-serif-title font-semibold text-lg text-stone-900">
                    Đổi mật khẩu tài khoản
                  </h3>
                  <p className="text-xs text-stone-500">
                    Cập nhật mật khẩu định kỳ để bảo vệ tài khoản.
                  </p>
                </div>
              </div>

              {passwordSuccess && (
                <Alert tone="success" className="mb-4">
                  {passwordSuccess}
                </Alert>
              )}

              {passwordError && (
                <Alert tone="error" className="mb-4">
                  {passwordError}
                </Alert>
              )}

              <form onSubmit={handleChangePassword} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                    Mật khẩu hiện tại
                  </label>
                  <input
                    type="password"
                    required
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Nhập mật khẩu đang dùng"
                    className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                    disabled={passwordLoading}
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                    Mật khẩu mới
                  </label>
                  <input
                    type="password"
                    required
                    minLength={6}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Ít nhất 6 ký tự"
                    className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                    disabled={passwordLoading}
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                    Xác nhận mật khẩu mới
                  </label>
                  <input
                    type="password"
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Nhập lại mật khẩu mới"
                    className="w-full h-11 px-3.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c] focus:bg-white"
                    disabled={passwordLoading}
                  />
                </div>

                <div className="flex justify-end pt-2">
                  <Button type="submit" disabled={passwordLoading || !currentPassword || !newPassword}>
                    {passwordLoading ? (
                      <>
                        <Loader2 size={15} className="mr-1.5 animate-spin" />
                        <span>Đang cập nhật...</span>
                      </>
                    ) : (
                      <>
                        <ShieldCheck size={15} className="mr-1.5" />
                        <span>Cập nhật mật khẩu</span>
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* Session & Logout */}
          <Card className="border-[#e7e2d8] shadow-xs">
            <CardContent className="p-6">
              <h3 className="font-serif-title font-semibold text-base text-stone-900 mb-2">
                Phiên làm việc & Bảo mật
              </h3>
              <p className="text-xs text-stone-500 mb-5">
                Đăng xuất sẽ kết thúc phiên làm việc hiện tại trên trình duyệt này. Bạn cần đăng nhập lại để tiếp tục quản lý và xem lịch hẹn.
              </p>
              <Button variant="secondary" onClick={handleLogout} className="w-full sm:w-auto justify-center">
                <LogOut size={16} aria-hidden="true" />
                Đăng xuất khỏi hệ thống
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </AppShell>
  );
};

export default Settings;
