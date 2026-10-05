export interface Service {
  id: number;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  imageUrl?: string | null;
  isActive: boolean;
  categoryId?: number | null;
  categoryName?: string | null;
  isFeatured?: boolean;
  processSteps?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateServiceRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  imageUrl?: string | null;
  isActive?: boolean;
  categoryId?: number | null;
  isFeatured?: boolean;
  processSteps?: string | null;
}

export interface UpdateServiceRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  imageUrl?: string | null;
  isActive?: boolean;
  categoryId?: number | null;
  isFeatured?: boolean;
  processSteps?: string | null;
}
