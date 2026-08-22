import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'live', pathMatch: 'full' },
  {
    path: 'live',
    loadComponent: () =>
      import('./pages/live-dashboard/live-dashboard.component').then((m) => m.LiveDashboardComponent),
  },
  {
    path: 'devices',
    loadComponent: () => import('./pages/devices/devices.component').then((m) => m.DevicesComponent),
  },
  {
    path: 'devices/:vehicleId',
    loadComponent: () => import('./pages/analytics/analytics.component').then((m) => m.AnalyticsComponent),
  },
  {
    path: 'analytics',
    loadComponent: () => import('./pages/analytics/analytics.component').then((m) => m.AnalyticsComponent),
  },
  {
    path: 'alerts',
    loadComponent: () => import('./pages/alerts/alerts.component').then((m) => m.AlertsComponent),
  },
  {
    path: 'simulator',
    loadComponent: () => import('./pages/simulator/simulator.component').then((m) => m.SimulatorComponent),
  },
  {
    path: 'model-test',
    loadComponent: () => import('./pages/model-test/model-test.component').then((m) => m.ModelTestComponent),
  },
  { path: '**', redirectTo: 'live' },
];