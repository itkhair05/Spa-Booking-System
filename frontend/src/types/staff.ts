export interface Staff {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStaffRequest {
  name: string;
  phone?: string;
  email?: string;
  isActive?: boolean;
}

export interface UpdateStaffRequest {
  name: string;
  phone?: string;
  email?: string;
  isActive?: boolean;
}
