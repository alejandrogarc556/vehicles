import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from './api.service';
import { Reservation, ReservationRequest, Vehicle } from './models';

@Component({
  selector: 'app-root',
  imports: [CommonModule, CurrencyPipe, ReactiveFormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  private catalogRequest = 0;

  readonly brand = signal('');
  readonly page = signal(0);
  readonly pageSize = 5;
  readonly vehicles = signal<Vehicle[]>([]);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly catalogLoading = signal(false);
  readonly catalogError = signal('');
  readonly reservationLoading = signal(false);
  readonly reservationError = signal('');
  readonly reservationResult = signal<Reservation | null>(null);
  readonly lookupLoading = signal(false);
  readonly lookupError = signal('');
  readonly lookupResult = signal<Reservation | null>(null);

  readonly reservationForm = this.formBuilder.nonNullable.group({
    referenceId: ['', [Validators.required, Validators.maxLength(50)]],
    buyerId: ['', [Validators.required, Validators.maxLength(50)]],
    vehicleId: ['', Validators.required]
  });
  readonly lookupForm = this.formBuilder.nonNullable.group({
    referenceId: ['', [Validators.required, Validators.maxLength(50)]]
  });

  constructor() { this.loadVehicles(); }

  loadVehicles(): void {
    const requestId = ++this.catalogRequest;
    this.catalogLoading.set(true);
    this.catalogError.set('');
    this.api.getVehicles(this.page(), this.pageSize, this.brand()).subscribe({
      next: result => {
        if (requestId !== this.catalogRequest) return;
        this.vehicles.set(result.content);
        this.totalPages.set(result.totalPages);
        this.totalElements.set(result.totalElements);
        this.catalogLoading.set(false);
      },
      error: error => {
        if (requestId !== this.catalogRequest) return;
        this.catalogLoading.set(false);
        this.catalogError.set(this.api.messageFor(error, 'Could not load vehicles.'));
      }
    });
  }

  applyFilter(): void { this.page.set(0); this.loadVehicles(); }
  clearFilter(): void { this.brand.set(''); this.applyFilter(); }

  previousPage(): void {
    if (this.page() === 0) return;
    this.page.update(value => value - 1);
    this.loadVehicles();
  }

  nextPage(): void {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.update(value => value + 1);
    this.loadVehicles();
  }

  selectVehicle(vehicle: Vehicle): void {
    this.reservationForm.controls.vehicleId.setValue(vehicle.id);
    document.getElementById('reservation-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  submitReservation(): void {
    this.reservationError.set('');
    this.reservationResult.set(null);
    if (this.reservationForm.invalid) {
      this.reservationForm.markAllAsTouched();
      return;
    }
    const request = this.reservationForm.getRawValue() as ReservationRequest;
    this.reservationLoading.set(true);
    this.api.createReservation(request).subscribe({
      next: result => {
        this.reservationLoading.set(false);
        this.reservationResult.set(result);
        this.loadVehicles();
      },
      error: error => {
        this.reservationLoading.set(false);
        this.reservationError.set(this.api.messageFor(error, 'Could not process the reservation.'));
      }
    });
  }

  lookupReservation(): void {
    this.lookupError.set('');
    this.lookupResult.set(null);
    if (this.lookupForm.invalid) {
      this.lookupForm.markAllAsTouched();
      return;
    }
    this.lookupLoading.set(true);
    this.api.findReservation(this.lookupForm.controls.referenceId.value.trim()).subscribe({
      next: result => {
        this.lookupLoading.set(false);
        this.lookupResult.set(result);
      },
      error: error => {
        this.lookupLoading.set(false);
        this.lookupError.set(this.api.messageFor(error, 'Could not find the reservation.'));
      }
    });
  }

  retryCatalog(): void { this.loadVehicles(); }
  retryLookup(): void { this.lookupReservation(); }
  retryReservation(): void { this.submitReservation(); }
}
