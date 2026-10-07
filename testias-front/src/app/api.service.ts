import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiError, Reservation, ReservationRequest, VehiclePage } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  getVehicles(page: number, size: number, brand: string): Observable<VehiclePage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (brand.trim()) params = params.set('brand', brand.trim());
    return this.http.get<VehiclePage>('/api/vehicles', { params });
  }

  createReservation(request: ReservationRequest): Observable<Reservation> {
    return this.http.post<Reservation>('/api/reservations', request);
  }

  findReservation(referenceId: string): Observable<Reservation> {
    return this.http.get<Reservation>(`/api/reservations/${encodeURIComponent(referenceId)}`);
  }

  messageFor(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as ApiError | string | null;
      if (typeof body === 'string' && body.trim()) return body;
      if (body && typeof body === 'object' && (body.message || body.error)) return body.message ?? body.error ?? fallback;
      if (error.status === 0) return 'The backend is unavailable. Check that the API is running and retry.';
      return `${error.status}: ${error.statusText || fallback}`;
    }
    return fallback;
  }
}
