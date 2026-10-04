export interface Staff {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  isActive: boolean;
  avatarUrl?: string | null;
  username?: string | null;
  accountEnabled?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStaffRequest {
  name: string;
  phone?: string;
  email?: string;
  avatarUrl?: string | null;
  isActive?: boolean;
}

export interface UpdateStaffRequest {
  name: string;
  phone?: string;
  email?: string;
  avatarUrl?: string | null;
  isActive?: boolean;
}

export interface CreateStaffAccountRequest {
  username: string;
  password: string;
}

export interface StaffAccountResponse {
  userId: number;
  username: string;
  role: string;
  staffId: number;
  staffName: string;
  isActive: boolean;
}
