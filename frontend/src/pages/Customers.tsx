import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getCustomers, deleteCustomer } from '../lib/api/customers';
import { formatDateDMY } from '../lib/format';
import type { Customer } from '../types/customer';
import { CustomerForm } from './CustomerForm';
import RestrictedAccess from '../components/RestrictedAccess';
import {
  Users,
  Trash2,
  Plus,
  Search,
  X,
  Phone,
  Mail,
  CalendarCheck,
  Clock
} from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

type CustomerCategory = 'all' | 'new' | 'has_bookings' | 'recent';

const CATEGORIES: { id: CustomerCategory; label: string; description: string }[] = [
  { id: 'all', label: 'Tất cả', description: 'Toàn bộ danh bạ khách hàng' },
  { id: 'new', label: 'Khách hàng mới', description: 'Mới tạo gần đây hoặc chưa có lịch hẹn' },
  { id: 'has_bookings', label: 'Đã từng đặt lịch', description: 'Đã có ít nhất 1 cuộc hẹn' },
  { id: 'recent', label: 'Hoạt động gần đây', description: 'Ghé spa hoặc đặt lịch trong 30 ngày qua' },
];

export default function Customers() {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isFiltering, setIsFiltering] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<CustomerCategory>('all');
  const [refreshKey, setRefreshKey] = useState(0);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | undefined>(undefined);

  // Delete state
  const [deletingCustomer, setDeletingCustomer] = useState<Customer | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // 250ms debounce exclusively for typing in search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, 250);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Main data synchronization effect
  useEffect(() => {
    if (!isOwner) return;

    let isMounted = true;

    getCustomers(debouncedSearch, selectedCategory)
      .then((data) => {
        if (isMounted) {
          setCustomers(data);
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const errorObj = err as { response?: { data?: { message?: string } } };
          setError(errorObj.response?.data?.message || 'Không thể tải dữ liệu khách hàng.');
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsInitialLoading(false);
          setIsFiltering(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [debouncedSearch, isOwner, refreshKey, selectedCategory]);

  // Immediate category tab selection (no lag, no layout jump)
  const handleSelectCategory = (cat: CustomerCategory) => {
    if (cat === selectedCategory) return;
    setIsFiltering(true);
    setSelectedCategory(cat);
  };

  const refreshCustomers = useCallback(() => {
    setIsFiltering(true);
    setRefreshKey((k) => k + 1);
  }, []);

  if (!isOwner) {
    return (
      <RestrictedAccess
        shellTitle="Khách hàng"
        icon={<Users className="w-7 h-7" />}
        message="Khu vực quản lý danh bạ khách hàng toàn cơ sở chỉ dành riêng cho Quản trị viên (OWNER). Nhân viên (STAFF) có thể xem thông tin liên hệ của khách hàng trực tiếp trên từng lịch hẹn được phân công."
      />
    );
  }

  const handleOpenForm = (customer?: Customer) => {
    setEditingCustomer(customer);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingCustomer(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    refreshCustomers();
  };

  const handleDeleteClick = (customer: Customer) => {
    setDeleteError(null);
    setDeletingCustomer(customer);
  };

  const handleConfirmDelete = async () => {
    if (!deletingCustomer) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteCustomer(deletingCustomer.id);
      setDeletingCustomer(null);
      refreshCustomers();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setDeleteError(errorObj.response?.data?.message || 'Không thể xóa khách hàng vì còn lịch hẹn liên quan.');
    } finally {
      setIsDeleting(false);
    }
  };

  if (isFormOpen) {
    return (
      <AppShell title="Khách hàng">
        <PageHeader title="Quản lý Khách hàng" />
        <div className="max-w-2xl mx-auto">
          <CustomerForm
            customer={editingCustomer}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  // Generate initials for avatar
  const getInitials = (name: string) => {
    if (!name) return 'KH';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  return (
    <AppShell title="Khách hàng">
      {/* Page Header */}
      <PageHeader
        title="Quản lý Khách hàng"
        description="Hồ sơ khách hàng, lịch sử đặt hẹn và phân nhóm chăm sóc tại TIKEY SPA."
        action={
          <Button onClick={() => handleOpenForm()}>
            <Plus size={16} />
            Thêm khách hàng
          </Button>
        }
      />

      {/* Search and Category Filter Section */}
      <div className="space-y-3.5 mb-6 w-full min-w-0">
        {/* Search Bar */}
        <div className="relative w-full max-w-xl min-w-0">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400">
            <Search size={17} />
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setIsFiltering(true);
            }}
            placeholder="Tìm khách hàng theo tên hoặc số điện thoại..."
            className="w-full pl-10 pr-10 py-2.5 bg-white border border-stone-200 rounded-xl text-sm text-stone-900 placeholder:text-stone-400 focus:outline-none focus:border-[#465d4c] focus:ring-1 focus:ring-[#465d4c] transition-all shadow-2xs"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => {
                setSearchQuery('');
                setIsFiltering(true);
              }}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-stone-400 hover:text-stone-700 cursor-pointer"
              aria-label="Xóa tìm kiếm"
            >
              <X size={15} />
            </button>
          )}
        </div>

        {/* Category Filter Tabs - Stable width, no tab jitter */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full w-full min-w-0 no-scrollbar">
          {CATEGORIES.map((cat) => {
            const isActive = selectedCategory === cat.id;
            return (
              <button
                key={cat.id}
                type="button"
                onClick={() => handleSelectCategory(cat.id)}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 cursor-pointer shrink-0 ${
                  isActive
                    ? 'bg-stone-900 text-white shadow-2xs'
                    : 'bg-white border border-stone-200 text-stone-600 hover:bg-stone-50 hover:text-stone-900'
                }`}
                title={cat.description}
              >
                <span>{cat.label}</span>
                {isActive && (
                  <span className="text-[10px] bg-white/20 px-1.5 py-0.2 rounded-full font-mono">
                    {customers.length}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Initial Loading Skeleton */}
      {isInitialLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse" aria-busy="true">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="bg-white border border-stone-200 rounded-2xl p-4 space-y-3">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-stone-100" />
                <div className="space-y-1.5 flex-1">
                  <div className="h-4 bg-stone-100 rounded w-24" />
                  <div className="h-3 bg-stone-100 rounded w-16" />
                </div>
              </div>
              <div className="h-16 bg-stone-50 rounded-xl" />
            </div>
          ))}
        </div>
      )}

      {/* Error State */}
      {error && !isInitialLoading && (
        <ErrorState message={error} onRetry={refreshCustomers} />
      )}

      {/* Empty State - No customers at all */}
      {!isInitialLoading && !error && customers.length === 0 && !searchQuery && selectedCategory === 'all' && (
        <EmptyState
          icon={<Users size={22} />}
          title="Chưa có khách hàng"
          description="Thêm khách hàng đầu tiên để bắt đầu tạo lịch hẹn cho họ."
          action={
            <Button onClick={() => handleOpenForm()}>
              <Plus size={16} />
              Thêm khách hàng
            </Button>
          }
        />
      )}

      {/* Empty State - Search / Filter Returned No Results */}
      {!isInitialLoading && !error && customers.length === 0 && (searchQuery || selectedCategory !== 'all') && (
        <div className="bg-white border border-stone-200 rounded-2xl p-10 text-center max-w-md mx-auto my-6 shadow-2xs">
          <Search size={32} className="text-stone-300 mx-auto mb-3" />
          <h3 className="font-serif-title font-semibold text-stone-900 text-base mb-1">
            Không tìm thấy khách hàng
          </h3>
          <p className="text-xs text-stone-500 mb-5 leading-relaxed">
            {searchQuery
              ? `Không có khách hàng nào phù hợp với từ khóa "${searchQuery}".`
              : 'Chưa có khách hàng nào trong nhóm phân loại này.'}
          </p>
          <div className="flex justify-center gap-2">
            {searchQuery && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setSearchQuery('')}
              >
                Xóa tìm kiếm
              </Button>
            )}
            {selectedCategory !== 'all' && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => handleSelectCategory('all')}
              >
                Xem tất cả
              </Button>
            )}
          </div>
        </div>
      )}

      {/* Customer List: Desktop Table + Mobile/Tablet Responsive Cards */}
      {!isInitialLoading && !error && customers.length > 0 && (
        <div className={`transition-opacity duration-150 ${isFiltering ? 'opacity-60 pointer-events-none' : 'opacity-100'}`}>
          {/* Desktop Table View (>= 1024px) */}
          <div className="hidden lg:block bg-white border border-stone-200 rounded-2xl overflow-hidden shadow-2xs">
            <table className="w-full text-left text-sm text-stone-600 border-collapse">
              <thead className="bg-stone-50/80 border-b border-stone-200 text-[11px] font-semibold text-stone-500 uppercase tracking-wider">
                <tr>
                  <th className="py-3.5 px-4 font-semibold">Khách hàng</th>
                  <th className="py-3.5 px-4 font-semibold">Số điện thoại</th>
                  <th className="py-3.5 px-4 font-semibold">Email</th>
                  <th className="py-3.5 px-4 font-semibold text-center">Số cuộc hẹn</th>
                  <th className="py-3.5 px-4 font-semibold">Lần ghé / Lịch gần nhất</th>
                  <th className="py-3.5 px-4 font-semibold text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {customers.map((customer) => {
                  const hasBookings = (customer.totalBookings ?? 0) > 0;
                  return (
                    <tr key={customer.id} className="hover:bg-stone-50/60 transition-colors">
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-xl bg-stone-100 text-stone-700 font-semibold text-xs flex items-center justify-center shrink-0 border border-stone-200/60">
                            {getInitials(customer.name)}
                          </div>
                          <div className="min-w-0">
                            <p className="font-semibold text-stone-900 text-sm truncate max-w-xs">{customer.name}</p>
                            {customer.isActive !== undefined && (
                              <span className={`inline-block text-[10px] px-1.5 py-0.2 rounded font-medium mt-0.5 ${
                                customer.isActive ? 'text-emerald-700 bg-emerald-50' : 'text-stone-500 bg-stone-100'
                              }`}>
                                {customer.isActive ? 'Hoạt động' : 'Ngừng'}
                              </span>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-xs font-mono">
                        {customer.phone ? (
                          <a href={`tel:${customer.phone}`} className="hover:text-stone-900 hover:underline">
                            {customer.phone}
                          </a>
                        ) : (
                          <span className="text-stone-400 italic">Chưa có</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-xs">
                        {customer.email ? (
                          <a href={`mailto:${customer.email}`} className="hover:text-stone-900 truncate block max-w-[200px]" title={customer.email}>
                            {customer.email}
                          </a>
                        ) : (
                          <span className="text-stone-400 italic">Chưa có</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <span className={`inline-flex items-center text-xs font-medium px-2.5 py-0.5 rounded-full ${
                          hasBookings
                            ? 'bg-emerald-50 text-emerald-800 border border-emerald-200/60 font-semibold'
                            : 'bg-stone-100 text-stone-600'
                        }`}>
                          {customer.totalBookings ?? 0}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-xs text-stone-500">
                        {customer.lastVisit ? (
                          <span>{formatDateDMY(customer.lastVisit)}</span>
                        ) : customer.lastBookingAt ? (
                          <span>{formatDateDMY(customer.lastBookingAt)}</span>
                        ) : (
                          <span className="text-stone-400 italic">—</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="inline-flex items-center gap-1.5">
                          <Button variant="secondary" size="sm" onClick={() => handleOpenForm(customer)}>
                            Chỉnh sửa
                          </Button>
                          <Button
                            variant="danger-outline"
                            size="sm"
                            onClick={() => handleDeleteClick(customer)}
                            aria-label={`Xóa khách hàng ${customer.name}`}
                          >
                            <Trash2 size={14} aria-hidden="true" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Mobile / Tablet Cards View (< 1024px) */}
          <div className="block lg:hidden space-y-3 w-full min-w-0">
            {customers.map((customer) => {
              const hasBookings = (customer.totalBookings ?? 0) > 0;
              return (
                <div
                  key={customer.id}
                  className="bg-white border border-stone-200/80 rounded-2xl p-3.5 sm:p-4 shadow-2xs space-y-3 w-full min-w-0"
                >
                  {/* Card Header: Avatar + Name + Badges */}
                  <div className="flex items-start justify-between gap-2.5 min-w-0">
                    <div className="flex items-center gap-2.5 min-w-0 flex-1">
                      <div className="w-9 h-9 rounded-xl bg-stone-100 text-stone-700 font-semibold text-xs flex items-center justify-center shrink-0 border border-stone-200/60">
                        {getInitials(customer.name)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <h4 className="font-semibold text-sm text-stone-900 truncate" title={customer.name}>
                          {customer.name}
                        </h4>
                        <div className="flex flex-wrap items-center gap-1.5 mt-0.5">
                          <span className={`inline-flex items-center text-[10px] font-medium px-2 py-0.2 rounded-md ${
                            hasBookings
                              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200/60'
                              : 'bg-stone-100 text-stone-600 border border-stone-200/60'
                          }`}>
                            {hasBookings ? `${customer.totalBookings} cuộc hẹn` : 'Khách mới'}
                          </span>
                          {customer.isActive !== undefined && (
                            <Badge tone={customer.isActive ? 'success' : 'neutral'}>
                              {customer.isActive ? 'Hoạt động' : 'Ngừng'}
                            </Badge>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Contact Info */}
                  <div className="text-xs text-stone-600 space-y-1.5 bg-stone-50/70 p-2.5 sm:p-3 rounded-xl border border-stone-100 min-w-0">
                    {customer.phone ? (
                      <p className="flex items-center gap-2 min-w-0">
                        <Phone size={13} className="text-stone-400 shrink-0" />
                        <a href={`tel:${customer.phone}`} className="hover:text-stone-900 font-medium truncate font-mono">
                          {customer.phone}
                        </a>
                      </p>
                    ) : (
                      <p className="flex items-center gap-2 text-stone-400">
                        <Phone size={13} className="shrink-0" /> Chưa có số điện thoại
                      </p>
                    )}

                    {customer.email && (
                      <p className="flex items-center gap-2 min-w-0">
                        <Mail size={13} className="text-stone-400 shrink-0" />
                        <a href={`mailto:${customer.email}`} className="hover:text-stone-900 truncate block flex-1" title={customer.email}>
                          {customer.email}
                        </a>
                      </p>
                    )}

                    {customer.lastVisit ? (
                      <p className="flex items-center gap-2 text-[11px] text-stone-500 pt-1 border-t border-stone-200/50">
                        <Clock size={12} className="text-stone-400 shrink-0" />
                        Lần ghé gần nhất: <strong className="text-stone-800">{formatDateDMY(customer.lastVisit)}</strong>
                      </p>
                    ) : customer.lastBookingAt ? (
                      <p className="flex items-center gap-2 text-[11px] text-stone-500 pt-1 border-t border-stone-200/50">
                        <CalendarCheck size={12} className="text-stone-400 shrink-0" />
                        Lịch hẹn gần nhất: <strong className="text-stone-800">{formatDateDMY(customer.lastBookingAt)}</strong>
                      </p>
                    ) : (
                      <p className="text-[11px] text-stone-400 pt-1 border-t border-stone-200/50">
                        Chưa có lịch sử ghé spa
                      </p>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center justify-end gap-2 pt-1 border-t border-stone-100">
                    <Button variant="secondary" size="sm" onClick={() => handleOpenForm(customer)}>
                      Chỉnh sửa
                    </Button>
                    <Button
                      variant="danger-outline"
                      size="sm"
                      onClick={() => handleDeleteClick(customer)}
                      aria-label={`Xóa khách hàng ${customer.name}`}
                    >
                      <Trash2 size={14} aria-hidden="true" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deletingCustomer !== null}
        title="Xóa khách hàng?"
        description={
          <>
            Khách hàng <strong>{deletingCustomer?.name}</strong> sẽ bị xóa vĩnh viễn. Thao tác này
            không thể hoàn tác và sẽ thất bại nếu khách hàng vẫn còn lịch hẹn liên quan.
          </>
        }
        confirmLabel="Xóa khách hàng"
        cancelLabel="Giữ lại"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingCustomer(null)}
        busy={isDeleting}
        error={deleteError}
      />
    </AppShell>
  );
}
