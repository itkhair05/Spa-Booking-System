export type RefundStatus = 'REFUND_PENDING' | 'REFUNDED' | 'REFUND_FAILED';

export interface RefundEligibilityResponse {
  refundEligible: boolean;
  originalPaidAmount: number;
  refundAmount: number;
  cancellationFee: number;
  refundPercentage: number;
  deadline: string | null;
  policyDescription: string;
  ineligibleReason?: string | null;
}

export interface InitiateRefundRequest {
  reason?: string;
}

export interface RefundResponse {
  id: number;
  paymentId: number;
  bookingId: number;
  refundRequestId: string;
  provider: string;
  originalAmount: number;
  refundAmount: number;
  cancellationFee: number;
  reason?: string | null;
  status: RefundStatus;
  providerResponseCode?: string | null;
  providerResponseMessage?: string | null;
  providerTransactionReference?: string | null;
  requestedAt: string;
  processedAt?: string | null;
}
