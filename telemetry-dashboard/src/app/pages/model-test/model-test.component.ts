import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import {
  EMPTY_READING,
  PredictionResponse,
  RootCause,
  TelemetryReading,
  WINDOW_SIZE
} from '../../models/telemetry.model';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { TelemetryApiService } from '../../services/telemetry-api.service';

interface RootCauseView extends RootCause {
  icon: string;
  unit: string;
  overPct: number;
  fillPct: number;
  thresholdPct: number;
  severity: 'critical' | 'warning' | 'ok';
}

interface PredictionHistoryEntry {
  timestamp: number;
  probability: number;
  status: string;
  mode: 'window' | 'single';
}

@Component({
  selector: 'app-model-test',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './model-test.component.html',
  styleUrl: './model-test.component.css'
})
export class ModelTestComponent {
readonly windowSize = WINDOW_SIZE;

  readingForm: FormGroup;
  buffer: TelemetryReading[] = [];

  loading = false;
  quickPredicting = false;
  error: string | null = null;
  result: PredictionResponse | null = null;

  history: PredictionHistoryEntry[] = [];
  readonly maxHistory = 20;

  vehicleStates = ['Idle', 'Cruising', 'Accelerating', 'Braking'];

  // Sidebar / header UI state — same as telemtry-dashboard.component.ts,
  // duplicated here since the shell markup is copied rather than shared.
  operatorName = 'Jack Ryad';   // TODO: bind to real auth/user data
  theme: 'light' | 'dark' = 'light';

  readonly fieldGroups = [
    {
      label: 'Trip & motion',
      icon: 'gauge',
      fields: ['Trip_Time_s', 'Speed_kmh', 'Vehicle_State', 'Braking_Active'],
    },
    {
      label: 'Powertrain',
      icon: 'cpu',
      fields: ['Motor_RPM', 'Motor_Temp_C'],
    },
    {
      label: 'Battery',
      icon: 'battery-charging',
      fields: ['Battery_SOC_pct', 'Battery_Voltage_V', 'Battery_Current_A', 'Battery_Temp_C'],
    },
    {
      label: 'Environment',
      icon: 'thermometer',
      fields: ['Ambient_Temp_C'],
    },
  ];

  constructor(private fb: FormBuilder, private telemetryService: TelemetryApiService) {
    this.readingForm = this.fb.group({
      Trip_Time_s: [EMPTY_READING.Trip_Time_s, Validators.required],
      Speed_kmh: [EMPTY_READING.Speed_kmh, [Validators.required, Validators.min(0)]],
      Motor_RPM: [EMPTY_READING.Motor_RPM, [Validators.required, Validators.min(0)]],
      Motor_Temp_C: [EMPTY_READING.Motor_Temp_C, Validators.required],
      Battery_SOC_pct: [EMPTY_READING.Battery_SOC_pct, [Validators.required, Validators.min(0), Validators.max(100)]],
      Battery_Voltage_V: [EMPTY_READING.Battery_Voltage_V, Validators.required],
      Battery_Current_A: [EMPTY_READING.Battery_Current_A, Validators.required],
      Battery_Temp_C: [EMPTY_READING.Battery_Temp_C, Validators.required],
      Ambient_Temp_C: [EMPTY_READING.Ambient_Temp_C, Validators.required],
      Braking_Active: [EMPTY_READING.Braking_Active, Validators.required],
      Vehicle_State: [EMPTY_READING.Vehicle_State, Validators.required],
    });
  }

  get isFull(): boolean {
    return this.buffer.length === this.windowSize;
  }

  get bufferPct(): number {
    return Math.round((this.buffer.length / this.windowSize) * 100);
  }

  get pipelineStage(): 'collecting' | 'ready' | 'predicted' {
    if (this.result) return 'predicted';
    if (this.isFull) return 'ready';
    return 'collecting';
  }

  get operatorInitials(): string {
    return this.operatorName.split(' ').map(w => w[0]).join('');
  }

  setTheme(t: 'light' | 'dark'): void {
    this.theme = t;
    // TODO: toggle a class on document.body / app-shell root and swap CSS custom properties for dark mode
  }

  addReading(): void {
    if (this.readingForm.invalid) {
      this.readingForm.markAllAsTouched();
      return;
    }
    const reading = this.readingForm.value as TelemetryReading;

    if (this.buffer.length === this.windowSize) {
      this.buffer.shift();
    }
    this.buffer.push(reading);

    this.readingForm.patchValue({ Trip_Time_s: Number(reading.Trip_Time_s) + 1 });
    this.result = null;
    this.error = null;
  }

  removeReading(index: number): void {
    this.buffer.splice(index, 1);
    this.result = null;
  }

  clearBuffer(): void {
    this.buffer = [];
    this.result = null;
    this.error = null;
  }

