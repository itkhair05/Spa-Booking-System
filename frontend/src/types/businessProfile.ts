export interface BusinessProfileResponse {
  id: number;
  name: string;
  slug: string;
  phone: string;
  email: string;
  address: string;
  timezone: string;
}

export interface UpdateBusinessProfileRequest {
  name: string;
  phone?: string;
  email?: string;
  address?: string;
}
