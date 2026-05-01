package tn.esprit.services;

import tn.esprit.models.ParticipationEvenement;
import tn.esprit.models.ParticipationDisplay;
import tn.esprit.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipationService {

    /**
     * Requête utilisée pour « Mes participations » (liste enrichie).
     * LEFT JOIN : une ligne de participation s’affiche même si l’événement référencé est absent
     * (un INNER JOIN exclurait alors la participation, ce qui ressemble à un « refresh » cassé).
     */
    private static final String SQL_AFFICHER_ENRICHED =
            "SELECT " +
            "p.id AS participation_id, " +
            "p.date_inscription, " +
            "p.statut AS participation_statut, " +
            "p.commentaire, " +
            "p.utilisateur_id, " +
            "p.evenement_id, " +
            "e.titre AS evenement_titre, " +
            "e.type AS evenement_type, " +
            "e.lieu AS evenement_lieu, " +
            "e.date_debut AS evenement_date_debut, " +
            "e.capacite_max AS evenement_capacite_max, " +
            "e.statut AS evenement_statut " +
            "FROM participation_evenement p " +
            "LEFT JOIN evenement e ON p.evenement_id = e.id " +
            "ORDER BY p.id DESC";

    public void ajouter(ParticipationEvenement p) {
        String query = "INSERT INTO participation_evenement (date_inscription, statut, commentaire, " +
                      "utilisateur_id, evenement_id) VALUES (?, ?, ?, ?, ?)";
        
        PreparedStatement ps = null;
        ResultSet genKeys = null;
        try {
            Connection conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            
            System.out.println("=== ParticipationService.ajouter() ===");
            System.out.println("Requête préparée: " + query);
            System.out.println("Valeurs bindées → utilisateur_id=" + p.getUtilisateur_id()
                    + ", evenement_id=" + p.getEvenement_id()
                    + ", statut=" + p.getStatut()
                    + ", commentaire=" + p.getCommentaire()
                    + ", date_inscription=" + p.getDate_inscription());
            
            ps.setTimestamp(1, Timestamp.valueOf(p.getDate_inscription()));
            ps.setString(2, p.getStatut());
            ps.setString(3, p.getCommentaire());
            ps.setInt(4, p.getUtilisateur_id());
            ps.setInt(5, p.getEvenement_id());
            
            int rowsAffected = ps.executeUpdate();
            System.out.println("executeUpdate() → " + rowsAffected + " (attendu: 1)");
            
            if (rowsAffected != 1) {
                throw new SQLException("INSERT: executeUpdate a retourné " + rowsAffected + " au lieu de 1");
            }
            
            genKeys = ps.getGeneratedKeys();
            if (genKeys.next()) {
                int generatedId = genKeys.getInt(1);
                p.setId(generatedId);
                System.out.println("Clé générée (getGeneratedKeys) → id=" + generatedId);
            } else {
                System.out.println("Avertissement: getGeneratedKeys() vide (id non renseigné par le driver)");
            }
            
            debugSelectDerniereLigneParticipation(conn);
            boolean trouvee = verifierParticipationParUtilisateurEtEvenement(conn, p.getUtilisateur_id(), p.getEvenement_id());
            System.out.println("Vérification forte SELECT par (utilisateur_id=" + p.getUtilisateur_id()
                    + ", evenement_id=" + p.getEvenement_id() + ") → " + (trouvee ? "TROUVÉE" : "NON TROUVÉE"));
            
            System.out.println("=== Fin ajouter() OK, id participation=" + p.getId() + " ===");
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de l'ajout de la participation : " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Échec insertion participation: " + ex.getMessage(), ex);
        } finally {
            if (genKeys != null) {
                try {
                    genKeys.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /** Après insertion : SELECT * … ORDER BY id DESC, affiche la première ligne (id le plus grand). */
    private void debugSelectDerniereLigneParticipation(Connection conn) throws SQLException {
        String sql = "SELECT * FROM participation_evenement ORDER BY id DESC";
        System.out.println("--- Contrôle post-insert (dernière ligne par id) ---");
        System.out.println("SQL: " + sql);
        try (PreparedStatement dbg = conn.prepareStatement(sql);
             ResultSet rs = dbg.executeQuery()) {
            if (rs.next()) {
                System.out.println("Dernière ligne [premier résultat trié DESC]: id=" + rs.getInt("id")
                        + ", utilisateur_id=" + rs.getInt("utilisateur_id")
                        + ", evenement_id=" + rs.getInt("evenement_id")
                        + ", statut=" + rs.getString("statut")
                        + ", date_inscription=" + rs.getTimestamp("date_inscription"));
            } else {
                System.out.println("Table participation_evenement: aucune ligne (inattendu après INSERT).");
            }
        }
    }

    private boolean verifierParticipationParUtilisateurEtEvenement(Connection conn, int utilisateurId, int evenementId) throws SQLException {
        String sql = "SELECT id FROM participation_evenement WHERE utilisateur_id = ? AND evenement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void modifier(ParticipationEvenement p) {
        String query = "UPDATE participation_evenement SET date_inscription = ?, statut = ?, " +
                      "commentaire = ?, utilisateur_id = ?, evenement_id = ? WHERE id = ?";
        
        PreparedStatement ps = null;
        try {
            Connection conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            
            ps.setTimestamp(1, Timestamp.valueOf(p.getDate_inscription()));
            ps.setString(2, p.getStatut());
            ps.setString(3, p.getCommentaire());
            ps.setInt(4, p.getUtilisateur_id());
            ps.setInt(5, p.getEvenement_id());
            ps.setInt(6, p.getId());
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✓ Participation modifiée avec succès !");
            } else {
                System.out.println("✗ Aucune participation trouvée avec l'ID : " + p.getId());
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de la modification de la participation : " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void supprimer(int id) {
        String query = "DELETE FROM participation_evenement WHERE id = ?";
        
        PreparedStatement ps = null;
        try {
            Connection conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, id);
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✓ Participation supprimée avec succès !");
            } else {
                System.out.println("✗ Aucune participation trouvée avec l'ID : " + id);
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de la suppression de la participation : " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public List<ParticipationEvenement> afficherAll() {
        List<ParticipationEvenement> participations = new ArrayList<>();
        String query = "SELECT * FROM participation_evenement ORDER BY date_inscription DESC";
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                ParticipationEvenement participation = new ParticipationEvenement();
                participation.setId(rs.getInt("id"));
                participation.setDate_inscription(rs.getTimestamp("date_inscription").toLocalDateTime());
                participation.setStatut(rs.getString("statut"));
                participation.setCommentaire(rs.getString("commentaire"));
                participation.setUtilisateur_id(rs.getInt("utilisateur_id"));
                participation.setEvenement_id(rs.getInt("evenement_id"));
                
                participations.add(participation);
            }
            
            System.out.println("✓ " + participations.size() + " participation(s) récupérée(s)");
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de la récupération des participations : " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        return participations;
    }
    
    /**
     * Récupère toutes les participations avec les informations enrichies des événements
     * Utilise un JOIN pour afficher des données plus utiles à l'utilisateur
     * @return Liste de ParticipationDisplay avec les infos de l'événement
     */
    public List<ParticipationDisplay> afficherAllEnriched() {
        List<ParticipationDisplay> participationsDisplay = new ArrayList<>();
        
        String query = SQL_AFFICHER_ENRICHED;
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            
            System.out.println("=== afficherAllEnriched() — « Mes participations » ===");
            System.out.println("Requête complète (LEFT JOIN, pas de LIMIT, tri p.id DESC):");
            System.out.println(query);
            
            rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                
                int participationId = rs.getInt("participation_id");
                int evenementId = rs.getInt("evenement_id");
                String evenementTitre = rs.getString("evenement_titre");
                if (evenementTitre == null) {
                    evenementTitre = "(événement introuvable — evenement_id=" + evenementId + ")";
                }
                
                System.out.println("  → Ligne " + count + ": p.id=" + participationId
                        + ", utilisateur_id=" + rs.getInt("utilisateur_id")
                        + ", evenement_id=" + evenementId
                        + ", titre='" + evenementTitre + "'");
                
                ParticipationEvenement participation = new ParticipationEvenement();
                participation.setId(participationId);
                participation.setDate_inscription(rs.getTimestamp("date_inscription").toLocalDateTime());
                participation.setStatut(rs.getString("participation_statut"));
                participation.setCommentaire(rs.getString("commentaire"));
                participation.setUtilisateur_id(rs.getInt("utilisateur_id"));
                participation.setEvenement_id(evenementId);
                
                ParticipationDisplay display = new ParticipationDisplay();
                display.setParticipationId(participationId);
                display.setEvenementTitre(evenementTitre);
                display.setEvenementType(rs.getString("evenement_type"));
                display.setEvenementLieu(rs.getString("evenement_lieu"));
                Timestamp tsDebut = rs.getTimestamp("evenement_date_debut");
                display.setEvenementDateDebut(tsDebut != null ? tsDebut.toLocalDateTime() : null);
                int cap = rs.getInt("evenement_capacite_max");
                if (rs.wasNull()) {
                    display.setEvenementCapaciteMax(0);
                } else {
                    display.setEvenementCapaciteMax(cap);
                }
                display.setParticipationStatut(rs.getString("participation_statut"));
                display.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
                display.setParticipationOriginal(participation);
                
                participationsDisplay.add(display);
            }
            
            System.out.println("✓ TOTAL lignes chargées: " + participationsDisplay.size());
            
        } catch (SQLException ex) {
            System.err.println("✗ Erreur SQL lors de la récupération des participations enrichies : " + ex.getMessage());
            System.err.println("✗ Code erreur SQL: " + ex.getErrorCode());
            System.err.println("✗ État SQL: " + ex.getSQLState());
            ex.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        return participationsDisplay;
    }

    /**
     * Compte le nombre de participations par evenement (pour l'affichage des cartes).
     * Ne change pas la structure de la base, simple SELECT + GROUP BY.
     */
    public Map<Integer, Integer> getParticipantsCountMap() {
        Map<Integer, Integer> counts = new HashMap<>();
        String query = "SELECT evenement_id, COUNT(*) AS total FROM participation_evenement GROUP BY evenement_id";

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = MyConnection.getInstance().prepareStatement(query);
            rs = ps.executeQuery();
            while (rs.next()) {
                counts.put(rs.getInt("evenement_id"), rs.getInt("total"));
            }
        } catch (SQLException ex) {
            System.err.println("Erreur comptage participations: " + ex.getMessage());
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }

        return counts;
    }
}
