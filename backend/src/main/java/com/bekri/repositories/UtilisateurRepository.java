package com.bekri.repositories;

import com.bekri.entities.Utilisateur;
import com.bekri.enums.UtilisateurStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByEmail(String email);

    List<Utilisateur> findByRole(String role);

    List<Utilisateur> findByStatut(UtilisateurStatut statut);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    // Native query to update role column directly (discriminator column cannot be updated via JPA)
    @Modifying
    @Query(value = "UPDATE utilisateur SET role = :role WHERE id = :id", nativeQuery = true)
    void updateRoleById(@Param("id") Integer id, @Param("role") String role);

    @Query("""
            select u from Utilisateur u
            where
              (:role is null or lower(u.role) = lower(:role))
              and (:statut is null or u.statut = :statut)
              and (
                :search is null
                or :search = ''
                or lower(u.nom) like lower(concat('%', :search, '%'))
                or lower(u.prenom) like lower(concat('%', :search, '%'))
                or lower(u.email) like lower(concat('%', :search, '%'))
              )
            order by u.id desc
            """)
    List<Utilisateur> search(@Param("search") String search,
                             @Param("role") String role,
                             @Param("statut") UtilisateurStatut statut);
}