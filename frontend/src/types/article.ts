export interface Article {
  id: number;
  title: string;
  slug: string;
  category?: string | null;
  readTime?: string | null;
  excerpt?: string | null;
  content: string;
  coverImage?: string | null;
  status: 'DRAFT' | 'PUBLISHED';
  publishedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateArticleRequest {
  title: string;
  slug?: string;
  category?: string;
  readTime?: string;
  excerpt?: string;
  content: string;
  coverImage?: string;
  status?: string;
}

export interface UpdateArticleRequest {
  title: string;
  slug?: string;
  category?: string;
  readTime?: string;
  excerpt?: string;
  content: string;
  coverImage?: string;
  status?: string;
}
