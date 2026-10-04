import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Badge, type BadgeTone } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getFeedback, updateFeedbackStatus } from '../lib/api/feedback';
import { formatDateDMY } from '../lib/format';
import type { FeedbackResponse, FeedbackStatus, FeedbackType } from '../types/feedback';
import { MessageSquare, Phone, Mail, Ticket, CheckCircle2, Clock, AlertTriangle } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const typeBadgeTone = (type: FeedbackType): BadgeTone => {
  switch (type) {
    case 'COMPLAINT':
      return 'danger';
    case 'SUGGESTION':
      return 'warning';
    case 'PRAISE':
      return 'success';
    case 'OTHER':
    default:
      return 'neutral';
  }
};

const typeLabel = (type: FeedbackType): string => {
  switch (type) {
    case 'COMPLAINT':
      return 'Khiếu nại';
    case 'SUGGESTION':
      return 'Góp ý dịch vụ';
    case 'PRAISE':
      return 'Khen ngợi';
    case 'OTHER':
    default:
      return 'Ý kiến khác';
  }
};

const statusLabel = (status: FeedbackStatus): string => {
  switch (status) {
    case 'NEW':
      return 'Chưa xử lý';
    case 'IN_REVIEW':
      return 'Đang xử lý';
    case 'RESOLVED':
      return 'Đã giải quyết';
  }
};

const statusTone = (status: FeedbackStatus): 'neutral' | 'info' | 'success' => {
  switch (status) {
    case 'NEW':
      return 'neutral';
    case 'IN_REVIEW':
      return 'info';
    case 'RESOLVED':
      return 'success';
  }
};

