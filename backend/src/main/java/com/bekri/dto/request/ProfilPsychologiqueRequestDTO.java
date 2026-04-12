package com.bekri.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilPsychologiqueRequestDTO {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer scoreGlobal;

    @NotBlank
    private String profilType;

    private LocalDateTime dateEvaluation;

    private String aiFeedback;
}
