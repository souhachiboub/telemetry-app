import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, OperatorFunction, throwError } from 'rxjs';
import { PredictionResponse, TelemetryPredictionRecord, TelemetryReading, TelemetryUploadResult } from '../models/telemetry.model';

@Injectable({ providedIn: 'root' })
export class TelemetryApiService {

  private readonly base = '/api/telemetry';

  constructor(private http: HttpClient) {}
 private handleError<T>(context: string): OperatorFunction<T, T> {
    return catchError((err) => {
      console.error(context, err);
      return throwError(() => err);
    });
  }
  
  history(vehicleId: string): Observable<TelemetryPredictionRecord[]> {
    return this.http.get<TelemetryPredictionRecord[]>(`${this.base}/${vehicleId}/history`);
  }

  
  devices(): Observable<TelemetryPredictionRecord[]> {
    return this.http.get<TelemetryPredictionRecord[]>(`${this.base}/devices`);
  }

  
  anomalies(): Observable<TelemetryPredictionRecord[]> {
    return this.http.get<TelemetryPredictionRecord[]>(`${this.base}/anomalies`);
  }


  uploadTelemetryFile(file: File): Observable<TelemetryUploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<TelemetryUploadResult>(`${this.base}/upload`, formData);
  }


  predictManual(reading: TelemetryReading): Observable<PredictionResponse> {
  return this.http
    .post<PredictionResponse>(`${this.base}/predict/manual`, reading)
    .pipe(this.handleError('Erreur predict manual'));
}

predictFromFile(file: File): Observable<PredictionResponse> {
  const formData = new FormData();
  formData.append('file', file, file.name);
  return this.http
    .post<PredictionResponse>(`${this.base}/predict/upload`, formData)
    .pipe(this.handleError('Erreur predict from file'));
}
}