  predict(): void {
    if (!this.isFull) return;
    this.loading = true;
    this.error = null;
    this.result = null;

   
  }

  quickPredict(): void {
    if (this.readingForm.invalid) {
      this.readingForm.markAllAsTouched();
      return;
    }
    const reading = this.readingForm.value as TelemetryReading;

    this.quickPredicting = true;
    this.error = null;
    this.result = null;

    this.telemetryService.predictManual(reading).subscribe({
      next: (res) => {
        this.result = res;
        this.pushHistory(res, 'single');
        this.quickPredicting = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? err?.message ?? 'Prediction request failed.';
        this.quickPredicting = false;
      },
    });
  }

  private pushHistory(res: PredictionResponse, mode: 'window' | 'single'): void {
    this.history.push({
      timestamp: Date.now(),
      probability: Math.round(res.probability * 100),
      status: res.status,
      mode,
    });
    if (this.history.length > this.maxHistory) {
      this.history.shift();
    }
  }

  get sparklinePoints(): string {
    if (this.history.length < 2) return '';
    const width = 100;
    const height = 32;
    const step = width / (this.history.length - 1);
    return this.history
      .map((entry, i) => {
        const x = i * step;
        const y = height - (entry.probability / 100) * height;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }

  get rootCauseViews(): RootCauseView[] {
    const causes = this.result?.root_causes ?? [];
    return causes.map((c) => {
      const capacity = c.threshold * 2.2;
      const overPct = Math.min((c.value / c.threshold) * 100 - 100, 220);
      return {
        ...c,
        icon: this.iconFor(c.sensor),
        unit: this.unitFor(c.sensor),
        overPct,
        fillPct: Math.min((c.value / capacity) * 100, 100),
        thresholdPct: Math.min((c.threshold / capacity) * 100, 100),
        severity: overPct >= 40 ? 'critical' : overPct >= 10 ? 'warning' : 'ok',
      };
    });
  }

  riskColor(level: string | undefined): string {
    const l = (level ?? '').toLowerCase();
    if (l === 'high') return '#D7373F';
    if (l === 'medium') return '#E8890C';
    return '#00954C';
  }

  riskSoft(level: string | undefined): string {
    const l = (level ?? '').toLowerCase();
    if (l === 'high') return '#FBEAEA';
    if (l === 'medium') return '#FDF1E2';
    return '#E4F5EB';
  }

  severityColor(severity: 'critical' | 'warning' | 'ok'): string {
    if (severity === 'critical') return '#D7373F';
    if (severity === 'warning') return '#E8890C';
    return '#00954C';
  }

  statusColor(): string {
    return this.result?.status === 'ANOMALY' ? '#D7373F' : '#00954C';
  }

  statusSoft(): string {
    return this.result?.status === 'ANOMALY' ? '#FBEAEA' : '#E4F5EB';
  }

  fieldLabel(name: string): string {
    const labels: Record<string, string> = {
      Trip_Time_s: 'Trip time (s)',
      Speed_kmh: 'Speed (km/h)',
      Motor_RPM: 'Motor RPM',
      Motor_Temp_C: 'Motor temp (°C)',
      Battery_SOC_pct: 'Battery SOC (%)',
      Battery_Voltage_V: 'Battery voltage (V)',
      Battery_Current_A: 'Battery current (A)',
      Battery_Temp_C: 'Battery temp (°C)',
      Ambient_Temp_C: 'Ambient temp (°C)',
      Braking_Active: 'Braking active',
      Vehicle_State: 'Vehicle state',
    };
    return labels[name] ?? name;
  }

  fieldType(name: string): 'number' | 'select-braking' | 'select-state' {
    if (name === 'Braking_Active') return 'select-braking';
    if (name === 'Vehicle_State') return 'select-state';
    return 'number';
  }

  private iconFor(sensor: string): string {
    if (sensor.includes('Current')) return 'battery';
    if (sensor.includes('Speed')) return 'gauge';
    if (sensor.includes('Temp')) return 'thermometer';
    if (sensor.includes('RPM')) return 'cpu';
    if (sensor.includes('SOC')) return 'battery-charging';
    return 'activity';
  }

  private unitFor(sensor: string): string {
    const units: Record<string, string> = {
      Battery_Current_A: 'A',
      Speed_kmh: ' km/h',
      Motor_Temp_C: '°C',
      Battery_Temp_C: '°C',
      Motor_RPM: ' rpm',
      Battery_SOC_pct: '%',
    };
    return units[sensor] ?? '';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.error = null;
    this.loading = true;

    this.telemetryService.predictFromFile(file).subscribe({
      next: (res) => {
        this.result = res;
        this.pushHistory(res, 'window');
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error ?? err?.message ?? 'Upload/prediction failed.';
        this.loading = false;
      },
      complete: () => { input.value = ''; }
    });
  }
}