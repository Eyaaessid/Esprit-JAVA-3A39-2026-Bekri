package com.bekri.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponseDTO {

    private int status;
    private String message;
    private LocalDateTime timestamp;
}
