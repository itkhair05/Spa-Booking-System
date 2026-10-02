export interface ApiError {
  status: number;
  message: string;
  error?: string;
  errors?: Record<string, string>;
  timestamp?: string;
}
