import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { TelemetryApiService } from '../../services/telemetry-api.service';
import { TelemetryWebsocketService } from '../../services/telemetry-websocket.service';
import { TelemetryPredictionRecord } from '../../models/telemetry.model';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

@Component({
  selector: 'app-live-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './live-dashboard.component.html',
  styleUrl: './live-dashboard.component.scss',
})
export class LiveDashboardComponent implements OnInit, OnDestroy {
  devices: TelemetryPredictionRecord[] = [];
  messagesPerMin = 0;
  private messageCountWindow: number[] = []; // timestamps (ms) of messages in the last 60s
  private sub?: Subscription;

  constructor(private api: TelemetryApiService, private ws: TelemetryWebsocketService) {}

  ngOnInit(): void {
    // Initial snapshot from REST so the table isn't empty before any
    // WebSocket message arrives.
    this.api.devices().subscribe((devices) => (this.devices = devices));

    this.sub = this.ws.global$.subscribe((prediction) => {
      this.trackThroughput();

      if (prediction.status === 'COLLECTING' || !prediction.vehicle_id) return;

      const idx = this.devices.findIndex((d) => d.vehicleId === prediction.vehicle_id);
      const updated: TelemetryPredictionRecord = {
        id: idx >= 0 ? this.devices[idx].id : Date.now(),
        vehicleId: prediction.vehicle_id!,
        status: prediction.status as 'NORMAL' | 'ANOMALY',
        probability: prediction.probability ?? 0,
        riskLevel: prediction.risk_level ?? 'LOW',
        label: prediction.label ?? 0,
        modelUsed: prediction.model_used ?? '',
        recommendation: prediction.recommendation ?? '',
        rootCauses: prediction.root_causes ?? [],
        createdAt: new Date().toISOString(),
      };

      if (idx >= 0) {
        this.devices[idx] = updated;
      } else {
        this.devices = [updated, ...this.devices];
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get onlineCount(): number {
    return this.devices.length;
  }

  get activeAnomalies(): number {
    return this.devices.filter((d) => d.status === 'ANOMALY').length;
  }

  private trackThroughput() {
    const now = Date.now();
    this.messageCountWindow.push(now);
    this.messageCountWindow = this.messageCountWindow.filter((t) => now - t < 60000);
    this.messagesPerMin = this.messageCountWindow.length;
  }
}
