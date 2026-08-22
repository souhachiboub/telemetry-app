import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TelemetryApiService } from '../../services/telemetry-api.service';
import { TelemetryPredictionRecord } from '../../models/telemetry.model';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { ScoreChartComponent } from '../../shared/score-chart.component';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, ScoreChartComponent],
  providers: [DatePipe],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export class AnalyticsComponent implements OnInit {
  vehicles: string[] = [];
  selectedVehicle = '';

  currentDetectionPage = 1;
detectionPageSize = 10;

  history: TelemetryPredictionRecord[] = [];
  recentAnomalies: TelemetryPredictionRecord[] = [];

  chartLabels: string[] = [];
  chartValues: number[] = [];

  loading = true;

  constructor(private api: TelemetryApiService, private route: ActivatedRoute, private datePipe: DatePipe) {}

  ngOnInit(): void {
    const routeVehicle = this.route.snapshot.paramMap.get('vehicleId');

    forkJoin({
      devices: this.api.devices(),
      anomalies: this.api.anomalies(),
    }).subscribe(({ devices, anomalies }) => {
      this.vehicles = devices.map((d) => d.vehicleId);
      this.recentAnomalies = anomalies;
      this.selectedVehicle = routeVehicle ?? this.vehicles[0] ?? '';
      if (this.selectedVehicle) this.loadHistory(this.selectedVehicle);
      this.loading = false;
    });
  }

  onVehicleChange(vehicleId: string) {
    this.selectedVehicle = vehicleId;
    this.loadHistory(vehicleId);
  }

  get anomaliesToday(): number {
    const today = new Date().toDateString();
    return this.recentAnomalies.filter((a) => new Date(a.createdAt).toDateString() === today).length;
  }

  get avgConfidencePct(): string {
    if (!this.history.length) return '—';
    const avg = this.history.reduce((sum, h) => sum + h.probability, 0) / this.history.length;
    return (avg * 100).toFixed(0) + '%';
  }

  get predictedMaintenanceCount(): number {
    return new Set(this.recentAnomalies.filter((a) => a.riskLevel === 'HIGH').map((a) => a.vehicleId)).size;
  }

  private loadHistory(vehicleId: string) {
    this.api.history(vehicleId).subscribe((records) => {
      // API returns most-recent-first; reverse so the chart reads left (past) -> right (now).
      this.history = [...records].reverse();
      this.chartLabels = this.history.map((r) => this.datePipe.transform(r.createdAt, 'HH:mm:ss') ?? '');
      this.chartValues = this.history.map((r) => r.probability);
    });
  }

  get paginatedAnomalies(): TelemetryPredictionRecord[] {
  const start = (this.currentDetectionPage - 1) * this.detectionPageSize;
  const end = start + this.detectionPageSize;

  return this.recentAnomalies.slice(start, end);
}

get detectionTotalPages(): number {
  return Math.ceil(
    this.recentAnomalies.length / this.detectionPageSize
  );
}

get detectionPages(): number[] {
  return Array.from(
    { length: this.detectionTotalPages },
    (_, i) => i + 1
  );
}

goToDetectionPage(page: number): void {
  if (page >= 1 && page <= this.detectionTotalPages) {
    this.currentDetectionPage = page;
  }
}

previousDetectionPage(): void {
  this.goToDetectionPage(this.currentDetectionPage - 1);
}

nextDetectionPage(): void {
  this.goToDetectionPage(this.currentDetectionPage + 1);
}
}
