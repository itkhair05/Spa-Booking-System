export interface BookingTrendPoint {
  /** ISO calendar date (YYYY-MM-DD) in Vietnam timezone. */
  date: string;
  count: number;
}

export interface BookingStatusCount {
  status: string;
  count: number;
}

export interface PopularServiceCount {
  serviceName: string;
  bookingCount: number;
}

export interface DashboardMetrics {
  todayBookingCount: number;
  upcomingBookingCount: number;
  pendingBookingCount: number;
  confirmedBookingCount: number;
  todayCompletedRevenue: number;
  /** Kept for backend contract compatibility; not rendered in the UI. */
  todayExpectedRevenue: number;
  totalCompletedRevenue: number;
  bookingTrend: BookingTrendPoint[];
  bookingStatusDistribution: BookingStatusCount[];
  popularServices: PopularServiceCount[];
}
