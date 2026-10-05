import api from './axios';
import type {
  Article,
  CreateArticleRequest,
  UpdateArticleRequest,
} from '../../types/article';

export const getArticles = async (): Promise<Article[]> => {
  const { data } = await api.get<Article[]>('/articles');
  return data;
};

export const getArticle = async (id: number): Promise<Article> => {
  const { data } = await api.get<Article>(`/articles/${id}`);
  return data;
};

export const createArticle = async (request: CreateArticleRequest): Promise<Article> => {
  const { data } = await api.post<Article>('/articles', request);
  return data;
};

export const updateArticle = async (
  id: number,
  request: UpdateArticleRequest
): Promise<Article> => {
  const { data } = await api.put<Article>(`/articles/${id}`, request);
  return data;
};

export const publishArticle = async (id: number): Promise<Article> => {
  const { data } = await api.patch<Article>(`/articles/${id}/publish`);
  return data;
};

export const unpublishArticle = async (id: number): Promise<Article> => {
  const { data } = await api.patch<Article>(`/articles/${id}/unpublish`);
  return data;
};

export const deleteArticle = async (id: number): Promise<void> => {
  await api.delete(`/articles/${id}`);
};

export const uploadArticleCover = async (id: number, file: File): Promise<Article> => {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await api.post<Article>(`/articles/${id}/cover`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
};
