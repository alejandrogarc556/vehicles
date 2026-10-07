import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ApiService } from './api.service';
import { App } from './app';
import { Reservation, VehiclePage } from './models';

describe('App reservation workflow', () => {
  let api: jasmine.SpyObj<ApiService>;
  let fixture: ReturnType<typeof TestBed.createComponent<App>>;

  const vehiclePage = (brand = 'Toyota'): VehiclePage => ({
    content: [{ id: 'VM-001', brand, model: 'Corolla', year: 2024, price: 24500, availability: 'AVAILABLE' }],
    number: 0,
    size: 5,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true
  });

  const reservation: Reservation = {
    referenceId: 'REF-001',
    vehicle: vehiclePage().content[0],
    buyerId: 'BUY-001',
    status: 'CONFIRMED',
    reason: null,
    createdAt: '2026-10-07T12:00:00Z'
  };

  beforeEach(async () => {
    api = jasmine.createSpyObj<ApiService>('ApiService', [
      'getVehicles', 'createReservation', 'findReservation', 'messageFor'
    ]);
    api.getVehicles.and.returnValue(of(vehiclePage()));
    api.createReservation.and.returnValue(of(reservation));
    api.findReservation.and.returnValue(of(reservation));
    api.messageFor.and.callFake((_error, fallback) => fallback);

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ApiService, useValue: api }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  });

  it('renders vehicles received from the API', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('VM-001');
    expect(element.textContent).toContain('Toyota Corolla');
    expect(element.textContent).toContain('AVAILABLE');
  });

  it('requests and displays results for the selected brand filter', () => {
    const app = fixture.componentInstance;
    app.brand.set('Mazda');
    api.getVehicles.and.returnValue(of(vehiclePage('Mazda')));

    app.applyFilter();
    fixture.detectChanges();

    expect(api.getVehicles).toHaveBeenCalledWith(0, 5, 'Mazda');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Mazda Corolla');
  });

  it('submits a valid reservation and displays the backend result', () => {
    const app = fixture.componentInstance;
    app.reservationForm.setValue({ referenceId: 'REF-001', buyerId: 'BUY-001', vehicleId: 'VM-001' });

    app.submitReservation();
    fixture.detectChanges();

    expect(api.createReservation).toHaveBeenCalledWith({ referenceId: 'REF-001', buyerId: 'BUY-001', vehicleId: 'VM-001' });
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('CONFIRMED');
  });

  it('looks up a reservation and displays its vehicle, buyer, and status', () => {
    const app = fixture.componentInstance;
    app.lookupForm.controls.referenceId.setValue('REF-001');

    app.lookupReservation();
    fixture.detectChanges();

    expect(api.findReservation).toHaveBeenCalledWith('REF-001');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('CONFIRMED');
    expect(text).toContain('BUY-001');
    expect(text).toContain('VM-001');
  });

  it('shows a friendly catalog error and can retry the request', () => {
    api.getVehicles.and.returnValues(
      throwError(() => new Error('network failure')),
      of(vehiclePage('Ford'))
    );
    const app = fixture.componentInstance;
    app.loadVehicles();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Could not load vehicles.');

    app.retryCatalog();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ford Corolla');
  });

  it('shows the loading indicator while a catalog request is pending', () => {
    const pending = new Subject<VehiclePage>();
    api.getVehicles.and.returnValue(pending.asObservable());
    const app = fixture.componentInstance;
    app.loadVehicles();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Loading vehicles');
    pending.next(vehiclePage());
    pending.complete();
  });

  it('does not allow an older catalog response to overwrite a newer filter request', () => {
    const requests: Subject<VehiclePage>[] = [];
    api.getVehicles.and.callFake(() => {
      const request = new Subject<VehiclePage>();
      requests.push(request);
      return request.asObservable();
    });
    const app = fixture.componentInstance;
    app.loadVehicles();
    app.brand.set('Mazda');
    app.applyFilter();

    requests[1].next(vehiclePage('Mazda'));
    requests[1].complete();
    requests[0].next(vehiclePage('Toyota'));
    requests[0].complete();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Mazda Corolla');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Toyota Corolla');
  });
});
