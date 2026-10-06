export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface StaffWorkingHours {
  id?: number;
  staffId?: number;
  dayOfWeek: DayOfWeek;
  startTime: string; // "HH:mm" or "HH:mm:ss"
  endTime: string;   // "HH:mm" or "HH:mm:ss"
  isActive: boolean;
}

export interface UpdateWorkingHoursRequest {
  workingHours: StaffWorkingHours[];
}

export interface StaffDayOff {
  id: number;
  staffId: number;
  staffName?: string;
  date: string; // YYYY-MM-DD
  reason?: string | null;
  createdAt?: string;
}

export interface CreateDayOffRequest {
  date: string; // YYYY-MM-DD
  reason?: string;
}

export interface DailyScheduleItem {
  id: number;
  bookingCode?: string;
  customerId?: number;
  customerName: string;
  customerPhone?: string;
  serviceId?: number;
  serviceName: string;
  serviceDuration?: number;
  staffId: number;
  staffName: string;
  startTime: string;
  endTime: string;
  status: string;
  price: number;
}
