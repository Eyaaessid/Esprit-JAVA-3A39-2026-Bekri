package com.bekri.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Coach hérite de Utilisateur (ROLE_USER + ROLE_COACH).
 *
 * Héritage comportemental :
 *  - Hérite de tous les droits User (voir son propre profil, profil psychologique, etc.)
 *  - A EN PLUS accès aux endpoints /api/coach/* :
 *      → Tableau de bord coach
 *      → Voir la liste des utilisateurs (pour organiser des événements)
 *
 * Pas de champs supplémentaires en base (table partagée Symfony + Java).
 * L'héritage est visible en session via le JWT : claim "role" = "coach".
 */
@Entity
@DiscriminatorValue("coach")
@NoArgsConstructor
public class Coach extends Utilisateur {
}