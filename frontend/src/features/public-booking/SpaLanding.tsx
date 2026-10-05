import { useEffect, useState, useMemo } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { getPublicServices, getPublicStaff, getPublicArticles, getPublicReviews } from '../../lib/api/publicBooking';
import { submitPublicFeedback } from '../../lib/api/feedback';
import { formatCurrency } from '../../lib/format';
import type {
  PublicSpaInfoResponse,
  PublicServiceResponse,
  PublicStaffResponse,
  PublicArticleResponse,
  PublicReviewResponse,
} from '../../types/publicBooking';
import type { FeedbackType } from '../../types/feedback';
import { PublicBookingLookup } from './PublicBookingLookup';
import { HeroAtmosphere } from './HeroAtmosphere';
import {
  MapPin,
  Phone,
  Mail,
  Clock,
  CalendarCheck,
  UserRound,
  ShieldCheck,
  Sparkles,
  ArrowRight,
  Search,
  LogIn,
  Star,
  Send,
  HeartHandshake,
  BookOpen,
  Leaf,
  CheckCircle2,
  AlertCircle,
  X,
  Menu,
  Info,
} from 'lucide-react';

interface SpaLandingProps {
  slug: string;
  spa: PublicSpaInfoResponse;
  children: ReactNode;
}

function initials(name: string): string {
  const words = name.trim().split(/\s+/);
  if (words.length === 0 || !words[0]) return '?';
  const first = words[0][0];
  const last = words.length > 1 ? words[words.length - 1][0] : '';
  return (first + last).toUpperCase();
}

function parseProcessSteps(stepsJson?: string | null): string[] {
  if (!stepsJson) return [];
  try {
    const parsed = JSON.parse(stepsJson);
    if (Array.isArray(parsed)) return parsed;
  } catch {
    return stepsJson.split('\n').filter((s) => s.trim().length > 0);
  }
  return [];
}

