export type Availability = 'AVAILABLE' | 'RESERVED';
export type ReservationStatus = 'CONFIRMED' | 'REJECTED';

export interface Vehicle {
  id: string;
  brand: string;
  model: string;
  year: number;
  price: number;
  availability: Availability;
}

export interface VehiclePage {
  content: Vehicle[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ReservationRequest { referenceId: string; vehicleId: string; buyerId: string; }

export interface Reservation {
  referenceId: string;
  vehicle: Vehicle | null;
  buyerId: string;
  status: ReservationStatus;
  reason: string | null;
  createdAt: string;
}

export interface ApiError { code?: string; message?: string; error?: string; }
