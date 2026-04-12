package com.bekri.repositories;

import com.bekri.entities.ProfilPsychologique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfilPsychologiqueRepository extends JpaRepository<ProfilPsychologique, Integer> {

    @Query("SELECT p FROM ProfilPsychologique p WHERE p.utilisateur.id = :utilisateurId")
    Optional<ProfilPsychologique> findByUtilisateurId(@Param("utilisateurId") Integer utilisateurId);
}
