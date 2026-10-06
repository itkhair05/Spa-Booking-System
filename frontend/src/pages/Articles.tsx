import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { getArticles, deleteArticle, publishArticle, unpublishArticle } from '../lib/api/articles';
import type { Article } from '../types/article';
import { ArticleForm } from './ArticleForm';
import RestrictedAccess from '../components/RestrictedAccess';
import { BookOpen, Plus, Trash2, Globe, EyeOff, Calendar } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const Articles = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [articles, setArticles] = useState<Article[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingArticle, setEditingArticle] = useState<Article | undefined>(undefined);

  // Delete state
  const [deletingArticle, setDeletingArticle] = useState<Article | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const fetchArticles = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getArticles();
      setArticles(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Không thể tải danh sách bài viết.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOwner) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchArticles();
    }
  }, [fetchArticles, isOwner]);

  const handleOpenForm = (article?: Article) => {
    if (!isOwner) return;
    setEditingArticle(article);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingArticle(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchArticles();
  };

  const handleTogglePublish = async (article: Article) => {
    if (!isOwner) return;
    try {
      if (article.status === 'PUBLISHED') {
        await unpublishArticle(article.id);
      } else {
        await publishArticle(article.id);
      }
      await fetchArticles();
    } catch {
      // ignore
    }
  };

  const handleDeleteClick = (article: Article) => {
    if (!isOwner) return;
    setDeleteError(null);
    setDeletingArticle(article);
  };

  const handleConfirmDelete = async () => {
    if (!deletingArticle) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteArticle(deletingArticle.id);
      setDeletingArticle(null);
      fetchArticles();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setDeleteError(errorObj.response?.data?.message || 'Không thể xóa bài viết.');
    } finally {
      setIsDeleting(false);
    }
  };

  if (!isOwner) {
    return (
      <RestrictedAccess
        shellTitle="Góc chăm sóc"
        icon={<BookOpen className="w-7 h-7" />}
        message="Khu vực quản lý bài viết — Góc chăm sóc chỉ dành riêng cho Quản trị viên (OWNER)."
      />
    );
  }

  if (isFormOpen && isOwner) {
    return (
      <AppShell title="Góc chăm sóc">
        <PageHeader title="Quản lý Bài viết" />
        <div className="max-w-2xl mx-auto">
          <ArticleForm
            article={editingArticle}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Góc chăm sóc">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader
          title="Quản lý Bài viết — Góc Chăm Sóc"
          description="Các bài viết chia sẻ kiến thức, mẹo dưỡng sinh và chăm sóc sắc đẹp trên website TIKEY SPA."
        />
        {isOwner && (
          <Button onClick={() => handleOpenForm()}>
            <Plus size={16} />
            Viết bài mới
          </Button>
        )}
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-40 bg-stone-100 rounded-xl" />
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchArticles} />
      )}

      {!isLoading && !error && articles.length === 0 && (
        <EmptyState
          icon={<BookOpen size={22} />}
          title="Chưa có bài viết nào"
          description={
            isOwner
              ? 'Tạo bài viết đầu tiên để chia sẻ kiến thức chăm sóc thân tâm với khách hàng.'
              : 'Hiện chưa có bài viết nào.'
          }
          action={
            isOwner ? (
              <Button onClick={() => handleOpenForm()}>
                <Plus size={16} />
                Viết bài mới
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && !error && articles.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {articles.map((article) => {
            const isPublished = article.status === 'PUBLISHED';

            return (
              <Card key={article.id} className="overflow-hidden flex flex-col justify-between border-[#e7e2d8] hover:border-[#c6d8c9] transition-all">
                {article.coverImage && (
                  <div className="h-40 w-full overflow-hidden bg-stone-100">
                    <img
                      src={article.coverImage}
                      alt={article.title}
                      className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                    />
                  </div>
                )}
                <CardContent className="p-5 flex flex-col h-full grow">
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="text-[11px] font-semibold text-[#566f5c] px-2 py-0.5 rounded-full bg-[#f2f6f3] border border-[#c6d8c9]/60">
                      {article.category || 'Góc chăm sóc'}
                    </span>
                    <Badge tone={isPublished ? 'success' : 'neutral'}>
                      {isPublished ? 'Đã xuất bản' : 'Bản nháp'}
                    </Badge>
                  </div>

                  <h4 className="font-semibold text-base text-stone-900 mb-1.5 leading-snug line-clamp-2">
                    {article.title}
                  </h4>

                  {article.excerpt && (
                    <p className="text-xs text-stone-500 mb-4 grow line-clamp-3 leading-relaxed">
                      {article.excerpt}
                    </p>
                  )}

                  <div className="flex items-center gap-2 text-[11px] text-stone-400 mb-3">
                    <Calendar size={12} />
                    <span>{article.readTime || '3 phút đọc'}</span>
                  </div>

                  {isOwner && (
                    <div className="mt-auto pt-3 border-t border-stone-100 flex items-center justify-between gap-2">
                      <button
                        type="button"
                        onClick={() => handleTogglePublish(article)}
                        className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1 rounded-lg border transition-colors cursor-pointer ${
                          isPublished
                            ? 'bg-stone-50 text-stone-600 border-stone-200 hover:bg-stone-100'
                            : 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100'
                        }`}
                      >
                        {isPublished ? (
                          <>
                            <EyeOff size={12} />
                            <span>Gỡ xuất bản</span>
                          </>
                        ) : (
                          <>
                            <Globe size={12} />
                            <span>Xuất bản</span>
                          </>
                        )}
                      </button>

                      <div className="flex items-center gap-1.5">
                        <Button variant="secondary" size="sm" onClick={() => handleOpenForm(article)}>
                          Sửa
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleDeleteClick(article)}
                          aria-label={`Xóa bài viết ${article.title}`}
                        >
                          <Trash2 size={14} aria-hidden="true" />
                        </Button>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      <ConfirmDialog
        open={deletingArticle !== null}
        title="Xóa bài viết?"
        description={
          <>
            Bài viết <strong>{deletingArticle?.title}</strong> sẽ bị xóa hoàn toàn khỏi hệ thống.
          </>
        }
        confirmLabel="Xóa bài viết"
        cancelLabel="Giữ lại"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingArticle(null)}
        busy={isDeleting}
        error={deleteError}
      />
    </AppShell>
  );
};

export default Articles;
