import { useState, useEffect, useMemo } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { getPublicServices } from '../../lib/api/publicBooking';
import { formatCurrency } from '../../lib/format';
import type { PublicServiceResponse } from '../../types/publicBooking';
import { Clock, AlertCircle, CheckCircle2, Sparkles, ChevronRight } from 'lucide-react';
import { resolveMediaUrl } from '../../lib/api/apiConfig';

export function StepServices() {
  const { slug, state, updateState, setStep } = useSpaBooking();
  const [services, setServices] = useState<PublicServiceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');

  useEffect(() => {
    let isMounted = true;
    getPublicServices(slug)
      .then((data) => {
        if (isMounted) {
          setServices(data);
          setLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          setError('Không thể tải danh sách dịch vụ.');
          setLoading(false);
        }
      });
    return () => {
      isMounted = false;
    };
  }, [slug]);

  // Extract distinct category names
  const categories = useMemo(() => {
    const cats = new Set<string>();
    services.forEach((s) => {
      if (s.categoryName && s.categoryName.trim()) {
        cats.add(s.categoryName.trim());
      }
    });
    return Array.from(cats);
  }, [services]);

  // Group services by category
  const groupedServices = useMemo(() => {
    const groups: { [key: string]: PublicServiceResponse[] } = {};
    services.forEach((service) => {
      const cat = service.categoryName && service.categoryName.trim() ? service.categoryName.trim() : 'Dịch vụ khác';
      if (!groups[cat]) {
        groups[cat] = [];
      }
      groups[cat].push(service);
    });
    return groups;
  }, [services]);

  const handleSelect = (service: PublicServiceResponse) => {
    if (state.service?.id !== service.id) {
      updateState({
        service,
        time: '',
      });
    } else {
      updateState({ service });
    }
    setStep(2);
  };

  if (loading) {
    return (
      <div>
        <h2 className="text-xl sm:text-2xl font-serif-title font-medium text-stone-900 mb-6">Chọn dịch vụ</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="p-5 rounded-2xl border border-stone-200 bg-white animate-pulse">
              <div className="flex justify-between items-start mb-3">
                <div className="h-5 w-32 bg-stone-100 rounded" />
                <div className="h-5 w-20 bg-stone-100 rounded" />
              </div>
              <div className="h-4 w-full bg-stone-100 rounded mb-2" />
              <div className="h-4 w-24 bg-stone-100 rounded" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-8 text-center text-rose-500">
        <AlertCircle className="w-8 h-8 mx-auto mb-2" aria-hidden="true" />
        <p>{error}</p>
      </div>
    );
  }

  if (services.length === 0) {
    return (
      <div className="py-12 text-center text-stone-500">
        Hiện chưa có dịch vụ để đặt lịch.
      </div>
    );
  }

  const categoryKeys = Object.keys(groupedServices);
  const displayedCategoryKeys = selectedCategory === 'ALL'
    ? categoryKeys
    : categoryKeys.filter((cat) => cat === selectedCategory);

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
        <div>
          <h2 className="text-xl sm:text-2xl font-serif-title font-medium text-stone-900">Chọn dịch vụ</h2>
          <p className="text-xs text-stone-500 mt-0.5">Chọn liệu trình chăm sóc sức khỏe & sắc đẹp phù hợp với bạn</p>
        </div>

        {/* Category Filter Pills */}
        {categories.length > 0 && (
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full no-scrollbar">
            <button
              type="button"
              onClick={() => setSelectedCategory('ALL')}
              className={`px-3.5 py-2 min-h-9 text-xs font-medium rounded-full transition-all whitespace-nowrap cursor-pointer flex items-center justify-center ${
                selectedCategory === 'ALL'
                  ? 'bg-[#465d4c] text-white shadow-xs'
                  : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              }`}
            >
              Tất cả ({services.length})
            </button>
            {categories.map((cat) => {
              const count = groupedServices[cat]?.length || 0;
              return (
                <button
                  key={cat}
                  type="button"
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-3.5 py-2 min-h-9 text-xs font-medium rounded-full transition-all whitespace-nowrap cursor-pointer flex items-center justify-center ${
                    selectedCategory === cat
                      ? 'bg-[#465d4c] text-white shadow-xs'
                      : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                  }`}
                >
                  {cat} ({count})
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Grouped Services List */}
      <div className="space-y-8">
        {displayedCategoryKeys.map((categoryName) => {
          const catServices = groupedServices[categoryName] || [];
          if (catServices.length === 0) return null;

          return (
            <div key={categoryName} className="space-y-3">
              <div className="flex items-center gap-2 border-b border-stone-200/80 pb-2">
                <span className="w-2 h-2 rounded-full bg-[#8a704c]" />
                <h3 className="font-serif-title font-medium text-stone-900 text-base sm:text-lg tracking-tight">
                  {categoryName}
                </h3>
                <span className="text-xs text-stone-400 font-normal">({catServices.length} liệu trình)</span>
              </div>

              <div className="grid gap-3.5 sm:grid-cols-2">
                {catServices.map((service) => {
                  const selected = state.service?.id === service.id;

                  return (
                    <div
                      key={service.id}
                      onClick={() => handleSelect(service)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          handleSelect(service);
                        }
                      }}
                      aria-pressed={selected}
                      className={`group relative flex flex-col justify-between p-4 sm:p-5 rounded-2xl border transition-all text-left bg-white cursor-pointer ${
                        selected
                          ? 'border-[#465d4c] ring-2 ring-[#465d4c]/20 bg-stone-50/60 shadow-sm'
                          : 'border-stone-200/90 hover:border-stone-300 hover:shadow-xs'
                      }`}
                    >
                      {service.isFeatured && (
                        <span className="absolute -top-2.5 right-4 inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-50 border border-amber-200 text-amber-800 text-[10px] font-semibold tracking-wider uppercase">
                          <Sparkles className="w-3 h-3 text-amber-600" />
                          Nổi bật
                        </span>
                      )}

                      <div>
                        <div className="flex gap-3 items-start">
                          {service.imageUrl && (
                            <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-xl overflow-hidden shrink-0 bg-stone-100 border border-stone-200">
                              <img src={resolveMediaUrl(service.imageUrl)} alt={service.name} className="w-full h-full object-cover" />
                            </div>
                          )}

                          <div className="flex-1 min-w-0">
                            <div className="flex items-start justify-between gap-2 mb-1">
                              <h4 className="font-medium text-stone-900 text-sm sm:text-base leading-snug">
                                {service.name}
                              </h4>
                            </div>

                            <div className="flex items-baseline gap-2 mb-2">
                              <span className="text-sm sm:text-base font-semibold text-[#8a704c]">
                                {formatCurrency(service.price)}
                              </span>
                              <span className="inline-flex items-center text-[11px] text-stone-500 bg-stone-100 px-2 py-0.5 rounded-md">
                                <Clock className="w-3 h-3 mr-1 text-stone-400" />
                                {service.durationMinutes} phút
                              </span>
                            </div>

                            {service.description && (
                              <p className="text-xs text-stone-600 line-clamp-2 leading-relaxed">
                                {service.description}
                              </p>
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="mt-4 pt-3 border-t border-stone-100 flex items-center justify-between">
                        <span className="text-xs text-stone-400">
                          {selected ? 'Đã chọn dịch vụ này' : 'Nhấn để tiếp tục'}
                        </span>
                        <span
                          aria-hidden="true"
                          className={`inline-flex items-center gap-1 px-3.5 py-1.5 min-h-9 text-xs font-medium rounded-xl transition-colors pointer-events-none select-none ${
                            selected
                              ? 'bg-[#465d4c] text-white'
                              : 'bg-stone-100 text-stone-700 group-hover:bg-[#465d4c] group-hover:text-white'
                          }`}
                        >
                          {selected ? (
                            <>
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              Đã chọn
                            </>
                          ) : (
                            <>
                              Chọn
                              <ChevronRight className="w-3.5 h-3.5" />
                            </>
                          )}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
