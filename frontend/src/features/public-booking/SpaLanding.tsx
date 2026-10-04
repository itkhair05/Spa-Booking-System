import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { getPublicServices, getPublicStaff } from '../../lib/api/publicBooking';
import { formatCurrency } from '../../lib/format';
import type { PublicSpaInfoResponse, PublicServiceResponse, PublicStaffResponse } from '../../types/publicBooking';
import { PublicBookingLookup } from './PublicBookingLookup';
import { TikeyHeroCanvas } from './TikeyHeroCanvas';
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
  LogIn
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

export function SpaLanding({ slug, spa, children }: SpaLandingProps) {
  const [services, setServices] = useState<PublicServiceResponse[] | null>(null);
  const [servicesFailed, setServicesFailed] = useState(false);
  const [staffList, setStaffList] = useState<PublicStaffResponse[] | null>(null);
  const [staffFailed, setStaffFailed] = useState(false);

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

    return () => {
      cancelled = true;
    };
  }, [slug]);

  const showServices = services !== null && services.length > 0;
  const showStaff = staffList !== null && staffList.length > 0;

  return (
    <div className="min-h-screen bg-[#faf8f5] flex flex-col font-sans text-stone-800">
      {/* Top Bar — Restrained Glassmorphism */}
      <header className="sticky top-0 z-30 bg-[#faf8f5]/85 backdrop-blur-md border-b border-[#e7e2d8]/80 transition-colors">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 h-16 sm:h-20 flex items-center justify-between gap-4">
          {/* Brand */}
          <div className="flex items-center gap-2.5">
            <span className="w-8 h-8 rounded-full bg-[#f2f6f3] border border-[#c6d8c9] flex items-center justify-center text-[#465d4c]">
              <Sparkles className="w-4 h-4" aria-hidden="true" />
            </span>
            <div className="flex flex-col">
              <span className="font-serif-title font-bold text-lg text-[var(--color-champagne-500)] tracking-tight leading-tight">
                {spa.name}
              </span>
              <span className="text-[10px] text-stone-500 uppercase tracking-widest font-semibold">
                Spa & Wellness
              </span>
            </div>
          </div>

          {/* Quick Nav Links */}
          <nav className="hidden md:flex items-center gap-6 text-xs font-semibold text-stone-600 uppercase tracking-wider">
            {showServices && (
              <a href="#dich-vu" className="hover:text-[#465d4c] transition-colors">
                Dịch vụ
              </a>
            )}
            {showStaff && (
              <a href="#doi-ngu" className="hover:text-[#465d4c] transition-colors">
                Đội ngũ
              </a>
            )}
            <a href="#tra-cuu" className="hover:text-[#465d4c] transition-colors">
              Tra cứu lịch
            </a>
            <a href="#lien-he" className="hover:text-[#465d4c] transition-colors">
              Liên hệ
            </a>
          </nav>

          {/* Header Action Button */}
          <div className="flex items-center gap-2.5">
            <Link
              to="/login"
              className="hidden sm:inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium text-stone-600 hover:text-stone-900 rounded-lg hover:bg-stone-100 transition-colors"
              title="Đăng nhập hệ thống"
            >
              <LogIn className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Đăng nhập</span>
            </Link>

            <a
              href="#booking"
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#465d4c] hover:bg-[#374a3c] text-white text-xs sm:text-sm font-medium rounded-xl transition-colors shadow-xs"
            >
              <span>Đặt lịch ngay</span>
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </a>
          </div>
        </div>
      </header>

      {/* Hero Section with Three.js Organic Visual */}
      <section className="relative overflow-hidden pt-12 sm:pt-20 pb-16 sm:pb-24 border-b border-[#e7e2d8]">
        {/* Three.js Organic Wellness 3D Form */}
        <TikeyHeroCanvas />

        {/* Subtle Organic Background Aura (secondary ambient glow) */}
        <div className="absolute top-0 right-1/4 w-96 h-96 bg-[#e2ece4]/40 rounded-full blur-3xl pointer-events-none -z-10" />
        <div className="absolute bottom-0 left-1/4 w-80 h-80 bg-[#f6efe2]/50 rounded-full blur-3xl pointer-events-none -z-10" />

        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/70 backdrop-blur-md border border-white/60 shadow-xs text-[#374a3c] text-xs font-semibold uppercase tracking-wider mb-5">
            <Sparkles className="w-3.5 h-3.5 text-[#566f5c]" aria-hidden="true" />
            <span>Nghệ thuật chăm sóc sức khỏe & sắc đẹp</span>
          </div>

          <h1 className="text-3xl sm:text-5xl lg:text-6xl font-serif-title font-medium tracking-tight text-stone-900 max-w-3xl mx-auto mb-6 leading-tight">
            Khoảnh khắc tĩnh tại, phục hồi trọn vẹn tại <span className="italic text-[#465d4c]">{spa.name}</span>
          </h1>

          {spa.address && (
            <p className="flex items-center justify-center gap-2 text-sm text-stone-600 mb-4">
              <MapPin className="w-4 h-4 text-[#566f5c] shrink-0" aria-hidden="true" />
              <span>{spa.address}</span>
            </p>
          )}

          <p className="text-stone-600 text-base sm:text-lg max-w-2xl mx-auto mb-10 leading-relaxed">
            Trải nghiệm các liệu pháp massage, trị liệu da và chăm sóc chuyên sâu được thiết kế riêng cho bạn.
            Đặt lịch chỉ trong 2 phút — không cần đăng ký tài khoản.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4 mb-14">
            <a
              href="#booking"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-7 py-3.5 bg-[#465d4c] text-white font-medium rounded-xl hover:bg-[#374a3c] transition-all shadow-md hover:shadow-lg max-sm:min-h-11"
            >
              <span>Đặt lịch trực tuyến</span>
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </a>

            {showServices && (
              <a
                href="#dich-vu"
                className="w-full sm:w-auto inline-flex items-center justify-center px-6 py-3.5 bg-white/80 backdrop-blur-md border border-[#d8d1c3]/70 text-stone-700 font-medium rounded-xl hover:bg-white hover:text-stone-900 transition-colors max-sm:min-h-11 shadow-xs"
              >
                Khám phá dịch vụ
              </a>
            )}

            <a
              href="#tra-cuu"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-[#f6efe2]/80 backdrop-blur-md text-[#9e7d52] font-medium rounded-xl hover:bg-[#ebdcc8] transition-colors max-sm:min-h-11 border border-[#ebdcc8]/50"
            >
              <Search className="w-4 h-4" aria-hidden="true" />
              <span>Tra cứu lịch hẹn</span>
            </a>
          </div>

          {/* Highlights — Restrained Liquid Glass Cards */}
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

      {/* Services Section with Demo Images */}
      {(showServices || servicesFailed) && (
        <section
          id="dich-vu"
          aria-labelledby="services-heading"
          className="py-14 sm:py-20 border-b border-[#e7e2d8] bg-white scroll-mt-20"
        >
          <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-2xl mx-auto mb-12">
              <span className="text-xs uppercase tracking-widest font-semibold text-[#566f5c]">
                Liệu trình chuyên sâu
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
            ) : (
              <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                {services!.map((service) => (
                  <div
                    key={service.id}
                    className="flex flex-col rounded-2xl border border-[#e7e2d8] bg-[#faf8f5] hover:bg-white hover:border-[#c6d8c9] hover:shadow-md transition-all group overflow-hidden"
                  >
                    {service.imageUrl && (
                      <div className="h-44 w-full overflow-hidden bg-stone-100">
                        <img
                          src={service.imageUrl}
                          alt={service.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                      </div>
                    )}
                    <div className="p-6 flex flex-col grow">
                      <div className="flex justify-between items-start gap-3 mb-2.5">
                        <h3 className="font-serif-title font-medium text-lg text-stone-900 group-hover:text-[#465d4c] transition-colors">
                          {service.name}
                        </h3>
                        <span className="font-semibold text-stone-900 shrink-0 text-base">
                          {formatCurrency(service.price)}
                        </span>
                      </div>

                      {service.description ? (
                        <p className="text-xs text-stone-600 mb-5 line-clamp-3 leading-relaxed">
                          {service.description}
                        </p>
                      ) : (
                        <p className="text-xs text-stone-400 italic mb-5">
                          Liệu trình chăm sóc toàn diện tại spa.
                        </p>
                      )}

                      <div className="mt-auto pt-4 border-t border-[#e7e2d8] flex items-center justify-between text-xs text-stone-500">
                        <span className="flex items-center gap-1.5 font-medium">
                          <Clock className="w-4 h-4 text-[#566f5c]" aria-hidden="true" />
                          {service.durationMinutes} phút
                        </span>
                        <a
                          href="#booking"
                          className="inline-flex items-center gap-1 font-semibold text-[#465d4c] hover:underline"
                        >
                          Đặt dịch vụ này
                          <ArrowRight className="w-3.5 h-3.5" />
                        </a>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {/* Staff Section with Persistent Avatars */}
      {(showStaff || staffFailed) && (
        <section
          id="doi-ngu"
          aria-labelledby="staff-heading"
          className="py-14 sm:py-20 border-b border-[#e7e2d8] scroll-mt-20 bg-[#faf8f5]"
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
                      className="w-12 h-12 rounded-full overflow-hidden bg-[#f2f6f3] border border-[#c6d8c9] flex items-center justify-center text-sm font-semibold text-[#465d4c] shrink-0"
                    >
                      {member.avatarUrl ? (
                        <img src={member.avatarUrl} alt={member.name} className="w-full h-full object-cover" />
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

      {/* Public Booking Lookup Section */}
      <section id="tra-cuu" className="py-14 sm:py-20 border-b border-[#e7e2d8] bg-white scroll-mt-20">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <PublicBookingLookup slug={slug} spaName={spa.name} spaPhone={spa.phone} />
        </div>
      </section>

      {/* Online Booking Flow Section */}
      <section
        id="booking"
        aria-labelledby="booking-heading"
        className="py-14 sm:py-20 border-b border-[#e7e2d8] bg-[#faf8f5] scroll-mt-20"
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

      {/* Footer / Contact */}
      <footer id="lien-he" className="mt-auto bg-stone-900 text-stone-300">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-14">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-10 pb-10 border-b border-stone-800">
            {/* Brand column */}
            <div>
              <div className="flex items-center gap-2 mb-3 text-white">
                <Sparkles className="w-5 h-5 text-[#b8976c]" />
                <span className="font-serif-title text-xl font-bold">{spa.name}</span>
              </div>
              <p className="text-xs text-stone-400 leading-relaxed mb-4">
                Hệ thống đặt lịch & quản lý spa tiêu chuẩn wellness. Cung cấp dịch vụ chăm sóc sắc đẹp, thư giãn và phục hồi năng lượng chuyên sâu.
              </p>
              <div className="text-xs text-stone-400">
                <span className="text-stone-200 font-medium">Giờ mở cửa:</span> Thứ 2 – Chủ Nhật (08:30 – 21:00)
              </div>
            </div>

            {/* Contact details */}
            <div>
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

            {/* Quick links & Portal login */}
            <div>
              <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">
                Lối tắt & Cổng quản trị
              </h3>
              <div className="flex flex-col gap-2.5 text-xs">
                <a href="#dich-vu" className="hover:text-white transition-colors">
                  Danh mục dịch vụ
                </a>
                <a href="#doi-ngu" className="hover:text-white transition-colors">
                  Đội ngũ chuyên viên
                </a>
                <a href="#tra-cuu" className="hover:text-white transition-colors">
                  Tra cứu lịch hẹn
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
