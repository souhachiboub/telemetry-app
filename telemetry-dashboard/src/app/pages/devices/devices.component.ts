import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TelemetryApiService } from '../../services/telemetry-api.service';
import { TelemetryPredictionRecord } from '../../models/telemetry.model';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

@Component({
  selector: 'app-devices',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './devices.component.html',
  styleUrl: './devices.component.scss',
})
export class DevicesComponent implements OnInit {
  devices: TelemetryPredictionRecord[] = [];
  loading = true;

  constructor(private api: TelemetryApiService) {}

  ngOnInit(): void {
    this.api.devices().subscribe({
      next: (devices) => {
        this.devices = devices;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }
}
