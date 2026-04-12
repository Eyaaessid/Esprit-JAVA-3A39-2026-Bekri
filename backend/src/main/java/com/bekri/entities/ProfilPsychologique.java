package com.bekri.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilPsychologique {

    private Integer id;
    private Integer utilisateurId;
    private Integer scoreGlobal;
    private String profilType;
    private LocalDateTime dateEvaluation;
    private String aiFeedback;
}
