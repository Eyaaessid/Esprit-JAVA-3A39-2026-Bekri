package com.bekri.services;

import com.bekri.dto.request.ProfilPsychologiqueRequestDTO;
import com.bekri.dto.response.ProfilPsychologiqueResponseDTO;
import com.bekri.entities.ProfilPsychologique;
import com.bekri.entities.Utilisateur;
import com.bekri.exceptions.ResourceNotFoundException;
import com.bekri.repositories.ProfilPsychologiqueRepository;
import com.bekri.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfilPsychologiqueService {

    private final ProfilPsychologiqueRepository profilPsychologiqueRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public ProfilPsychologiqueResponseDTO getForCurrentUser(Utilisateur utilisateur) {
        if (utilisateur.getId() == null) {
            throw new ResourceNotFoundException("Profil psychologique introuvable");
        }
        return profilPsychologiqueRepository.findByUtilisateurId(utilisateur.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Profil psychologique introuvable"));
    }

    @Transactional
    public ProfilPsychologiqueResponseDTO upsertForCurrentUser(
            Utilisateur utilisateur, ProfilPsychologiqueRequestDTO dto) {
        if (utilisateur.getId() == null) {
            throw new IllegalArgumentException("Utilisateur non persisté");
        }
        Utilisateur managed = utilisateurRepository.getReferenceById(utilisateur.getId());
        ProfilPsychologique entity = profilPsychologiqueRepository
                .findByUtilisateurId(utilisateur.getId())
                .orElseGet(() -> ProfilPsychologique.builder().utilisateur(managed).build());
        if (entity.getUtilisateur() == null) {
            entity.setUtilisateur(managed);
        }
        entity.setScoreGlobal(dto.getScoreGlobal());
        entity.setProfilType(dto.getProfilType());
        entity.setDateEvaluation(dto.getDateEvaluation() != null ? dto.getDateEvaluation() : LocalDateTime.now());
        entity.setAiFeedback(dto.getAiFeedback());
        return toResponse(profilPsychologiqueRepository.save(entity));
    }

    private ProfilPsychologiqueResponseDTO toResponse(ProfilPsychologique p) {
        return ProfilPsychologiqueResponseDTO.builder()
                .id(p.getId())
                .utilisateurId(p.getUtilisateur() != null ? p.getUtilisateur().getId() : null)
                .scoreGlobal(p.getScoreGlobal())
                .profilType(p.getProfilType())
                .dateEvaluation(p.getDateEvaluation())
                .aiFeedback(p.getAiFeedback())
                .build();
    }
}
