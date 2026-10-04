import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { getPublicServices, getPublicStaff } from '../../lib/api/publicBooking';
import { formatCurrency } from '../../lib/format';
import type { PublicSpaInfoResponse, PublicServiceResponse, PublicStaffResponse } from '../../types/publicBooking';
import { MapPin, Phone, Mail, Clock, CalendarCheck, UserRound, PhoneCall, ArrowRight } from 'lucide-react';

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
    let isMounted = true;
    getPublicServices(slug)
      .then((data) => {
        if (isMounted) setServices(data);
      })
      .catch(() => {
        if (isMounted) setServicesFailed(true);
      });
    getPublicStaff(slug)
      .then((data) => {
        if (isMounted) setStaffList(data);
      })
      .catch(() => {
        if (isMounted) setStaffFailed(true);
      });
    return () => {
      isMounted = false;
    };
  }, [slug]);

  const showServices = services !== null && services.length > 0;
  const showStaff = staffList !== null && staffList.length > 0;

  return (
    <div className="flex flex-col min-h-screen bg-stone-50 font-sans text-stone-800">
      {/* Header */}
      <header className="sticky top-0 z-20 bg-white/90 backdrop-blur border-b border-stone-200">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          <span className="font-medium text-stone-900 truncate">{spa.name}</span>
          <div className="flex items-center gap-2 sm:gap-4 shrink-0">
            {spa.phone && (
              <a
                href={`tel:${spa.phone}`}
                className="hidden sm:flex items-center gap-1.5 text-sm text-stone-500 hover:text-stone-900 transition-colors"
              >
                <Phone className="w-4 h-4" aria-hidden="true" />
                {spa.phone}
              </a>
            )}
            <a
              href="#booking"
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-stone-900 text-white text-sm font-medium rounded-xl hover:bg-stone-800 transition-colors max-sm:min-h-11"
            >
              Đặt lịch
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </a>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="max-w-3xl mx-auto w-full px-4 sm:px-6 lg:px-8 pt-12 sm:pt-20 pb-10 sm:pb-16">
        <p className="text-sm font-medium text-brand-700 uppercase tracking-wider mb-4">
          Đặt lịch trực tuyến
        </p>
        <h1 className="text-3xl sm:text-5xl font-medium tracking-tight text-stone-900 mb-5">
          {spa.name}
        </h1>
        {spa.address && (
          <p className="flex items-start gap-2 text-stone-500 mb-2">
            <MapPin className="w-5 h-5 shrink-0 mt-0.5 text-stone-400" aria-hidden="true" />
            {spa.address}
          </p>
        )}
        <p className="text-stone-500 max-w-xl mb-8">
          Chọn dịch vụ, nhân viên và khung giờ phù hợp — hoàn tất đặt lịch trong vài phút.
        </p>

        <div className="flex flex-col sm:flex-row gap-3 mb-12">
          <a
            href="#booking"
            className="inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-stone-900 text-white font-medium rounded-xl hover:bg-stone-800 transition-colors max-sm:min-h-11"
          >
            Đặt lịch ngay
            <ArrowRight className="w-5 h-5" aria-hidden="true" />
          </a>
          {showServices && (
            <a
              href="#dich-vu"
              className="inline-flex items-center justify-center px-6 py-3.5 bg-white border border-stone-200 text-stone-700 font-medium rounded-xl hover:bg-stone-100 hover:text-stone-900 transition-colors max-sm:min-h-11"
            >
              Xem dịch vụ
            </a>
          )}
        </div>

        <ul className="grid sm:grid-cols-3 gap-4 text-sm text-stone-600">
          <li className="flex items-center gap-3">
            <CalendarCheck className="w-5 h-5 text-stone-400 shrink-0" aria-hidden="true" />
            Chọn ngày giờ phù hợp
          </li>
          <li className="flex items-center gap-3">
            <UserRound className="w-5 h-5 text-stone-400 shrink-0" aria-hidden="true" />
            Không cần tạo tài khoản
          </li>
          <li className="flex items-center gap-3">
            <PhoneCall className="w-5 h-5 text-stone-400 shrink-0" aria-hidden="true" />
            Spa liên hệ xác nhận
          </li>
        </ul>
      </section>

      {/* Services */}
      {(showServices || servicesFailed) && (
        <section
          id="dich-vu"
          aria-labelledby="services-heading"
          className="border-t border-stone-200 bg-white scroll-mt-20"
        >
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14">
            <h2 id="services-heading" className="text-2xl sm:text-3xl font-medium tracking-tight text-stone-900 mb-8">
              Dịch vụ
            </h2>

            {servicesFailed ? (
              <p className="text-stone-500">Không thể tải danh sách dịch vụ.</p>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {services!.map((service) => (
                  <div
                    key={service.id}
                    className="flex flex-col p-5 rounded-2xl border border-stone-200 bg-stone-50"
                  >
                    <div className="flex justify-between items-start gap-3 mb-2">
                      <h3 className="font-medium text-stone-900">{service.name}</h3>
                      <span className="font-medium text-stone-900 shrink-0">
                        {formatCurrency(service.price)}
                      </span>
                    </div>
                    {service.description && (
                      <p className="text-sm text-stone-500 mb-4 line-clamp-2">{service.description}</p>
                    )}
                    <div className="mt-auto flex items-center gap-1.5 text-sm text-stone-500">
                      <Clock className="w-4 h-4 text-stone-400" aria-hidden="true" />
                      {service.durationMinutes} phút
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {/* Team */}
      {(showStaff || staffFailed) && (
        <section
          id="doi-ngu"
          aria-labelledby="staff-heading"
          className="border-t border-stone-200 scroll-mt-20"
        >
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14">
            <h2 id="staff-heading" className="text-2xl sm:text-3xl font-medium tracking-tight text-stone-900 mb-8">
              Đội ngũ
            </h2>

            {staffFailed ? (
              <p className="text-stone-500">Không thể tải danh sách nhân viên.</p>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {staffList!.map((member) => (
                  <div key={member.id} className="flex items-center gap-4 p-4 rounded-2xl border border-stone-200 bg-white">
                    <span
                      aria-hidden="true"
                      className="w-11 h-11 rounded-full bg-stone-100 border border-stone-200 flex items-center justify-center text-sm font-medium text-stone-600 shrink-0"
                    >
                      {initials(member.name)}
                    </span>
                    <span className="font-medium text-stone-900">{member.name}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {/* Booking */}
      <section
        id="booking"
        aria-labelledby="booking-heading"
        className="border-t border-stone-200 bg-white scroll-mt-20"
      >
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14">
          <h2 id="booking-heading" className="text-2xl sm:text-3xl font-medium tracking-tight text-stone-900 mb-2">
            Đặt lịch hẹn
          </h2>
          <p className="text-stone-500 mb-8">Hoàn tất các bước dưới đây để đặt lịch.</p>
          {children}
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-auto border-t border-stone-200 bg-stone-50">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex flex-col sm:flex-row sm:items-start justify-between gap-6">
          <div>
            <p className="font-medium text-stone-900 mb-3">{spa.name}</p>
            <ul className="space-y-2 text-sm text-stone-500">
              {spa.address && (
                <li className="flex items-start gap-2">
                  <MapPin className="w-4 h-4 shrink-0 mt-0.5 text-stone-400" aria-hidden="true" />
                  {spa.address}
                </li>
              )}
              {spa.phone && (
                <li className="flex items-center gap-2">
                  <Phone className="w-4 h-4 shrink-0 text-stone-400" aria-hidden="true" />
                  <a href={`tel:${spa.phone}`} className="hover:text-stone-900 transition-colors">
                    {spa.phone}
                  </a>
                </li>
              )}
              {spa.email && (
                <li className="flex items-center gap-2">
                  <Mail className="w-4 h-4 shrink-0 text-stone-400" aria-hidden="true" />
                  <a href={`mailto:${spa.email}`} className="hover:text-stone-900 transition-colors">
                    {spa.email}
                  </a>
                </li>
              )}
            </ul>
          </div>
          <p className="text-sm text-stone-400">Powered by Spa Booking System</p>
        </div>
      </footer>
    </div>
  );
}
