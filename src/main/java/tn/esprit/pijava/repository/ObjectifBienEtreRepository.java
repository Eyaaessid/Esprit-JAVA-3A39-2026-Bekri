package tn.esprit.pijava.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.pijava.entity.ObjectifBienEtre;

import java.util.List;

public interface ObjectifBienEtreRepository extends JpaRepository<ObjectifBienEtre, Integer> {
    List<ObjectifBienEtre> findByUtilisateurId(Integer utilisateurId);
}