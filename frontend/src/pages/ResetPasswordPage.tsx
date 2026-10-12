import { useState, useEffect, useId } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Sparkles, KeyRound, CheckCircle2, AlertTriangle, ArrowLeft } from 'lucide-react';
import { resetPassword } from '../lib/api/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '../types/api';

const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState<string | null>(() => {
    const queryToken = searchParams.get('token');
    return queryToken && queryToken.trim() ? queryToken.trim() : null;
  });

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  const passwordId = useId();
  const confirmPasswordId = useId();
  const alertId = useId();

  // Clean visible browser URL once to avoid exposing token in history / shoulder surfing
  useEffect(() => {
    if (searchParams.has('token')) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, [searchParams]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!token) {
      setErrorMessage('Không tìm thấy mã xác thực (token). Vui lòng yêu cầu lại liên kết đặt lại mật khẩu.');
      return;
    }

    if (!newPassword || newPassword.length < 6) {
      setErrorMessage('Mật khẩu mới phải có ít nhất 6 ký tự.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage('Xác nhận mật khẩu không khớp với mật khẩu mới.');
      return;
    }

    setIsSubmitting(true);
    try {
      await resetPassword(token, newPassword);
      setIsSuccess(true);
      setNewPassword('');
      setConfirmPassword('');
      setToken(null); // Clear token from memory immediately after consumption
    } catch (err: unknown) {
      const axiosErr = err as AxiosError<ApiError>;
      if (!axiosErr.response) {
        setErrorMessage('Không thể kết nối đến máy chủ. Vui lòng thử lại.');
      } else if (axiosErr.response.status === 400) {
        const serverMsg = axiosErr.response.data?.message;
        setErrorMessage(serverMsg || 'Token đã hết hạn hoặc không hợp lệ.');
      } else if (axiosErr.response.status === 429) {
        setErrorMessage('Quá nhiều yêu cầu thử lại. Vui lòng đợi trong giây lát.');
      } else {
        setErrorMessage('Đã xảy ra lỗi khi đặt lại mật khẩu. Vui lòng thử lại.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <main className="login-card">
        {/* Brand */}
        <div className="login-brand">
          <span className="login-brand-icon" aria-hidden="true">
            <Sparkles size={24} className="text-[#465d4c]" />
          </span>
          <span className="font-serif-title text-2xl font-bold text-[var(--color-champagne-500)] tracking-tight">
            TIKEY SPA
          </span>
        </div>

        <h1 className="login-heading font-serif-title">Đặt lại mật khẩu</h1>
        <p className="login-subtitle">
          Tạo mật khẩu mới cho tài khoản quản trị của bạn
        </p>

        {errorMessage && (
          <div
            id={alertId}
            role="alert"
            className="login-error"
            aria-live="assertive"
          >
            {errorMessage}
          </div>
        )}

        {isSuccess ? (
          <div className="flex flex-col gap-5 text-center py-2" role="status" aria-live="polite">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50 text-emerald-600 mx-auto border border-emerald-200">
              <CheckCircle2 size={24} />
            </div>
            <div className="bg-emerald-50/70 border border-emerald-200/80 rounded-lg p-4 text-sm text-emerald-900 text-left">
              <p className="font-medium mb-1">Đặt lại mật khẩu thành công</p>
              <p className="text-emerald-800 text-xs leading-relaxed">
                Mật khẩu tài khoản của bạn đã được cập nhật thành công. Tất cả các phiên đăng nhập trước đó đã được thu hồi vì lý do bảo mật.
              </p>
            </div>
            <Link
              to="/login"
              className="inline-flex items-center justify-center gap-2 w-full py-2.5 px-4 rounded-lg bg-[#465d4c] text-white font-medium hover:bg-[#36493c] transition-colors shadow-sm text-sm"
            >
              Đăng nhập ngay
            </Link>
          </div>
        ) : !token ? (
          <div className="flex flex-col gap-5 text-center py-2">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-amber-50 text-amber-600 mx-auto border border-amber-200">
              <AlertTriangle size={24} />
            </div>
            <div className="bg-amber-50/70 border border-amber-200/80 rounded-lg p-4 text-sm text-amber-900 text-left">
              <p className="font-medium mb-1">Liên kết không hợp lệ hoặc thiếu mã xác thực</p>
              <p className="text-amber-800 text-xs leading-relaxed">
                Liên kết đặt lại mật khẩu này không chứa mã xác thực hợp lệ hoặc đã được sử dụng. Vui lòng gửi lại yêu cầu để nhận liên kết mới.
              </p>
            </div>
            <div className="flex flex-col gap-2 pt-2">
              <Link
                to="/forgot-password"
                className="inline-flex items-center justify-center gap-2 w-full py-2.5 px-4 rounded-lg bg-[#465d4c] text-white font-medium hover:bg-[#36493c] transition-colors shadow-sm text-sm"
              >
                Yêu cầu liên kết mới
              </Link>
              <Link
                to="/login"
                className="inline-flex items-center justify-center gap-2 text-xs text-stone-500 hover:text-[#465d4c] transition-colors py-2"
              >
                <ArrowLeft size={14} /> Quay lại trang đăng nhập
              </Link>
            </div>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit}
            noValidate
            aria-describedby={errorMessage ? alertId : undefined}
            className="flex flex-col gap-5"
          >
            <Input
              id={passwordId}
              label="Mật khẩu mới"
              type="password"
              name="newPassword"
              autoComplete="new-password"
              required
              autoFocus
              disabled={isSubmitting}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Tối thiểu 6 ký tự"
              aria-required="true"
            />

            <Input
              id={confirmPasswordId}
              label="Xác nhận mật khẩu mới"
              type="password"
              name="confirmPassword"
              autoComplete="new-password"
              required
              disabled={isSubmitting}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Nhập lại mật khẩu mới"
              aria-required="true"
            />

            <Button
              type="submit"
              disabled={isSubmitting}
              aria-busy={isSubmitting}
              className="w-full mt-2"
              size="lg"
            >
              {isSubmitting ? (
                'Đang cập nhật mật khẩu...'
              ) : (
                <span className="inline-flex items-center gap-2">
                  <KeyRound size={16} /> Đổi mật khẩu
                </span>
              )}
            </Button>

            <div className="text-center pt-2">
              <Link
                to="/login"
                className="text-xs text-stone-500 hover:text-[#465d4c] transition-colors"
              >
                ← Quay lại trang đăng nhập
              </Link>
            </div>
          </form>
        )}
      </main>
    </div>
  );
};

export default ResetPasswordPage;