const FeedbackPage = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [feedbackList, setFeedbackList] = useState<FeedbackResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const fetchList = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getFeedback();
      setFeedbackList(data);
    } catch {
      setError('Không thể tải danh sách phản hồi.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOwner) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchList();
    }
  }, [fetchList, isOwner]);

  const handleStatusChange = async (id: number, newStatus: FeedbackStatus) => {
    setUpdatingId(id);
    try {
      const updated = await updateFeedbackStatus(id, newStatus);
      setFeedbackList((prev) =>
        prev.map((item) => (item.id === id ? updated : item))
      );
    } catch {
      // ignore or show error
    } finally {
      setUpdatingId(null);
    }
  };

  if (!isOwner) {
    return (
      <AppShell title="Phản hồi & Khiếu nại">
        <div className="max-w-2xl mx-auto py-16 text-center px-4">
          <div className="w-14 h-14 rounded-full bg-amber-50 text-amber-700 flex items-center justify-center mx-auto mb-4 border border-amber-200">
            <MessageSquare className="w-7 h-7" />
          </div>
          <h2 className="text-xl font-serif-title font-semibold text-stone-900 mb-2">Quyền truy cập hạn chế</h2>
          <p className="text-stone-600 text-sm leading-relaxed mb-6">
            Khu vực quản lý phản hồi và khiếu nại của khách hàng chỉ dành riêng cho Quản trị viên (OWNER).
          </p>
        </div>
      </AppShell>
    );
  }

  const filtered = filterStatus === 'ALL'
    ? feedbackList
    : feedbackList.filter((f) => f.status === filterStatus);

  const newCount = feedbackList.filter((f) => f.status === 'NEW').length;
  const inReviewCount = feedbackList.filter((f) => f.status === 'IN_REVIEW').length;
  const resolvedCount = feedbackList.filter((f) => f.status === 'RESOLVED').length;

  return (
    <AppShell title="Phản hồi & Khiếu nại">
      <PageHeader
        title="Phản hồi & Khiếu nại"
        description="Lắng nghe ý kiến đóng góp, giải quyết khiếu nại để không ngừng nâng cao chất lượng dịch vụ tại TIKEY SPA."
      />

      {/* Filter Tabs */}
      <div className="flex flex-wrap items-center gap-2 mb-6">
        <button
          onClick={() => setFilterStatus('ALL')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
            filterStatus === 'ALL'
              ? 'bg-[#465d4c] text-white shadow-xs'
              : 'bg-white text-stone-600 border border-stone-200 hover:bg-stone-50'
          }`}
        >
          Tất cả ({feedbackList.length})
        </button>

        <button
          onClick={() => setFilterStatus('NEW')}
          className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
            filterStatus === 'NEW'
              ? 'bg-rose-700 text-white shadow-xs'
              : 'bg-white text-stone-600 border border-stone-200 hover:bg-stone-50'
          }`}
        >
          <Clock size={13} />
          <span>Mới chưa xử lý ({newCount})</span>
        </button>

        <button
          onClick={() => setFilterStatus('IN_REVIEW')}
          className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
            filterStatus === 'IN_REVIEW'
              ? 'bg-sky-700 text-white shadow-xs'
              : 'bg-white text-stone-600 border border-stone-200 hover:bg-stone-50'
          }`}
        >
          <AlertTriangle size={13} />
          <span>Đang xử lý ({inReviewCount})</span>
        </button>

        <button
          onClick={() => setFilterStatus('RESOLVED')}
          className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
            filterStatus === 'RESOLVED'
              ? 'bg-emerald-700 text-white shadow-xs'
              : 'bg-white text-stone-600 border border-stone-200 hover:bg-stone-50'
          }`}
        >
          <CheckCircle2 size={13} />
          <span>Đã giải quyết ({resolvedCount})</span>
        </button>
      </div>

      {loading && (
        <div className="space-y-4" aria-busy="true">
          {[1, 2, 3].map((i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="h-28 bg-stone-100 rounded-xl" />
            </Card>
          ))}
        </div>
      )}

      {error && !loading && (
        <ErrorState message={error} onRetry={fetchList} />
      )}

      {!loading && !error && filtered.length === 0 && (
        <EmptyState
          icon={<MessageSquare size={24} />}
          title="Không có phản hồi nào"
          description={
            filterStatus === 'ALL'
              ? 'Chưa có ý kiến phản hồi hoặc khiếu nại nào từ khách hàng.'
              : 'Không có phản hồi nào thuộc trạng thái này.'
          }
        />
      )}

      {!loading && !error && filtered.length > 0 && (
        <div className="space-y-4">
          {filtered.map((item) => (
            <Card key={item.id} className="border-[#e7e2d8] hover:border-[#c6d8c9] transition-all">
              <CardContent className="p-5 sm:p-6">
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 mb-4 pb-4 border-b border-stone-100">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="font-semibold text-base text-stone-900">{item.name}</h3>
                      <Badge tone={typeBadgeTone(item.type)}>
                        {typeLabel(item.type)}
                      </Badge>
                      <Badge tone={statusTone(item.status)}>
                        {statusLabel(item.status)}
                      </Badge>
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-stone-500 pt-1">
                      <span className="flex items-center gap-1">
                        <Phone size={12} className="text-stone-400" />
                        <a href={`tel:${item.phone}`} className="hover:text-[#465d4c] font-medium">
                          {item.phone}
                        </a>
                      </span>

                      {item.email && (
                        <span className="flex items-center gap-1">
                          <Mail size={12} className="text-stone-400" />
                          <a href={`mailto:${item.email}`} className="hover:text-[#465d4c]">
                            {item.email}
                          </a>
                        </span>
                      )}

                      {item.bookingCode && (
                        <span className="flex items-center gap-1 font-mono font-medium text-[#b8976c] bg-[#fcf9f2] px-2 py-0.5 rounded border border-[#f6efe2]">
                          <Ticket size={12} />
                          <span>Mã: {item.bookingCode}</span>
                        </span>
                      )}

                      <span className="text-stone-400">
                        {formatDateDMY(item.createdAt)}
                      </span>
                    </div>
                  </div>

                  {/* Status Dropdown/Selector */}
                  <div className="flex items-center gap-2 shrink-0">
                    <span className="text-xs text-stone-500 font-medium">Trạng thái:</span>
                    <select
                      value={item.status}
                      onChange={(e) => handleStatusChange(item.id, e.target.value as FeedbackStatus)}
                      disabled={updatingId === item.id}
                      className="text-xs font-semibold px-2.5 py-1.5 rounded-lg border border-stone-200 bg-white text-stone-800 focus:outline-none focus:border-[#465d4c] cursor-pointer"
                    >
                      <option value="NEW">Chưa xử lý</option>
                      <option value="IN_REVIEW">Đang xử lý</option>
                      <option value="RESOLVED">Đã giải quyết</option>
                    </select>
                  </div>
                </div>

                {/* Message Content */}
                <div className="bg-stone-50/70 p-4 rounded-xl border border-stone-100 text-sm text-stone-800 whitespace-pre-wrap leading-relaxed">
                  {item.message}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </AppShell>
  );
};

export default FeedbackPage;
