import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { TelemetryApiService } from '../../services/telemetry-api.service';
import { TelemetryWebsocketService } from '../../services/telemetry-websocket.service';
import { TelemetryPredictionRecord, TelemetryPrediction } from '../../models/telemetry.model';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

interface AlertRow {
  vehicleId: string;
  riskLevel: string;
  probability: number;
  recommendation: string;
  rootCauses: { sensor: string; issue: string }[];
  when: Date;
  live?: boolean; // true if it just arrived over WebSocket, for a brief highlight
}

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './alerts.component.html',
  styleUrl: './alerts.component.scss',
})
export class AlertsComponent implements OnInit, OnDestroy {
  alerts: AlertRow[] = [];
  loading = true;
   // Pagination
  currentPage = 1;
  pageSize = 10;
  private sub?: Subscription;

  constructor(private api: TelemetryApiService, private ws: TelemetryWebsocketService) {}

  ngOnInit(): void {
    this.api.anomalies().subscribe({
      next: (records: TelemetryPredictionRecord[]) => {
        this.alerts = records.map((r) => ({
          vehicleId: r.vehicleId,
          riskLevel: r.riskLevel,
          probability: r.probability,
          recommendation: r.recommendation,
          rootCauses: r.rootCauses,
          when: new Date(r.createdAt),
        }));
        this.loading = false;
      },
      error: () => (this.loading = false),
    });

    this.sub = this.ws.anomalies$().subscribe((p: TelemetryPrediction) => {
      const row: AlertRow = {
        vehicleId: p.vehicle_id ?? 'unknown',
        riskLevel: p.risk_level ?? 'HIGH',
        probability: p.probability ?? 0,
        recommendation: p.recommendation ?? '',
        rootCauses: p.root_causes ?? [],
        when: new Date(),
        live: true,
      };
      this.alerts = [row, ...this.alerts];
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  // Alertes affichées sur la page actuelle
  get paginatedAlerts(): AlertRow[] {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;

    return this.alerts.slice(start, end);
  }

  // Nombre total de pages
  get totalPages(): number {
    return Math.ceil(this.alerts.length / this.pageSize);
  }

  // Numéros des pages
  get pages(): number[] {
    return Array.from(
      { length: this.totalPages },
      (_, i) => i + 1
    );
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  previousPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  nextPage(): void {
    this.goToPage(this.currentPage + 1);
  }
}
