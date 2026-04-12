package com.bekri.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilPsychologiqueResponseDTO {

    private Integer id;
    private Integer utilisateurId;
    private Integer scoreGlobal;
    private String profilType;
    private LocalDateTime dateEvaluation;
    private String aiFeedback;
}