export function SpaLanding({ slug, spa, children }: SpaLandingProps) {
  const [services, setServices] = useState<PublicServiceResponse[] | null>(null);
  const [servicesFailed, setServicesFailed] = useState(false);
  const [staffList, setStaffList] = useState<PublicStaffResponse[] | null>(null);
  const [staffFailed, setStaffFailed] = useState(false);
  const [articles, setArticles] = useState<PublicArticleResponse[] | null>(null);
  const [reviews, setReviews] = useState<PublicReviewResponse[] | null>(null);

  // Modals
  const [selectedServiceDetail, setSelectedServiceDetail] = useState<PublicServiceResponse | null>(null);
  const [readingArticle, setReadingArticle] = useState<PublicArticleResponse | null>(null);

  // Mobile menu toggle
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Header scroll detection for quiet-luxury glass transition
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 15);
    };
    handleScroll();
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Feedback form state
  const [fbName, setFbName] = useState('');
  const [fbPhone, setFbPhone] = useState('');
  const [fbEmail, setFbEmail] = useState('');
  const [fbType, setFbType] = useState<FeedbackType>('SUGGESTION');
  const [fbBookingCode, setFbBookingCode] = useState('');
  const [fbMessage, setFbMessage] = useState('');

  const [fbSubmitting, setFbSubmitting] = useState(false);
  const [fbSuccess, setFbSuccess] = useState(false);
  const [fbError, setFbError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    getPublicServices(slug)
      .then((data) => {
        if (!cancelled) setServices(data);
      })
      .catch(() => {
        if (!cancelled) setServicesFailed(true);
      });

    getPublicStaff(slug)
      .then((data) => {
        if (!cancelled) setStaffList(data);
      })
      .catch(() => {
        if (!cancelled) setStaffFailed(true);
      });

    getPublicArticles(slug)
      .then((data) => {
        if (!cancelled) setArticles(data);
      })
      .catch(() => {
        // Fallback or ignore
      });

    getPublicReviews(slug)
      .then((data) => {
        if (!cancelled) setReviews(data);
      })
      .catch(() => {
        // Fallback or ignore
      });

    return () => {
      cancelled = true;
    };
  }, [slug]);

  // Featured services filtered strictly by isFeatured
  const featuredServices = useMemo(() => {
    if (!services) return [];
    return services.filter((s) => s.isFeatured);
  }, [services]);

  const handleFeedbackSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFbError(null);

    // Basic validation
    if (!fbName.trim()) {
      setFbError('Vui lòng nhập họ và tên của bạn.');
      return;
    }
    const cleanPhone = fbPhone.trim().replace(/\D/g, '');
    if (cleanPhone.length < 10 || cleanPhone.length > 11) {
      setFbError('Số điện thoại không hợp lệ (cần từ 10 - 11 chữ số).');
      return;
    }
    if (!fbMessage.trim() || fbMessage.trim().length < 10) {
      setFbError('Nội dung phản hồi cần tối thiểu 10 ký tự để chúng tôi hiểu rõ ý kiến của bạn.');
      return;
    }

    try {
      setFbSubmitting(true);
      await submitPublicFeedback(slug, {
        name: fbName.trim(),
        phone: cleanPhone,
        email: fbEmail.trim() ? fbEmail.trim() : undefined,
        type: fbType,
        bookingCode: fbBookingCode.trim() ? fbBookingCode.trim().toUpperCase() : undefined,
        message: fbMessage.trim(),
      });

      setFbSuccess(true);
      setFbName('');
      setFbPhone('');
      setFbEmail('');
      setFbBookingCode('');
      setFbMessage('');
      setFbType('SUGGESTION');
    } catch {
      setFbError('Không thể gửi phản hồi lúc này. Xin vui lòng thử lại sau.');
    } finally {
      setFbSubmitting(false);
    }
  };

  const showStaff = (staffList && staffList.length > 0) || staffFailed;

  return (
    <div className="min-h-screen bg-[#faf8f5] text-stone-800 flex flex-col font-sans antialiased selection:bg-[#465d4c] selection:text-white">
      {/* HEADER / NAVIGATION BAR */}
      <header
        className={`sticky top-0 z-40 transition-all duration-300 ${
          isScrolled
            ? 'bg-[#faf8f5]/85 backdrop-blur-md shadow-xs border-b border-[#e7e2d8]'
            : 'bg-[#faf8f5]/60 backdrop-blur-xs border-b border-transparent'
        }`}
      >
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 h-18 sm:h-20 flex items-center justify-between gap-4">
          {/* Logo & Spa Name */}
          <a href="#" className="flex items-center gap-2.5 group shrink-0">
            <span className="w-8 h-8 rounded-full bg-[#f2f6f3] border border-[#c6d8c9] flex items-center justify-center group-hover:scale-105 transition-transform">
              <Sparkles className="w-4 h-4 text-[#b8976c]" aria-hidden="true" />
            </span>
            <div className="flex flex-col">
              <span className="font-serif-title font-bold text-lg text-[var(--color-champagne-500)] tracking-tight leading-tight whitespace-nowrap">
                {spa.name}
              </span>
              <span className="text-[10px] text-stone-500 uppercase tracking-widest font-semibold whitespace-nowrap">
                Spa & Chăm Sóc Sức Khỏe
              </span>
            </div>
          </a>

          {/* Quick Nav Links — Desktop */}
          <nav className="hidden lg:flex items-center gap-3.5 xl:gap-6 text-xs font-semibold text-stone-600 uppercase tracking-wider flex-nowrap shrink-0">
            <a href="#gioi-thieu" className="whitespace-nowrap hover:text-[#465d4c] transition-colors py-1">
              Giới thiệu
            </a>
            <a href="#dich-vu" className="whitespace-nowrap hover:text-[#465d4c] transition-colors py-1">
              Dịch vụ nổi bật
            </a>
            {showStaff && (
              <a href="#doi-ngu" className="whitespace-nowrap hover:text-[#465d4c] transition-colors py-1">
                Đội ngũ
              </a>
            )}
            <a href="#goc-cham-soc" className="whitespace-nowrap hover:text-[#465d4c] transition-colors py-1">
              Góc chăm sóc
            </a>
            <a href="#danh-gia" className="whitespace-nowrap hover:text-[#465d4c] transition-colors py-1">
              Đánh giá
            </a>
            <a href="#phan-hoi" className="whitespace-nowrap hover:text-[#465d4c] transition-colors py-1">
              Phản hồi
            </a>
            <a href="#tra-cuu" className="whitespace-nowrap hover:text-[#465d4c] transition-colors py-1">
              Tra cứu
            </a>
          </nav>

          {/* Header Action Buttons */}
          <div className="flex items-center gap-2 sm:gap-2.5 shrink-0">
            <Link
              to="/login"
              className="hidden sm:inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium text-stone-600 hover:text-stone-900 rounded-lg hover:bg-black/5 transition-colors whitespace-nowrap"
              title="Cổng nhân viên & Quản trị"
            >
              <LogIn className="w-3.5 h-3.5" aria-hidden="true" />
              <span className="whitespace-nowrap">Đăng nhập</span>
            </Link>

            <a
              href="#booking"
              className="inline-flex items-center gap-1.5 px-3.5 sm:px-4 py-2 bg-[#465d4c] hover:bg-[#374a3c] text-white text-xs sm:text-sm font-medium rounded-xl transition-all shadow-xs hover:shadow-sm whitespace-nowrap"
            >
              <span className="whitespace-nowrap">Đặt lịch ngay</span>
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </a>

            {/* Mobile menu button */}
            <button
              type="button"
              className="lg:hidden p-2 text-stone-600 hover:text-stone-900 focus:outline-none"
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              aria-label="Mở menu chuyển trang"
            >
              {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Mobile dropdown navigation */}
        {isMobileMenuOpen && (
          <div className="lg:hidden bg-[#faf8f5]/95 backdrop-blur-xl border-b border-[#e7e2d8] px-4 py-4 space-y-2 shadow-lg">
            <div className="flex flex-col space-y-1.5 text-sm font-medium text-stone-700">
              <a
                href="#gioi-thieu"
                onClick={() => setIsMobileMenuOpen(false)}
                className="whitespace-nowrap py-2 px-3 hover:bg-black/5 rounded-xl transition-colors"
              >
                Giới thiệu
              </a>
              <a
                href="#dich-vu"
                onClick={() => setIsMobileMenuOpen(false)}
                className="whitespace-nowrap py-2 px-3 hover:bg-black/5 rounded-xl transition-colors"
              >
                Dịch vụ nổi bật
              </a>
              {showStaff && (
                <a
                  href="#doi-ngu"
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="whitespace-nowrap py-2 px-3 hover:bg-black/5 rounded-xl transition-colors"
                >
                  Đội ngũ chuyên viên
                </a>
              )}
              <a
                href="#goc-cham-soc"
                onClick={() => setIsMobileMenuOpen(false)}
                className="whitespace-nowrap py-2 px-3 hover:bg-black/5 rounded-xl transition-colors"
              >
                Góc chăm sóc (Bài viết)
              </a>
              <a
                href="#danh-gia"
                onClick={() => setIsMobileMenuOpen(false)}
                className="whitespace-nowrap py-2 px-3 hover:bg-black/5 rounded-xl transition-colors"
              >
                Đánh giá khách hàng
              </a>
              <a
                href="#phan-hoi"
                onClick={() => setIsMobileMenuOpen(false)}
                className="whitespace-nowrap py-2 px-3 hover:bg-black/5 rounded-xl transition-colors text-[#b8976c] font-semibold"
              >
                Phản hồi & Khiếu nại
              </a>
              <a
                href="#tra-cuu"
                onClick={() => setIsMobileMenuOpen(false)}
                className="whitespace-nowrap py-2 px-3 hover:bg-black/5 rounded-xl transition-colors"
              >
                Tra cứu lịch hẹn
              </a>
              <Link
                to="/login"
                onClick={() => setIsMobileMenuOpen(false)}
                className="whitespace-nowrap py-2 px-3 text-stone-500 hover:text-stone-900 border-t border-stone-200/80 pt-2.5 flex items-center gap-2"
              >
                <LogIn className="w-4 h-4" />
                <span className="whitespace-nowrap">Đăng nhập Quản trị / Nhân viên</span>
              </Link>
            </div>
          </div>
        )}
      </header>

      {/* 1. HERO SECTION */}
      <section className="relative overflow-hidden pt-12 sm:pt-20 pb-16 sm:pb-24 border-b border-[#e7e2d8]">
        <HeroAtmosphere />

        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/75 backdrop-blur-md border border-white/60 shadow-xs text-[#374a3c] text-xs font-semibold uppercase tracking-wider mb-5">
            <Sparkles className="w-3.5 h-3.5 text-[#b8976c]" aria-hidden="true" />
            <span className="whitespace-nowrap">Nghệ thuật chăm sóc sức khỏe & sắc đẹp</span>
          </div>

          <h1 className="text-3xl sm:text-5xl lg:text-6xl font-serif-title font-medium tracking-tight text-stone-900 max-w-3xl mx-auto mb-6 leading-tight">
            Khoảnh khắc tĩnh tại, phục hồi trọn vẹn tại <span className="italic text-[#465d4c]">{spa.name}</span>
          </h1>

          <p className="text-stone-600 text-base sm:text-lg max-w-2xl mx-auto mb-10 leading-relaxed font-light">
            Không gian yên tĩnh giao hòa cùng thảo mộc tự nhiên Việt Nam. Trải nghiệm các liệu pháp massage,
            trị liệu da và phục hồi năng lượng được thiết kế riêng cho bạn.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4 mb-14">
            <a
              href="#booking"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-7 py-3.5 bg-[#465d4c] text-white font-medium rounded-xl hover:bg-[#374a3c] transition-all shadow-md hover:shadow-lg max-sm:min-h-11"
            >
              <span>Đặt lịch trực tuyến</span>
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </a>

            <a
              href="#dich-vu"
              className="w-full sm:w-auto inline-flex items-center justify-center px-6 py-3.5 bg-white/80 backdrop-blur-md border border-[#d8d1c3]/70 text-stone-700 font-medium rounded-xl hover:bg-white hover:text-stone-900 transition-colors max-sm:min-h-11 shadow-xs"
            >
              Khám phá dịch vụ
            </a>

            <a
              href="#tra-cuu"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-[#f6efe2]/80 backdrop-blur-md text-[#9e7d52] font-medium rounded-xl hover:bg-[#ebdcc8] transition-colors max-sm:min-h-11 border border-[#ebdcc8]/50"
            >
              <Search className="w-4 h-4" aria-hidden="true" />
              <span>Tra cứu lịch hẹn</span>
            </a>
          </div>

          {/* Highlights */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-3xl mx-auto text-left">
            <div className="p-4 rounded-2xl bg-white/75 backdrop-blur-md border border-white/60 shadow-xs flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-[#f2f6f3]/90 text-[#465d4c] shrink-0">
                <CalendarCheck className="w-5 h-5" aria-hidden="true" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-stone-900">Chọn giờ linh hoạt</h4>
                <p className="text-xs text-stone-500 mt-0.5">Khung giờ cập nhật trực tiếp theo thời gian thực</p>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-white/75 backdrop-blur-md border border-white/60 shadow-xs flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-[#f2f6f3]/90 text-[#465d4c] shrink-0">
                <UserRound className="w-5 h-5" aria-hidden="true" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-stone-900">Không cần tài khoản</h4>
                <p className="text-xs text-stone-500 mt-0.5">Đặt lịch nhanh chóng với số điện thoại liên hệ</p>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-white/75 backdrop-blur-md border border-white/60 shadow-xs flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-[#f2f6f3]/90 text-[#465d4c] shrink-0">
                <ShieldCheck className="w-5 h-5" aria-hidden="true" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-stone-900">Mã tra cứu bảo mật</h4>
                <p className="text-xs text-stone-500 mt-0.5">Dễ dàng theo dõi tiến độ lịch hẹn bất cứ khi nào</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. ABOUT / PHILOSOPHY SECTION */}
      <section id="gioi-thieu" className="py-16 sm:py-24 border-b border-[#e7e2d8] bg-[#fcfaf7] scroll-mt-20">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-14 items-center">
            {/* Left Narrative */}
            <div className="lg:col-span-7 space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#f2f6f3] border border-[#c6d8c9] text-[#465d4c] text-xs font-semibold uppercase tracking-wider">
                <Leaf className="w-3.5 h-3.5 text-[#b8976c]" />
                <span>Triết lý dưỡng sinh & phục hồi</span>
              </div>
              <h2 className="text-2xl sm:text-4xl font-serif-title font-medium text-stone-900 leading-tight">
                Không gian tĩnh lặng giữa lòng đô thị, nơi thân tâm tìm về chốn bình yên
              </h2>
              <p className="text-stone-600 text-sm sm:text-base leading-relaxed">
                Tại <strong className="font-semibold text-stone-800">{spa.name}</strong>, chúng tôi tin rằng
                chăm sóc sức khỏe không chỉ dừng lại ở các liệu trình trên bề mặt da hay cơ bắp. Đó là sự giao thoa
                hài hòa giữa tinh hoa thảo mộc bản địa Việt Nam, kỹ thuật ấn huyệt cổ truyền và không gian kiến trúc
                mang hơi thở quiet luxury tối giản.
              </p>
              <p className="text-stone-600 text-sm sm:text-base leading-relaxed">
                Từng góc nhỏ đều được chăm chút với ánh sáng dịu mắt, hương thơm tự nhiên từ tinh dầu thảo mộc và
                giai điệu thiền định êm đềm — giúp bạn buông bỏ âu lo thường nhật ngay khi bước chân qua cánh cửa.
              </p>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-6 pt-4 border-t border-[#e7e2d8]">
                <div>
                  <span className="block font-serif-title text-2xl sm:text-3xl font-bold text-[#465d4c]">100%</span>
                  <span className="text-xs text-stone-500 font-medium">Thảo mộc tự nhiên</span>
                </div>
                <div>
                  <span className="block font-serif-title text-2xl sm:text-3xl font-bold text-[#b8976c]">1:1</span>
                  <span className="text-xs text-stone-500 font-medium">Chuyên viên riêng biệt</span>
                </div>
                <div>
                  <span className="block font-serif-title text-2xl sm:text-3xl font-bold text-stone-800">Chuẩn</span>
                  <span className="text-xs text-stone-500 font-medium">Vệ sinh & An toàn</span>
                </div>
              </div>
            </div>

            {/* Right Card */}
            <div className="lg:col-span-5">
              <div className="relative rounded-3xl p-8 bg-white border border-[#e7e2d8] shadow-sm">
                <div className="w-12 h-12 rounded-2xl bg-[#f2f6f3] border border-[#c6d8c9] flex items-center justify-center text-[#465d4c] mb-6">
                  <HeartHandshake className="w-6 h-6 text-[#b8976c]" />
                </div>
                <h3 className="font-serif-title text-xl font-medium text-stone-900 mb-3">
                  Cam kết chất lượng dịch vụ
                </h3>
                <ul className="space-y-3.5 text-xs sm:text-sm text-stone-600">
                  <li className="flex items-start gap-2.5">
                    <CheckCircle2 className="w-4 h-4 text-[#465d4c] shrink-0 mt-0.5" />
                    <span>Kỹ thuật viên được đào tạo chính quy, am hiểu sâu sắc về giải phẫu cơ thể.</span>
                  </li>
                  <li className="flex items-start gap-2.5">
                    <CheckCircle2 className="w-4 h-4 text-[#465d4c] shrink-0 mt-0.5" />
                    <span>Dụng cụ, khăn và drap trải giường được hấp sấy tiệt trùng theo tiêu chuẩn y tế sau mỗi lượt khách.</span>
                  </li>
                  <li className="flex items-start gap-2.5">
                    <CheckCircle2 className="w-4 h-4 text-[#465d4c] shrink-0 mt-0.5" />
                    <span>Minh bạch thời gian và chi phí, tuyệt đối không chèo kéo bán gói hay tip ép buộc.</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. FEATURED SERVICES SECTION */}
      <section
        id="dich-vu"
        aria-labelledby="services-heading"
        className="py-16 sm:py-24 border-b border-[#e7e2d8] bg-white scroll-mt-20"
      >
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <span className="text-xs uppercase tracking-widest font-semibold text-[#566f5c]">
              Liệu trình tuyển chọn
            </span>
            <h2 id="services-heading" className="text-2xl sm:text-4xl font-serif-title font-medium text-stone-900 mt-1 mb-3">
              Dịch vụ nổi bật tại {spa.name}
            </h2>
            <p className="text-stone-500 text-sm">
              Mỗi liệu trình được chắt lọc tinh tế để đem lại hiệu quả trị liệu tối ưu và cảm giác thư giãn tuyệt đối.
            </p>
          </div>

          {servicesFailed ? (
            <p className="text-stone-500 text-center">Không thể tải danh sách dịch vụ.</p>
          ) : featuredServices.length === 0 ? (
            <div className="text-center py-10 px-4 bg-stone-50 rounded-2xl border border-stone-200 max-w-md mx-auto text-stone-500 text-sm">
              Hiện chưa có dịch vụ nào được gắn nổi bật. Vui lòng xem danh mục dịch vụ đầy đủ ở phần Đặt lịch bên dưới.
            </div>
          ) : (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {featuredServices.map((service) => {
                const steps = parseProcessSteps(service.processSteps);

                return (
                  <div
                    key={service.id}
                    className="flex flex-col rounded-2xl border border-[#e7e2d8] bg-[#faf8f5] hover:bg-white hover:border-[#c6d8c9] hover:shadow-md transition-all group overflow-hidden"
                  >
                    {service.imageUrl && (
                      <div className="h-44 w-full overflow-hidden bg-stone-100">
                        <img
                          src={service.imageUrl}
                          alt={service.name}
                          loading="lazy"
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                      </div>
                    )}
                    <div className="p-6 flex flex-col grow">
                      <div className="flex items-center justify-between gap-2 mb-2">
                        {service.categoryName && (
                          <span className="text-[11px] font-semibold text-[#566f5c] px-2 py-0.5 rounded-full bg-[#f2f6f3] border border-[#c6d8c9]/60">
                            {service.categoryName}
                          </span>
                        )}
                        <span className="inline-flex items-center gap-1 text-[10px] uppercase font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                          <Sparkles className="w-3 h-3 text-amber-500" />
                          Nổi bật
                        </span>
                      </div>

                      <div className="flex justify-between items-start gap-3 mb-2.5">
                        <h3 className="font-serif-title font-medium text-lg text-stone-900 group-hover:text-[#465d4c] transition-colors">
                          {service.name}
                        </h3>
                        <span className="font-semibold text-stone-900 shrink-0 text-base">
                          {formatCurrency(service.price)}
                        </span>
                      </div>

                      {service.description ? (
                        <p className="text-xs text-stone-600 mb-4 line-clamp-3 leading-relaxed">
                          {service.description}
                        </p>
                      ) : (
                        <p className="text-xs text-stone-400 italic mb-4">
                          Liệu trình chăm sóc toàn diện tại spa.
                        </p>
                      )}

                      {/* Process steps pill */}
                      {steps.length > 0 && (
                        <div className="mb-4">
                          <span className="text-[11px] text-stone-500 bg-white px-2 py-1 rounded-md border border-stone-200/70 inline-flex items-center gap-1">
                            <CheckCircle2 className="w-3 h-3 text-[#465d4c]" />
                            Quy trình {steps.length} bước trị liệu
                          </span>
                        </div>
                      )}

                      <div className="mt-auto pt-4 border-t border-[#e7e2d8] flex items-center justify-between text-xs text-stone-500">
                        <span className="flex items-center gap-1.5 font-medium">
                          <Clock className="w-4 h-4 text-[#566f5c]" aria-hidden="true" />
                          {service.durationMinutes} phút
                        </span>

                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => setSelectedServiceDetail(service)}
                            className="text-xs text-stone-600 hover:text-[#465d4c] font-medium inline-flex items-center gap-1 cursor-pointer"
                          >
                            <Info className="w-3.5 h-3.5" />
                            Xem chi tiết
                          </button>
                          <a
                            href="#booking"
                            className="inline-flex items-center gap-1 font-semibold text-[#465d4c] hover:underline"
                          >
                            Đặt lịch
                            <ArrowRight className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </section>

      {/* Service Detail Modal */}
      {selectedServiceDetail && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-xs animate-in fade-in duration-200"
        >
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border border-stone-200 overflow-hidden relative max-h-[90vh] flex flex-col text-left">
            <button
              type="button"
              onClick={() => setSelectedServiceDetail(null)}
              className="absolute top-4 right-4 p-2 rounded-full text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition-colors"
              aria-label="Đóng"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-4 mb-4">
              {selectedServiceDetail.imageUrl && (
                <img
                  src={selectedServiceDetail.imageUrl}
                  alt={selectedServiceDetail.name}
                  className="w-16 h-16 rounded-2xl object-cover border border-stone-200 shrink-0"
                />
              )}
              <div>
                {selectedServiceDetail.categoryName && (
                  <span className="text-[11px] font-semibold text-[#566f5c] px-2 py-0.5 rounded-full bg-[#f2f6f3] border border-[#c6d8c9]/60 inline-block mb-1">
                    {selectedServiceDetail.categoryName}
                  </span>
                )}
                <h3 className="text-xl font-serif-title font-medium text-stone-900 leading-snug">
                  {selectedServiceDetail.name}
                </h3>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-base font-semibold text-[#8a704c]">
                    {formatCurrency(selectedServiceDetail.price)}
                  </span>
                  <span className="text-xs text-stone-400">•</span>
                  <span className="text-xs text-stone-500 inline-flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 text-stone-400" />
                    {selectedServiceDetail.durationMinutes} phút
                  </span>
                </div>
              </div>
            </div>

            <div className="overflow-y-auto space-y-5 pr-1 flex-1">
              {selectedServiceDetail.description && (
                <div>
                  <h4 className="text-xs font-semibold uppercase tracking-wider text-stone-400 mb-1.5">
                    Mô tả liệu trình
                  </h4>
                  <p className="text-sm text-stone-600 leading-relaxed">
                    {selectedServiceDetail.description}
                  </p>
                </div>
              )}

              <div>
                <h4 className="text-xs font-semibold uppercase tracking-wider text-stone-400 mb-2.5">
                  Quy trình thực hiện ({parseProcessSteps(selectedServiceDetail.processSteps).length} bước)
                </h4>
                {parseProcessSteps(selectedServiceDetail.processSteps).length > 0 ? (
                  <ol className="space-y-2">
                    {parseProcessSteps(selectedServiceDetail.processSteps).map((step, idx) => (
                      <li key={idx} className="flex items-start gap-2.5 text-xs sm:text-sm text-stone-700 bg-stone-50 p-3 rounded-xl border border-stone-100">
                        <span className="flex items-center justify-center w-5 h-5 rounded-full bg-[#465d4c] text-white text-[11px] font-semibold shrink-0 mt-0.5">
                          {idx + 1}
                        </span>
                        <span className="leading-snug">{step.replace(/^\d+[.\s]*/, '')}</span>
                      </li>
                    ))}
                  </ol>
                ) : (
                  <p className="text-xs text-stone-500 italic bg-stone-50 p-3 rounded-xl border border-stone-100">
                    Liệu trình được thực hiện theo quy trình chuẩn hóa chăm sóc sức khỏe và phục hồi của TIKEY SPA.
                  </p>
                )}
              </div>
            </div>

            <div className="pt-4 mt-4 border-t border-stone-100 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setSelectedServiceDetail(null)}
                className="px-4 py-2.5 text-xs font-medium rounded-xl border border-stone-200 text-stone-600 hover:bg-stone-50"
              >
                Đóng
              </button>
              <a
                href="#booking"
                onClick={() => setSelectedServiceDetail(null)}
                className="inline-flex items-center gap-1.5 px-5 py-2.5 text-xs font-medium rounded-xl bg-[#465d4c] text-white hover:bg-[#374a3c] transition-colors shadow-xs"
              >
                <span>Đặt lịch ngay</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </a>
            </div>
          </div>
        </div>
      )}

      {/* 4. STAFF / TEAM SECTION */}
      {showStaff && (
        <section
          id="doi-ngu"
          aria-labelledby="staff-heading"
          className="py-16 sm:py-24 border-b border-[#e7e2d8] scroll-mt-20 bg-[#faf8f5]"
        >
          <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-2xl mx-auto mb-12">
              <span className="text-xs uppercase tracking-widest font-semibold text-[#566f5c]">
                Kỹ thuật viên lành nghề
              </span>
              <h2 id="staff-heading" className="text-2xl sm:text-4xl font-serif-title font-medium text-stone-900 mt-1 mb-3">
                Đội ngũ chuyên viên tận tâm
              </h2>
              <p className="text-stone-500 text-sm">
                Đội ngũ chuyên gia được đào tạo bài bản, thấu hiểu cơ thể và tâm lý khách hàng để mang đến buổi trị liệu tốt nhất.
              </p>
            </div>

            {staffFailed ? (
              <p className="text-stone-500 text-center">Không thể tải danh sách nhân viên.</p>
            ) : (
              <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 max-w-4xl mx-auto">
                {staffList!.map((member) => (
                  <div
                    key={member.id}
                    className="flex items-center gap-4 p-5 rounded-2xl border border-[#e7e2d8] bg-white shadow-xs hover:border-[#c6d8c9] transition-all"
                  >
                    <div
                      className="w-14 h-14 rounded-full overflow-hidden bg-[#f2f6f3] border border-[#c6d8c9] flex items-center justify-center text-sm font-semibold text-[#465d4c] shrink-0"
                    >
                      {member.avatarUrl ? (
                        <img
                          src={member.avatarUrl}
                          alt={member.name}
                          loading="lazy"
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        initials(member.name)
                      )}
                    </div>
                    <div>
                      <h4 className="font-semibold text-stone-900 text-base">{member.name}</h4>
                      <p className="text-xs text-stone-500 mt-0.5">Kỹ thuật viên trị liệu</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {/* 5. ARTICLES / BLOG SECTION ("GÓC CHĂM SÓC") */}
      <section
        id="goc-cham-soc"
        aria-labelledby="articles-heading"
        className="py-16 sm:py-24 border-b border-[#e7e2d8] bg-white scroll-mt-20"
      >
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-12 gap-4">
            <div>
              <span className="text-xs uppercase tracking-widest font-semibold text-[#566f5c]">
                Kiến thức & Phong cách sống
              </span>
              <h2 id="articles-heading" className="text-2xl sm:text-4xl font-serif-title font-medium text-stone-900 mt-1">
                Góc chăm sóc thân tâm
              </h2>
            </div>
            <p className="text-stone-500 text-xs sm:text-sm max-w-md">
              Những chia sẻ hữu ích từ đội ngũ chuyên gia TIKEY SPA giúp bạn duy trì vẻ đẹp và tinh thần sảng khoái mỗi ngày.
            </p>
          </div>

          {!articles || articles.length === 0 ? (
            <p className="text-stone-500 text-center py-8">Chưa có bài viết nào được xuất bản.</p>
          ) : (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {articles.map((art) => (
                <div
                  key={art.id}
                  className="flex flex-col rounded-2xl border border-[#e7e2d8] bg-[#faf8f5] hover:bg-white hover:border-[#c6d8c9] hover:shadow-md transition-all p-6 text-left"
                >
                  <div className="flex items-center justify-between text-[11px] font-semibold text-[#566f5c] mb-3">
                    <span className="px-2.5 py-0.5 rounded-full bg-[#f2f6f3] border border-[#c6d8c9]/60">
                      {art.category || 'Chăm sóc'}
                    </span>
                    <span className="text-stone-400 font-normal">{art.readTime || '3 phút đọc'}</span>
                  </div>

                  <h3 className="font-serif-title font-medium text-base text-stone-900 mb-2.5 leading-snug line-clamp-2">
                    {art.title}
                  </h3>

                  {art.excerpt && (
                    <p className="text-xs text-stone-600 line-clamp-3 mb-6 leading-relaxed">
                      {art.excerpt}
                    </p>
                  )}

                  <div className="mt-auto pt-4 border-t border-[#e7e2d8]">
                    <button
                      type="button"
                      onClick={() => setReadingArticle(art)}
                      className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#465d4c] hover:text-[#374a3c] transition-colors cursor-pointer"
                    >
                      <BookOpen className="w-3.5 h-3.5" />
                      <span>Đọc bài viết</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Article Detail Modal */}
      {readingArticle && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-article-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-xs text-left"
        >
          <div className="bg-[#faf8f5] border border-[#e7e2d8] rounded-3xl max-w-2xl w-full max-h-[85vh] overflow-y-auto p-6 sm:p-8 shadow-2xl relative">
            <button
              type="button"
              onClick={() => setReadingArticle(null)}
              className="absolute top-5 right-5 p-2 rounded-full hover:bg-stone-200 text-stone-500 hover:text-stone-800 transition-colors"
              aria-label="Đóng bài viết"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 text-xs text-[#566f5c] font-semibold mb-3">
              <span className="px-3 py-0.5 rounded-full bg-[#f2f6f3] border border-[#c6d8c9]">
                {readingArticle.category || 'Góc chăm sóc'}
              </span>
              <span className="text-stone-400 font-normal">{readingArticle.readTime || '3 phút đọc'}</span>
            </div>

            <h2 id="modal-article-title" className="text-2xl sm:text-3xl font-serif-title font-medium text-stone-900 mb-6 leading-tight">
              {readingArticle.title}
            </h2>

            {readingArticle.coverImage && (
              <div className="rounded-2xl overflow-hidden mb-6 border border-stone-200 max-h-72">
                <img
                  src={readingArticle.coverImage}
                  alt={readingArticle.title}
                  className="w-full h-full object-cover"
                />
              </div>
            )}

            <div className="space-y-4 text-stone-700 text-sm leading-relaxed mb-8 whitespace-pre-line">
              {readingArticle.content}
            </div>

            <div className="flex justify-end pt-4 border-t border-[#e7e2d8]">
              <button
                type="button"
                onClick={() => setReadingArticle(null)}
                className="px-5 py-2.5 bg-[#465d4c] text-white text-xs font-medium rounded-xl hover:bg-[#374a3c] transition-colors"
              >
                Đóng bài viết
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 6. REVIEWS / TESTIMONIALS SECTION */}
      <section
        id="danh-gia"
        aria-labelledby="testimonials-heading"
        className="py-16 sm:py-24 border-b border-[#e7e2d8] bg-[#fcfaf7] scroll-mt-20"
      >
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#f2f6f3] border border-[#c6d8c9] text-[#465d4c] text-xs font-semibold mb-3">
              <Sparkles className="w-3 h-3 text-[#b8976c]" />
              <span>Đánh giá từ khách hàng</span>
            </div>
            <h2 id="testimonials-heading" className="text-2xl sm:text-4xl font-serif-title font-medium text-stone-900 mt-1 mb-3">
              Khách hàng nói gì về chúng tôi
            </h2>
            <p className="text-stone-500 text-sm">
              Sự hài lòng và cảm giác nhẹ nhõm sau mỗi buổi trị liệu là niềm tự hào lớn nhất của TIKEY SPA.
            </p>
          </div>

          {!reviews || reviews.length === 0 ? (
            <p className="text-stone-500 text-center py-8">Chưa có đánh giá nào được phê duyệt.</p>
          ) : (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4 text-left">
              {reviews.map((r) => (
                <div
                  key={r.id}
                  className="p-6 rounded-2xl bg-white border border-[#e7e2d8] shadow-xs flex flex-col justify-between"
                >
                  <div>
                    {/* Star Rating & Demo Tag */}
                    <div className="flex items-center justify-between gap-1 mb-3">
                      <div className="flex items-center text-[#b8976c]">
                        {Array.from({ length: r.rating || 5 }).map((_, sIdx) => (
                          <Star key={sIdx} className="w-4 h-4 fill-current" />
                        ))}
                      </div>
                      {r.isDemo && (
                        <span className="text-[10px] text-stone-400 bg-stone-50 px-2 py-0.5 rounded border border-stone-200">
                          Minh họa
                        </span>
                      )}
                    </div>

                    <p className="text-xs sm:text-sm text-stone-600 italic leading-relaxed mb-6">
                      &ldquo;{r.comment}&rdquo;
                    </p>
                  </div>

                  <div className="pt-4 border-t border-[#e7e2d8]">
                    <h4 className="font-semibold text-stone-900 text-sm">{r.customerName}</h4>
                    {r.serviceName && (
                      <span className="text-[11px] text-[#566f5c] font-medium block truncate mt-0.5">
                        {r.serviceName}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="mt-8 text-center">
            <span className="text-[11px] text-stone-400 italic">
              * Khách hàng trải nghiệm có thể đóng góp ý kiến trực tiếp tại mục Phản hồi bên dưới.
            </span>
          </div>
        </div>
      </section>

      {/* 7. FEEDBACK & COMPLAINTS SECTION */}
      <section
        id="phan-hoi"
        aria-labelledby="feedback-heading"
        className="py-16 sm:py-24 border-b border-[#e7e2d8] bg-white scroll-mt-20"
      >
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-14">
            {/* Left Contact & Hotline Column */}
            <div className="lg:col-span-5 space-y-6">
              <div>
                <span className="text-xs uppercase tracking-widest font-semibold text-[#b8976c]">
                  Lắng nghe & Tận tâm
                </span>
                <h2 id="feedback-heading" className="text-2xl sm:text-4xl font-serif-title font-medium text-stone-900 mt-1 mb-3">
                  Phản hồi & Khiếu nại
                </h2>
                <p className="text-stone-600 text-sm leading-relaxed">
                  Chúng tôi luôn trân trọng mọi ý kiến đóng góp từ khách hàng để không ngừng nâng cao chất lượng dịch vụ.
                  Nếu bạn có bất kỳ điều gì chưa hài lòng hoặc muốn chia sẻ trải nghiệm, xin vui lòng gửi thông tin cho chúng tôi.
                </p>
              </div>

              {/* Direct Channels */}
              <div className="space-y-4 pt-2">
                {spa.phone && (
                  <div className="flex items-start gap-3.5 p-4 rounded-2xl bg-[#faf8f5] border border-[#e7e2d8]">
                    <div className="p-2.5 rounded-xl bg-[#f2f6f3] text-[#465d4c] shrink-0">
                      <Phone className="w-5 h-5" />
                    </div>
                    <div>
                      <span className="text-xs text-stone-500 block">Đường dây nóng tiếp nhận</span>
                      <a href={`tel:${spa.phone}`} className="font-semibold text-stone-900 text-sm hover:text-[#465d4c]">
                        {spa.phone}
                      </a>
                    </div>
                  </div>
                )}

                {spa.email && (
                  <div className="flex items-start gap-3.5 p-4 rounded-2xl bg-[#faf8f5] border border-[#e7e2d8]">
                    <div className="p-2.5 rounded-xl bg-[#f2f6f3] text-[#465d4c] shrink-0">
                      <Mail className="w-5 h-5" />
                    </div>
                    <div>
                      <span className="text-xs text-stone-500 block">Thư điện tử chăm sóc khách hàng</span>
                      <a href={`mailto:${spa.email}`} className="font-semibold text-stone-900 text-sm hover:text-[#465d4c]">
                        {spa.email}
                      </a>
                    </div>
                  </div>
                )}

                <div className="p-4 rounded-2xl bg-[#f2f6f3]/70 border border-[#c6d8c9]/60 text-xs text-stone-600 space-y-1.5">
                  <div className="font-semibold text-[#465d4c] flex items-center gap-1.5">
                    <CheckCircle2 className="w-4 h-4 text-[#b8976c]" />
                    <span>Cam kết bảo mật & Xử lý thỏa đáng</span>
                  </div>
                  <p>Mọi ý kiến sẽ được Ban Quản trị TIKEY SPA tiếp nhận và phản hồi chính thức trong vòng 24 giờ làm việc.</p>
                </div>
              </div>
            </div>

            {/* Right Interactive Submission Form */}
            <div className="lg:col-span-7">
              <div className="rounded-3xl p-6 sm:p-8 bg-[#faf8f5] border border-[#e7e2d8] shadow-xs">
                <h3 className="font-serif-title text-xl font-medium text-stone-900 mb-2">
                  Gửi thông tin phản hồi trực tuyến
                </h3>
                <p className="text-xs text-stone-500 mb-6">
                  Thông tin phản hồi của quý khách được bảo mật và chuyển trực tiếp tới Ban Quản lý.
                </p>

                {fbSuccess ? (
                  <div className="p-6 rounded-2xl bg-[#edf7f2] border border-[#b7e4c7] text-center space-y-3">
                    <CheckCircle2 className="w-10 h-10 text-[#2d6a4f] mx-auto" />
                    <h4 className="font-serif-title text-lg font-medium text-stone-900">
                      Gửi phản hồi thành công!
                    </h4>
                    <p className="text-xs text-stone-600 max-w-md mx-auto">
                      Cảm ơn quý khách đã dành thời gian đóng góp ý kiến. Bộ phận Quản lý của {spa.name} sẽ xem xét và liên hệ với quý khách trong thời gian sớm nhất.
                    </p>
                    <button
                      type="button"
                      onClick={() => setFbSuccess(false)}
                      className="mt-2 inline-flex items-center gap-1.5 px-4 py-2 bg-[#465d4c] text-white text-xs font-medium rounded-xl hover:bg-[#374a3c] transition-colors"
                    >
                      Gửi thêm ý kiến khác
                    </button>
                  </div>
                ) : (
                  <form onSubmit={handleFeedbackSubmit} className="space-y-4">
                    {fbError && (
                      <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-start gap-2">
                        <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-500" />
                        <span>{fbError}</span>
                      </div>
                    )}

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label htmlFor="fb-name" className="block text-xs font-semibold text-stone-700 mb-1">
                          Họ và tên <span className="text-red-500">*</span>
                        </label>
                        <input
                          id="fb-name"
                          type="text"
                          required
                          value={fbName}
                          onChange={(e) => setFbName(e.target.value)}
                          placeholder="Nguyễn Văn A"
                          className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#465d4c]/30 focus:border-[#465d4c]"
                        />
                      </div>

                      <div>
                        <label htmlFor="fb-phone" className="block text-xs font-semibold text-stone-700 mb-1">
                          Số điện thoại <span className="text-red-500">*</span>
                        </label>
                        <input
                          id="fb-phone"
                          type="tel"
                          required
                          value={fbPhone}
                          onChange={(e) => setFbPhone(e.target.value)}
                          placeholder="0912 345 678"
                          className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#465d4c]/30 focus:border-[#465d4c]"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label htmlFor="fb-email" className="block text-xs font-semibold text-stone-700 mb-1">
                          Email liên hệ (không bắt buộc)
                        </label>
                        <input
                          id="fb-email"
                          type="email"
                          value={fbEmail}
                          onChange={(e) => setFbEmail(e.target.value)}
                          placeholder="email@example.com"
                          className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#465d4c]/30 focus:border-[#465d4c]"
                        />
                      </div>

                      <div>
                        <label htmlFor="fb-type" className="block text-xs font-semibold text-stone-700 mb-1">
                          Loại phản hồi <span className="text-red-500">*</span>
                        </label>
                        <select
                          id="fb-type"
                          value={fbType}
                          onChange={(e) => setFbType(e.target.value as FeedbackType)}
                          className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#465d4c]/30 focus:border-[#465d4c]"
                        >
                          <option value="SUGGESTION">Góp ý cải thiện dịch vụ</option>
                          <option value="COMPLAINT">Khiếu nại / Chưa hài lòng</option>
                          <option value="PRAISE">Khen ngợi kỹ thuật viên</option>
                          <option value="OTHER">Ý kiến khác</option>
                        </select>
                      </div>
                    </div>

                    <div>
                      <label htmlFor="fb-booking-code" className="block text-xs font-semibold text-stone-700 mb-1">
                        Mã lịch hẹn liên quan (nếu có)
                      </label>
                      <input
                        id="fb-booking-code"
                        type="text"
                        value={fbBookingCode}
                        onChange={(e) => setFbBookingCode(e.target.value)}
                        placeholder="Ví dụ: TK-ABCDE"
                        className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-stone-300 bg-white uppercase font-mono tracking-wider focus:outline-none focus:ring-2 focus:ring-[#465d4c]/30 focus:border-[#465d4c]"
                      />
                    </div>

                    <div>
                      <label htmlFor="fb-message" className="block text-xs font-semibold text-stone-700 mb-1">
                        Nội dung phản hồi <span className="text-red-500">*</span>
                      </label>
                      <textarea
                        id="fb-message"
                        required
                        rows={4}
                        value={fbMessage}
                        onChange={(e) => setFbMessage(e.target.value)}
                        placeholder="Quý khách vui lòng mô tả chi tiết trải nghiệm hoặc góp ý để chúng tôi hỗ trợ tốt nhất..."
                        className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#465d4c]/30 focus:border-[#465d4c]"
                      />
                    </div>

                    <button
                      type="submit"
                      disabled={fbSubmitting}
                      className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 bg-[#465d4c] hover:bg-[#374a3c] text-white text-sm font-medium rounded-xl transition-all shadow-xs disabled:opacity-60"
                    >
                      <Send className="w-4 h-4" />
                      <span>{fbSubmitting ? 'Đang gửi...' : 'Gửi phản hồi'}</span>
                    </button>
                  </form>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 8. PUBLIC BOOKING LOOKUP SECTION */}
      <section id="tra-cuu" className="py-16 sm:py-24 border-b border-[#e7e2d8] bg-[#fcfaf7] scroll-mt-20">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <PublicBookingLookup slug={slug} spaName={spa.name} spaPhone={spa.phone} />
        </div>
      </section>

      {/* 9. ONLINE BOOKING FLOW SECTION */}
      <section
        id="booking"
        aria-labelledby="booking-heading"
        className="py-16 sm:py-24 border-b border-[#e7e2d8] bg-[#faf8f5] scroll-mt-20"
      >
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-10">
            <span className="text-xs uppercase tracking-widest font-semibold text-[#566f5c]">
              Trực tuyến 24/7
            </span>
            <h2 id="booking-heading" className="text-2xl sm:text-4xl font-serif-title font-medium text-stone-900 mt-1 mb-2">
              Đặt lịch hẹn tại {spa.name}
            </h2>
            <p className="text-stone-500 text-sm max-w-md mx-auto">
              Chỉ với vài thao tác đơn giản, bạn sẽ chọn được thời gian và chuyên viên ưng ý nhất.
            </p>
          </div>

          {children}
        </div>
      </section>

      {/* 10. FOOTER */}
      <footer id="lien-he" className="mt-auto bg-stone-900 text-stone-300">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-14">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 pb-10 border-b border-stone-800">
            <div className="md:col-span-1">
              <div className="flex items-center gap-2 mb-3 text-white">
                <Sparkles className="w-5 h-5 text-[#b8976c]" />
                <span className="font-serif-title text-xl font-bold">{spa.name}</span>
              </div>
              <p className="text-xs text-stone-400 leading-relaxed mb-4">
                Hệ thống đặt lịch & chăm sóc sức khỏe tiêu chuẩn wellness. Mang đến không gian tĩnh tại và các liệu pháp phục hồi thân tâm chuyên sâu.
              </p>
              <div className="text-xs text-stone-400">
                <span className="text-stone-200 font-medium">Giờ mở cửa:</span> Thứ 2 – Chủ Nhật (08:30 – 21:00)
              </div>
            </div>

            <div className="md:col-span-1">
              <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">
                Thông tin liên hệ
              </h3>
              <ul className="space-y-3 text-xs text-stone-300">
                {spa.address && (
                  <li className="flex items-start gap-2.5">
                    <MapPin className="w-4 h-4 text-[#b8976c] shrink-0 mt-0.5" aria-hidden="true" />
                    <span>{spa.address}</span>
                  </li>
                )}
                {spa.phone && (
                  <li className="flex items-center gap-2.5">
                    <Phone className="w-4 h-4 text-[#b8976c] shrink-0" aria-hidden="true" />
                    <a href={`tel:${spa.phone}`} className="hover:text-white transition-colors">
                      {spa.phone}
                    </a>
                  </li>
                )}
                {spa.email && (
                  <li className="flex items-center gap-2.5">
                    <Mail className="w-4 h-4 text-[#b8976c] shrink-0" aria-hidden="true" />
                    <a href={`mailto:${spa.email}`} className="hover:text-white transition-colors">
                      {spa.email}
                    </a>
                  </li>
                )}
              </ul>
            </div>

            <div className="md:col-span-1">
              <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">
                Lối tắt trang
              </h3>
              <div className="flex flex-col gap-2.5 text-xs">
                <a href="#gioi-thieu" className="hover:text-white transition-colors">
                  Giới thiệu & Triết lý
                </a>
                <a href="#dich-vu" className="hover:text-white transition-colors">
                  Dịch vụ nổi bật
                </a>
                <a href="#doi-ngu" className="hover:text-white transition-colors">
                  Đội ngũ chuyên viên
                </a>
                <a href="#goc-cham-soc" className="hover:text-white transition-colors">
                  Góc chăm sóc (Bài viết)
                </a>
                <a href="#danh-gia" className="hover:text-white transition-colors">
                  Đánh giá từ khách hàng
                </a>
              </div>
            </div>

            <div className="md:col-span-1">
              <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">
                Hỗ trợ & Quản trị
              </h3>
              <div className="flex flex-col gap-2.5 text-xs">
                <a href="#phan-hoi" className="text-[#b8976c] hover:underline font-medium">
                  Phản hồi & Khiếu nại dịch vụ
                </a>
                <a href="#tra-cuu" className="hover:text-white transition-colors">
                  Tra cứu tiến độ lịch hẹn
                </a>
                <a href="#booking" className="hover:text-white transition-colors">
                  Đặt lịch trực tuyến
                </a>
              </div>

              <div className="pt-4 mt-4 border-t border-stone-800">
                <Link
                  to="/login"
                  className="inline-flex items-center gap-2 text-xs font-semibold text-[#b8976c] hover:underline"
                >
                  <LogIn className="w-3.5 h-3.5" aria-hidden="true" />
                  <span>Đăng nhập Quản trị / Nhân viên</span>
                </Link>
              </div>
            </div>
          </div>

          <div className="pt-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-stone-400">
            <p>© {new Date().getFullYear()} {spa.name}. Bảo lưu mọi quyền.</p>
            <p className="flex items-center gap-1.5">
              <span>Hệ thống vận hành bởi</span>
              <strong className="text-stone-300">TIKEY SPA System</strong>
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
