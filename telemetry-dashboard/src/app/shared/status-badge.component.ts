import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="badge" [class]="'badge--' + tone()">{{ label }}</span>`,
  styles: [
    `
      .badge {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 2px 9px;
        border-radius: 999px;
        font-size: 11.5px;
        font-weight: 600;
        letter-spacing: 0.01em;
      }
      .badge::before {
        content: '';
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: currentColor;
      }
      .badge--good { background: var(--bg-green); color: var(--accent-green); }
      .badge--warn { background: var(--bg-amber); color: var(--accent-amber); }
      .badge--bad { background: var(--bg-red); color: var(--accent-red); }
      .badge--neutral { background: var(--panel-raised); color: var(--text-muted); }
    `,
  ],
})
export class StatusBadgeComponent {
  @Input({ required: true }) label!: string;
  /** One of: NORMAL, ANOMALY, LOW, MEDIUM, HIGH, COLLECTING, ERROR (case-insensitive). */
  @Input({ required: true }) status!: string;

  tone(): 'good' | 'warn' | 'bad' | 'neutral' {
    const s = (this.status || '').toUpperCase();
    if (s === 'NORMAL' || s === 'LOW') return 'good';
    if (s === 'MEDIUM' || s === 'COLLECTING') return 'warn';
    if (s === 'ANOMALY' || s === 'HIGH' || s === 'ERROR') return 'bad';
    return 'neutral';
  }
}
