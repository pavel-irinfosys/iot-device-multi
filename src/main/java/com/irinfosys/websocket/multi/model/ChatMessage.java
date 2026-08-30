package com.irinfosys.websocket.multi.model;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record ChatMessage(
    @NotBlank String sender,
    @NotBlank String content,
    Instant sentAt
) {

}
