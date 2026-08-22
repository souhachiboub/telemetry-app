package com.telemetry.backend.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetry.backend.dto.TelemetryDto;
import com.telemetry.backend.dto.TelemetryFileDto;
import com.telemetry.backend.service.TelemetryProcessingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.Map;

@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "${app.frontend-url}")
public class TelemetryUploadController {


    private final ObjectMapper objectMapper;
    private final TelemetryProcessingService processingService;

    public TelemetryUploadController(
            ObjectMapper objectMapper,
            TelemetryProcessingService processingService) {

        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadTelemetry(
            @RequestParam("file") MultipartFile file) {

        try {

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "error", "File is empty"
                        )
                );
            }

            // Read the complete JSON object
            TelemetryFileDto telemetryFile =
                    objectMapper.readValue(
                            file.getInputStream(),
                            TelemetryFileDto.class
                    );

            // Validate vehicle ID
            if (telemetryFile.getVehicleId() == null ||
                    telemetryFile.getVehicleId().isBlank()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "error", "vehicleId is missing"
                        )
                );
            }

            // Validate window
            if (telemetryFile.getWindow() == null ||
                    telemetryFile.getWindow().isEmpty()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "error", "Telemetry window is empty"
                        )
                );
            }

            String vehicleId =
                    telemetryFile.getVehicleId();

            /*
             * Process every telemetry reading.
             */
            for (TelemetryDto telemetry :
                    telemetryFile.getWindow()) {

                String telemetryJson =
                        objectMapper.writeValueAsString(
                                telemetry
                        );

                processingService.processTelemetry(
                        vehicleId,
                        telemetryJson
                );
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "vehicleId", vehicleId,
                            "recordsReceived",
                            telemetryFile.getWindow().size()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        }
    }
}