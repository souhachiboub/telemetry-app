import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TelemetryReading } from '../models/telemetry.model';

@Injectable({ providedIn: 'root' })
export class SimulatorApiService {
  private readonly base = '/api/simulator';

  constructor(private http: HttpClient) {}

  /** Publishes one simulated reading over MQTT for a vehicle. */
  publishOne(vehicleId: string, scenario: 'normal' | 'anomaly', step = 0): Observable<unknown> {
    const params = new URLSearchParams({ scenario, step: String(step) });
    return this.http.post(`${this.base}/publish/${vehicleId}?${params}`, {});
  }

  /** Publishes `count` consecutive readings in one call (default 15 = fills the ML window). */
  publishBatch(vehicleId: string, scenario: 'normal' | 'anomaly', count = 15): Observable<unknown> {
    const params = new URLSearchParams({ scenario, count: String(count) });
    return this.http.post(`${this.base}/publish/${vehicleId}/batch?${params}`, {});
  }

  /** Publishes one hand-edited reading, exactly as typed in the manual form. */
  publishManual(vehicleId: string, telemetry: TelemetryReading): Observable<unknown> {
    return this.http.post(`${this.base}/publish-manual`, { vehicleId, telemetry });
  }
}
