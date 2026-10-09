export interface Review {
  id: number;
  customerName: string;
  rating: number;
  comment: string;
  serviceName?: string | null;
  isPublished: boolean;
  isDemo: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReviewRequest {
  customerName: string;
  rating: number;
  comment: string;
  serviceName?: string | null;
  isPublished?: boolean;
  isDemo?: boolean;
  displayOrder?: number;
}

export interface UpdateReviewRequest {
  customerName?: string;
  rating?: number;
  comment?: string;
  serviceName?: string | null;
  isPublished?: boolean;
  isDemo?: boolean;
  displayOrder?: number;
}
