import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter } from 'rxjs/operators';
import { TelemetryPrediction } from '../models/telemetry.model';

/**
 * Wraps the STOMP connection to Spring's WebSocketConfig (/ws endpoint,
 * /topic/telemetry and /topic/telemetry/{vehicleId} destinations).
 *
 * One shared connection for the whole app: components subscribe to the
 * Observables below rather than each opening their own socket.
 */
@Injectable({ providedIn: 'root' })
export class TelemetryWebsocketService implements OnDestroy {
  private client: Client;

  private connectedSubject = new BehaviorSubject<boolean>(false);
  readonly connected$ = this.connectedSubject.asObservable();

  // Every message published on /topic/telemetry (all vehicles) -- feeds the
  // Live Dashboard's devices table.
  private globalSubject = new Subject<TelemetryPrediction>();
  readonly global$: Observable<TelemetryPrediction> = this.globalSubject.asObservable();

  // Per-vehicle subjects, created lazily -- feeds the Analytics page /
  // Devices detail view for one vehicle at a time.
  private vehicleSubjects = new Map<string, Subject<TelemetryPrediction>>();
  private vehicleSubscriptions = new Set<string>();

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.onConnect = () => {
      this.connectedSubject.next(true);

      this.client.subscribe('/topic/telemetry', (message: IMessage) => {
        this.globalSubject.next(JSON.parse(message.body));
      });

      // Re-subscribe to any per-vehicle topics that were requested before
      // the connection was ready (or after a reconnect).
      this.vehicleSubscriptions.forEach((vehicleId) => this.subscribeToVehicleTopic(vehicleId));
    };

    this.client.onDisconnect = () => this.connectedSubject.next(false);
    this.client.onWebSocketClose = () => this.connectedSubject.next(false);

    this.client.activate();
  }

  /** Live stream of predictions/collecting-progress for one vehicle. */
  forVehicle(vehicleId: string): Observable<TelemetryPrediction> {
    if (!this.vehicleSubjects.has(vehicleId)) {
      this.vehicleSubjects.set(vehicleId, new Subject<TelemetryPrediction>());
    }
    if (!this.vehicleSubscriptions.has(vehicleId)) {
      this.vehicleSubscriptions.add(vehicleId);
      if (this.client.connected) {
        this.subscribeToVehicleTopic(vehicleId);
      }
    }
    return this.vehicleSubjects.get(vehicleId)!.asObservable();
  }

  /** Global stream filtered down to anomalies only, handy for an alerts banner/page. */
  anomalies$(): Observable<TelemetryPrediction> {
    return this.global$.pipe(filter((p) => p.status === 'ANOMALY'));
  }

  private subscribeToVehicleTopic(vehicleId: string) {
    this.client.subscribe(`/topic/telemetry/${vehicleId}`, (message: IMessage) => {
      this.vehicleSubjects.get(vehicleId)?.next(JSON.parse(message.body));
    });
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }
}
