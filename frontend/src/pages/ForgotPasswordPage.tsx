import { useState, useId } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Sparkles, Mail, CheckCircle2, ArrowLeft } from 'lucide-react';
import { forgotPassword } from '../lib/api/auth';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const emailId = useId();
  const alertId = useId();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage(null);

    const trimmed = email.trim();
    if (!trimmed) {
      setErrorMessage('Vui lòng nhập địa chỉ email.');
      return;
    }

    if (!EMAIL_REGEX.test(trimmed)) {
      setErrorMessage('Địa chỉ email không đúng định dạng.');
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await forgotPassword(trimmed);
      setSuccessMessage(res.message || 'Nếu địa chỉ email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn.');
    } catch {
      // In case of unexpected network failure
      setErrorMessage('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.');
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

        <h1 className="login-heading font-serif-title">Quên mật khẩu</h1>
        <p className="login-subtitle">
          Nhập địa chỉ email tài khoản Chủ cơ sở hoặc Nhân viên để nhận liên kết đặt lại mật khẩu
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

        {successMessage ? (
          <div className="flex flex-col gap-5 text-center py-2" role="status" aria-live="polite">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-emerald-50 text-emerald-600 mx-auto border border-emerald-200">
              <CheckCircle2 size={24} />
            </div>
            <div className="bg-emerald-50/70 border border-emerald-200/80 rounded-lg p-4 text-sm text-emerald-900 text-left">
              <p className="font-medium mb-1">Đã gửi yêu cầu thành công</p>
              <p className="text-emerald-800 leading-relaxed text-xs">
                {successMessage}
              </p>
              <p className="mt-2 text-xs text-stone-500">
                Lưu ý: Liên kết đặt lại mật khẩu có hiệu lực trong vòng <strong>15 phút</strong>. Vui lòng kiểm tra cả hộp thư rác (Spam).
              </p>
            </div>
            <Link
              to="/login"
              className="inline-flex items-center justify-center gap-2 text-sm font-medium text-[#465d4c] hover:text-[#36493c] transition-colors pt-2"
            >
              <ArrowLeft size={16} /> Quay lại đăng nhập
            </Link>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit}
            noValidate
            aria-describedby={errorMessage ? alertId : undefined}
            className="flex flex-col gap-5"
          >
            <Input
              id={emailId}
              label="Địa chỉ email đã đăng ký"
              type="email"
              name="email"
              autoComplete="email"
              required
              autoFocus
              disabled={isSubmitting}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="ten@example.com"
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
                'Đang gửi yêu cầu...'
              ) : (
                <span className="inline-flex items-center gap-2">
                  <Mail size={16} /> Gửi hướng dẫn đặt lại mật khẩu
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

export default ForgotPasswordPage;
