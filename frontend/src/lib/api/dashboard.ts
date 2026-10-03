import api from './axios';
import type { DashboardMetrics } from '../../types/dashboard';

/**
 * Fetches dashboard metrics from the backend API.
 */
export async function getDashboardMetrics(): Promise<DashboardMetrics> {
  const response = await api.get<DashboardMetrics>('/dashboard/metrics');
  return response.data;
}
