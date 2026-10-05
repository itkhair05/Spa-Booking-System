import { useState } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Alert } from '../components/ui/Alert';
import { createArticle, updateArticle, uploadArticleCover } from '../lib/api/articles';
import type { Article, CreateArticleRequest, UpdateArticleRequest } from '../types/article';
import { Upload, Image as ImageIcon } from 'lucide-react';

interface ArticleFormProps {
  article?: Article;
  onSuccess: () => void;
  onCancel: () => void;
}

export const ArticleForm = ({ article, onSuccess, onCancel }: ArticleFormProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState(article ? article.title : '');
  const [slug, setSlug] = useState(article ? article.slug : '');
  const [category, setCategory] = useState(article ? (article.category || '') : 'Chăm sóc da');
  const [readTime, setReadTime] = useState(article ? (article.readTime || '') : '5 phút đọc');
  const [excerpt, setExcerpt] = useState(article ? (article.excerpt || '') : '');
  const [content, setContent] = useState(article ? article.content : '');
  const [status, setStatus] = useState<'DRAFT' | 'PUBLISHED'>(article ? article.status : 'DRAFT');

  // Cover image
  const currentCover = article?.coverImage || null;
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [coverPreview, setCoverPreview] = useState<string | null>(null);

  const handleCoverChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setError('Chỉ chấp nhận ảnh định dạng JPG, PNG hoặc WEBP.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('Dung lượng ảnh bìa không được vượt quá 5MB.');
      return;
    }
    setCoverFile(file);
    setCoverPreview(URL.createObjectURL(file));
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError('Vui lòng nhập tiêu đề và nội dung bài viết.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      let savedArticle: Article;
      if (article) {
        const data: UpdateArticleRequest = {
          title: title.trim(),
          slug: slug.trim() || undefined,
          category: category.trim() || undefined,
          readTime: readTime.trim() || undefined,
          excerpt: excerpt.trim() || undefined,
          content: content.trim(),
          status,
        };
        savedArticle = await updateArticle(article.id, data);
      } else {
        const data: CreateArticleRequest = {
          title: title.trim(),
          slug: slug.trim() || undefined,
          category: category.trim() || undefined,
          readTime: readTime.trim() || undefined,
          excerpt: excerpt.trim() || undefined,
          content: content.trim(),
          status,
        };
        savedArticle = await createArticle(data);
      }

      if (coverFile) {
        await uploadArticleCover(savedArticle.id, coverFile);
      }

      onSuccess();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Có lỗi xảy ra khi lưu bài viết.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {article ? 'Chỉnh sửa bài viết' : 'Viết bài mới cho Góc chăm sóc'}
        </h3>

        {error && (
          <Alert tone="error" className="mb-4">
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Cover Image Upload */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-2">
              Ảnh bìa bài viết
            </label>
            <div className="flex items-center gap-4">
              <div className="w-28 h-20 rounded-xl overflow-hidden bg-stone-100 border border-stone-200 flex items-center justify-center shrink-0">
                {coverPreview ? (
                  <img src={coverPreview} alt="Xem trước" className="w-full h-full object-cover" />
                ) : currentCover ? (
                  <img src={currentCover} alt="Ảnh bìa" className="w-full h-full object-cover" />
                ) : (
                  <ImageIcon className="w-7 h-7 text-stone-400" />
                )}
              </div>
              <div>
                <label className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-stone-100 hover:bg-stone-200 text-stone-800 text-xs font-semibold cursor-pointer transition-colors border border-stone-200">
                  <Upload size={13} />
                  <span>{coverPreview || currentCover ? 'Thay ảnh' : 'Tải ảnh bìa'}</span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    className="hidden"
                    onChange={handleCoverChange}
                    disabled={isSubmitting}
                  />
                </label>
                <p className="text-[11px] text-stone-500 mt-1">Định dạng JPG, PNG, WEBP (tối đa 5MB)</p>
              </div>
            </div>
          </div>

          <Input
            type="text"
            label="Tiêu đề bài viết"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            placeholder="VD: Bí quyết phục hồi cơ thể sau tuần làm việc"
          />

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input
              type="text"
              label="Chuyên mục"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="VD: Massage & Thư giãn"
            />
            <Input
              type="text"
              label="Thời gian đọc ước tính"
              value={readTime}
              onChange={(e) => setReadTime(e.target.value)}
              placeholder="VD: 5 phút đọc"
            />
            <Input
              type="text"
              label="Đường dẫn (slug tùy chọn)"
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              placeholder="bi-quyet-phuc-hoi"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-stone-700 mb-1">
              Đoạn tóm tắt (Excerpt)
            </label>
            <textarea
              rows={2}
              value={excerpt}
              onChange={(e) => setExcerpt(e.target.value)}
              placeholder="Tóm tắt ngắn gọn hiển thị trên thẻ bài viết..."
              className="w-full px-3.5 py-2 text-xs rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-stone-700 mb-1">
              Nội dung bài viết <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={8}
              required
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Nội dung chi tiết của bài viết..."
              className="w-full px-3.5 py-2.5 text-xs rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-1 focus:ring-[#465d4c]"
            />
          </div>

          <div className="pt-2 border-t border-stone-100 flex items-center justify-between">
            <label className="flex items-center gap-2 text-xs font-semibold text-stone-700 cursor-pointer">
              <input
                type="checkbox"
                checked={status === 'PUBLISHED'}
                onChange={(e) => setStatus(e.target.checked ? 'PUBLISHED' : 'DRAFT')}
                className="w-4 h-4 rounded border-stone-300 text-[#465d4c] focus:ring-[#465d4c]"
              />
              Xuất bản công khai (PUBLISHED)
            </label>

            <div className="flex gap-2">
              <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
                Hủy
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Đang lưu...' : (article ? 'Lưu thay đổi' : 'Tạo bài viết')}
              </Button>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
