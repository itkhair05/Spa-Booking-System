import api from './axios';
import type {
  StaffWorkingHours,
  UpdateWorkingHoursRequest,
  StaffDayOff,
  CreateDayOffRequest,
  DailyScheduleItem,
} from '../../types/schedule';

export const getStaffWorkingHours = async (staffId: number): Promise<StaffWorkingHours[]> => {
  const response = await api.get<StaffWorkingHours[]>(`/staff/${staffId}/working-hours`);
  return response.data;
};

export const updateStaffWorkingHours = async (
  staffId: number,
  data: UpdateWorkingHoursRequest
): Promise<StaffWorkingHours[]> => {
  const response = await api.put<StaffWorkingHours[]>(`/staff/${staffId}/working-hours`, data);
  return response.data;
};

export const getStaffDaysOff = async (staffId: number): Promise<StaffDayOff[]> => {
  const response = await api.get<StaffDayOff[]>(`/staff/${staffId}/days-off`);
  return response.data;
};

export const addStaffDayOff = async (
  staffId: number,
  data: CreateDayOffRequest
): Promise<StaffDayOff> => {
  const response = await api.post<StaffDayOff>(`/staff/${staffId}/days-off`, data);
  return response.data;
};

export const deleteStaffDayOff = async (staffId: number, dayOffId: number): Promise<void> => {
  await api.delete(`/staff/${staffId}/days-off/${dayOffId}`);
};

export const getDailySchedule = async (
  date: string,
  staffId?: number
): Promise<DailyScheduleItem[]> => {
  const params: Record<string, string | number> = { date };
  if (staffId != null) {
    params.staffId = staffId;
  }
  const response = await api.get<DailyScheduleItem[]>('/schedule/daily', { params });
  return response.data;
};

export const getStaffAvailability = async (
  staffId: number,
  serviceId: number,
  date: string
): Promise<string[]> => {
  const response = await api.get<string[]>(`/staff/${staffId}/availability`, {
    params: { serviceId, date },
  });
  return response.data;
};
