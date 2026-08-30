package com.irinfosys.websocket.multi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DeviceDataUploadRequest(
    @NotBlank String imei,
    String command,
    @NotNull Map<String, Object> data
) {}
