import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-score-chart',
  standalone: true,
  template: `<canvas #canvas></canvas>`,
  styles: [
    `
      :host {
        display: block;
        height: 200px;
        position: relative;
      }
      canvas {
        width: 100% !important;
        height: 100% !important;
      }
    `,
  ],
})
export class ScoreChartComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @Input() labels: string[] = [];
  @Input() values: number[] = [];

  private chart?: Chart;

  ngAfterViewInit(): void {
    this.render();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.chart && (changes['labels'] || changes['values'])) {
      this.chart.data.labels = this.labels;
      this.chart.data.datasets[0].data = this.values;
      this.chart.update();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private render() {
    // ACTIA tokens (Chart.js needs literal values, can't read CSS custom
    // properties from here) -- keep these in sync with styles.scss:
    //   --accent-red: #D7373F / --accent-red-soft: #FBEAEA
    //   --line: #E6E7E8 / --text-muted: #8C8D8F
    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: this.labels,
        datasets: [
          {
            label: 'Anomaly probability',
            data: this.values,
            borderColor: '#D7373F',
            backgroundColor: 'rgba(215, 55, 63, 0.08)',
            fill: true,
            tension: 0.3,
            pointRadius: 2,
            borderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: {
            min: 0,
            max: 1,
            ticks: { color: '#8C8D8F', stepSize: 0.25 },
            grid: { color: '#E6E7E8' },
          },
          x: {
            ticks: { color: '#8C8D8F' },
            grid: { display: false },
          },
        },
      },
    };
    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }
}