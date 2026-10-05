import { useState, useEffect, useMemo } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { getPublicServices } from '../../lib/api/publicBooking';
import { formatCurrency } from '../../lib/format';
import type { PublicServiceResponse } from '../../types/publicBooking';
import { Clock, AlertCircle, CheckCircle2, Sparkles, ChevronRight, Info, X } from 'lucide-react';

export function StepServices() {
  const { slug, state, updateState, setStep } = useSpaBooking();
  const [services, setServices] = useState<PublicServiceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [previewService, setPreviewService] = useState<PublicServiceResponse | null>(null);

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

  const parseProcessSteps = (stepsJson?: string | null): string[] => {
    if (!stepsJson) return [];
    try {
      const parsed = JSON.parse(stepsJson);
      if (Array.isArray(parsed)) return parsed;
    } catch {
      return stepsJson.split('\n').filter((s) => s.trim().length > 0);
    }
    return [];
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
              className={`px-3 py-1.5 text-xs font-medium rounded-full transition-all whitespace-nowrap cursor-pointer ${
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
                  className={`px-3 py-1.5 text-xs font-medium rounded-full transition-all whitespace-nowrap cursor-pointer ${
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
                  const steps = parseProcessSteps(service.processSteps);

                  return (
                    <div
                      key={service.id}
                      className={`relative flex flex-col justify-between p-4 sm:p-5 rounded-2xl border transition-all text-left bg-white ${
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
                              <img src={service.imageUrl} alt={service.name} className="w-full h-full object-cover" />
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

                        {/* Process steps preview if available */}
                        {steps.length > 0 && (
                          <div className="mt-3 pt-2.5 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
                            <span className="text-[11px] text-stone-400">Quy trình {steps.length} bước tiêu chuẩn</span>
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                setPreviewService(service);
                              }}
                              className="text-[11px] text-[#465d4c] hover:underline font-medium inline-flex items-center gap-0.5 cursor-pointer"
                            >
                              <Info className="w-3 h-3" />
                              Xem quy trình
                            </button>
                          </div>
                        )}
                      </div>

                      <div className="mt-4 pt-3 border-t border-stone-100 flex items-center justify-between">
                        <span className="text-xs text-stone-400">
                          {selected ? 'Đã chọn dịch vụ này' : 'Nhấn để tiếp tục'}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleSelect(service)}
                          className={`inline-flex items-center gap-1 px-3.5 py-1.5 text-xs font-medium rounded-xl transition-colors cursor-pointer ${
                            selected
                              ? 'bg-[#465d4c] text-white'
                              : 'bg-stone-100 text-stone-700 hover:bg-[#465d4c] hover:text-white'
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
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      {/* Premium Treatment Detail Sheet / Process UX */}
      {previewService && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-stone-900/60 backdrop-blur-xs transition-opacity duration-200"
          role="dialog"
          aria-modal="true"
          aria-labelledby="treatment-modal-title"
          onClick={(e) => {
            if (e.target === e.currentTarget) setPreviewService(null);
          }}
        >
          <div className="bg-white rounded-3xl max-w-xl w-full shadow-2xl border border-stone-200/80 relative max-h-[90vh] overflow-y-auto flex flex-col animate-in fade-in zoom-in-95 duration-200">
            {/* Header Banner / Visual Anchor */}
            <div className="sticky top-0 z-10 bg-gradient-to-br from-stone-100 via-amber-50/60 to-stone-50 p-5 sm:p-6 border-b border-stone-200/70 backdrop-blur-xs">
              <button
                type="button"
                onClick={() => setPreviewService(null)}
                className="absolute top-4 right-4 p-2 rounded-full text-stone-400 hover:text-stone-700 hover:bg-white/80 transition-colors cursor-pointer shadow-2xs"
                aria-label="Đóng chi tiết"
              >
                <X className="w-5 h-5" />
              </button>

              <div className="flex flex-wrap items-center gap-2 mb-2.5">
                {previewService.categoryName && (
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-[#465d4c]/10 text-[#465d4c] tracking-wide uppercase">
                    {previewService.categoryName}
                  </span>
                )}
                {previewService.isFeatured && (
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-100/80 text-amber-800 tracking-wide uppercase">
                    <Sparkles className="w-3 h-3 text-amber-600" />
                    Liệu trình nổi bật
                  </span>
                )}
              </div>

              <div className="flex gap-4 items-start">
                {previewService.imageUrl && (
                  <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl overflow-hidden shrink-0 bg-stone-200 border border-stone-300/60 shadow-xs">
                    <img
                      src={previewService.imageUrl}
                      alt={previewService.name}
                      className="w-full h-full object-cover"
                    />
                  </div>
                )}
                <div className="flex-1 min-w-0 pr-6">
                  <h3 id="treatment-modal-title" className="text-lg sm:text-xl font-serif-title font-medium text-stone-900 leading-snug">
                    {previewService.name}
                  </h3>

                  <div className="flex flex-wrap items-baseline gap-3 mt-2">
                    <span className="text-lg sm:text-xl font-bold text-[#8a704c]">
                      {formatCurrency(previewService.price)}
                    </span>
                    <span className="inline-flex items-center text-xs font-medium text-stone-600 bg-white/90 px-2.5 py-1 rounded-lg border border-stone-200 shadow-2xs">
                      <Clock className="w-3.5 h-3.5 mr-1 text-stone-400" />
                      {previewService.durationMinutes} phút
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Content without nested scrollbar */}
            <div className="p-5 sm:p-6 space-y-5 text-left flex-1">
              {/* Short Description */}
              {previewService.description && (
                <div className="bg-stone-50/80 p-4 rounded-2xl border border-stone-100">
                  <h4 className="text-[11px] font-bold uppercase tracking-wider text-stone-400 mb-1.5 flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#8a704c]" />
                    Mô tả liệu trình
                  </h4>
                  <p className="text-xs sm:text-sm text-stone-700 leading-relaxed font-sans">
                    {previewService.description}
                  </p>
                </div>
              )}

              {/* Numbered Process Steps */}
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h4 className="text-[11px] font-bold uppercase tracking-wider text-stone-500 flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#465d4c]" />
                    Quy trình trị liệu tiêu chuẩn ({parseProcessSteps(previewService.processSteps).length} bước)
                  </h4>
                  <span className="text-[11px] text-stone-400 italic">Chuẩn quy chuẩn TIKEY SPA</span>
                </div>

                {parseProcessSteps(previewService.processSteps).length > 0 ? (
                  <div className="space-y-2.5">
                    {parseProcessSteps(previewService.processSteps).map((step, idx) => (
                      <div
                        key={idx}
                        className="flex items-start gap-3 p-3.5 rounded-2xl bg-white border border-stone-200/80 hover:border-stone-300 transition-colors shadow-2xs"
                      >
                        <span className="flex items-center justify-center w-6 h-6 rounded-full bg-[#465d4c] text-white text-xs font-semibold shrink-0 mt-0.5 shadow-2xs">
                          {idx + 1}
                        </span>
                        <div className="flex-1 min-w-0">
                          <p className="text-xs sm:text-sm font-medium text-stone-800 leading-snug">
                            {step.replace(/^\d+[.\s]*/, '')}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="p-4 rounded-2xl bg-stone-50 text-center border border-stone-100">
                    <p className="text-xs text-stone-500 italic">
                      Liệu trình được thực hiện theo phác đồ độc quyền TIKEY SPA, phối hợp thảo dược tự nhiên và kỹ thuật viên chuyên nghiệp.
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Footer Action Bar */}
            <div className="sticky bottom-0 z-10 p-4 sm:p-5 bg-stone-50/95 backdrop-blur-xs border-t border-stone-200/80 flex items-center justify-between gap-3 mt-auto">
              <button
                type="button"
                onClick={() => setPreviewService(null)}
                className="px-4 py-2.5 text-xs sm:text-sm font-medium rounded-xl border border-stone-200 bg-white text-stone-600 hover:bg-stone-100 hover:text-stone-900 transition-colors cursor-pointer"
              >
                Đóng
              </button>

              <button
                type="button"
                onClick={() => {
                  const s = previewService;
                  setPreviewService(null);
                  handleSelect(s);
                }}
                className="px-6 py-2.5 text-xs sm:text-sm font-semibold rounded-xl bg-[#465d4c] text-white hover:bg-[#374a3c] transition-colors shadow-xs inline-flex items-center gap-1.5 cursor-pointer"
              >
                <span>Đặt lịch ngay</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
