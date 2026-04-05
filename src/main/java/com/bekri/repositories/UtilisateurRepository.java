package com.bekri.repositories;

import com.bekri.entities.Utilisateur;
import com.bekri.enums.UtilisateurRole;
import com.bekri.enums.UtilisateurStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByEmail(String email);

    List<Utilisateur> findByRole(UtilisateurRole role);

    List<Utilisateur> findByStatut(UtilisateurStatut statut);

    boolean existsByEmail(String email);
}
