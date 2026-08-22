
export const WINDOW_SIZE = 15;
export interface RootCause {
  sensor: string;
  value: number;
  threshold: number;
  issue: string;
}
export type TelemetryStatus = 'COLLECTING' | 'NORMAL' | 'ANOMALY' | 'ERROR';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface TelemetryPrediction {
  status: TelemetryStatus;

 
  readings_received?: number;
  readings_required?: number;

  vehicle_id?: string;

  
  probability?: number;
  risk_level?: RiskLevel;
  label?: number;
  model_used?: string;
  root_causes?: RootCause[];
  recommendation?: string;

  
  message?: string;
  errorType?: string;
  timestamp?: string;
}


export interface TelemetryPredictionRecord {
  id: number;
  vehicleId: string;
  status: 'NORMAL' | 'ANOMALY';
  probability: number;
  riskLevel: RiskLevel;
  label: number;
  modelUsed: string;
  recommendation: string;
  rootCauses: RootCause[];
  createdAt: string;
}


export interface TelemetryReading {
  Trip_Time_s: number;
  Speed_kmh: number;
  Motor_RPM: number;
  Motor_Temp_C: number;
  Battery_SOC_pct: number;
  Battery_Voltage_V: number;
  Battery_Current_A: number;
  Battery_Temp_C: number;
  Ambient_Temp_C: number;
  Braking_Active: 'ON' | 'OFF';
  Vehicle_State: string;
}
export interface PredictionResponse {
  status: 'ANOMALY' | 'NORMAL';
  probability: number;
  risk_level: 'LOW' | 'MEDIUM' | 'HIGH';
  label: number;
  model_used: string;
  root_causes: RootCause[];
  recommendation: string;
}


export interface TelemetryUploadResult {
  success: boolean;
  vehicleId?: string;
  recordsReceived?: number;
  error?: string;
}

export const EMPTY_READING: TelemetryReading = {
  Trip_Time_s: 0,
  Speed_kmh: 0,
  Motor_RPM: 0,
  Motor_Temp_C: 20,
  Battery_SOC_pct: 80,
  Battery_Voltage_V: 355,
  Battery_Current_A: 0,
  Battery_Temp_C: 25,
  Ambient_Temp_C: 24,
  Braking_Active: 'OFF',
  Vehicle_State: 'Idle',
};