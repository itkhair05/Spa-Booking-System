export interface Service {
  id: number;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateServiceRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  isActive?: boolean;
}

export interface UpdateServiceRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  isActive?: boolean;
}
