import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { SimulatorApiService } from '../../services/simulator-api.service';
import { TelemetryApiService} from '../../services/telemetry-api.service';
import { TelemetryWebsocketService } from '../../services/telemetry-websocket.service';
import { TelemetryPrediction, TelemetryReading, TelemetryUploadResult } from '../../models/telemetry.model';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

@Component({
  selector: 'app-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './simulator.component.html',
  styleUrl: './simulator.component.scss',
})
export class SimulatorComponent implements OnInit, OnDestroy {
  vehicleId = 'device-sim-01';

  // --- ML self-test (auto-generated scenarios) ---------------------------
  scenario: 'normal' | 'anomaly' = 'anomaly';
  batchCount = 15;
  busy = false;

  // --- Publish message with payload: manual reading -----------------------
  manual: TelemetryReading = {
    Trip_Time_s: 512,
    Speed_kmh: 120,
    Motor_RPM: 8200,
    Motor_Temp_C: 78.5,
    Battery_SOC_pct: 46.2,
    Battery_Voltage_V: 355.1,
    Battery_Current_A: 59.8,
    Battery_Temp_C: 41,
    Ambient_Temp_C: 24,
    Braking_Active: 'OFF',
    Vehicle_State: 'Accelerating',
  };

  // --- Publish message with payload: upload a shared JSON file -----------
  selectedFile: File | null = null;
  uploadBusy = false;
  uploadResult: TelemetryUploadResult | null = null;

  // --- Live response -------------------------------------------------------
  liveEvents: TelemetryPrediction[] = [];
  private sub?: Subscription;
  private currentSubscribedVehicle = '';

  constructor(
    private simulatorApi: SimulatorApiService,
    private telemetryApi: TelemetryApiService,
    private ws: TelemetryWebsocketService,
  ) {}

  ngOnInit(): void {
    this.watchVehicle();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  /** Re-subscribes the WebSocket stream whenever the target vehicle id changes. */
  watchVehicle() {
    if (!this.vehicleId || this.vehicleId === this.currentSubscribedVehicle) return;
    this.sub?.unsubscribe();
    this.currentSubscribedVehicle = this.vehicleId;
    this.liveEvents = [];
    this.sub = this.ws.forVehicle(this.vehicleId).subscribe((event) => {
      this.liveEvents = [event, ...this.liveEvents].slice(0, 20);
    });
  }

  // --- ML self-test actions -------------------------------------------------

  publishOne() {
    this.watchVehicle();
    this.busy = true;
    this.simulatorApi.publishOne(this.vehicleId, this.scenario).subscribe({
      complete: () => (this.busy = false),
      error: () => (this.busy = false),
    });
  }

  publishBatch() {
    this.watchVehicle();
    this.busy = true;
    this.simulatorApi.publishBatch(this.vehicleId, this.scenario, this.batchCount).subscribe({
      complete: () => (this.busy = false),
      error: () => (this.busy = false),
    });
  }

  // --- Publish message with payload: manual reading -------------------------

  publishManual() {
    this.watchVehicle();
    this.busy = true;
    this.simulatorApi.publishManual(this.vehicleId, this.manual).subscribe({
      complete: () => (this.busy = false),
      error: () => (this.busy = false),
    });
  }

  // --- Publish message with payload: upload a shared JSON file --------------

  /** Grabs the file picked in the <input type="file">, if any. */
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.uploadResult = null;
  }

  /**
   * Extracts and replays a telemetry window that was already shared as a
   * JSON file ({ vehicleId, window: TelemetryReading[] }), via
   * TelemetryApiService#uploadTelemetryFile. On success, switches the "Live
   * response" panel over to watch the vehicle the file belongs to.
   */
  uploadFile() {
    if (!this.selectedFile) return;
    this.uploadBusy = true;
    this.uploadResult = null;

    this.telemetryApi.uploadTelemetryFile(this.selectedFile).subscribe({
      next: (result) => {
        this.uploadResult = result;
        this.uploadBusy = false;
        if (result.success && result.vehicleId) {
          this.vehicleId = result.vehicleId;
          this.watchVehicle();
        }
      },
      error: (err) => {
        this.uploadResult = { success: false, error: err?.error?.error ?? 'Upload failed.' };
        this.uploadBusy = false;
      },
    });
  }
}