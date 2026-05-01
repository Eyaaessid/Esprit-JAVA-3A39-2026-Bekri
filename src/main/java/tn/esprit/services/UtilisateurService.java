package tn.esprit.services;

import tn.esprit.models.Utilisateur;
import tn.esprit.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC à la table {@code utilisateur} (lecture seule pour ce module).
 */
public class UtilisateurService {

    /**
     * Indique si un utilisateur existe pour cet identifiant (clé primaire).
     */
    public boolean existsById(int id) {
        String sql = "SELECT 1 FROM utilisateur WHERE id = ? LIMIT 1";
        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.err.println("UtilisateurService.existsById: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Liste tous les utilisateurs pour alimenter un ComboBox.
     * Colonnes attendues : {@code id}, {@code nom}, {@code prénom}.
     */
    public List<Utilisateur> afficherAll() {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT id, nom, prenom FROM utilisateur ORDER BY id";
        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Utilisateur u = new Utilisateur();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                list.add(u);
            }
        } catch (SQLException ex) {
            System.err.println("UtilisateurService.afficherAll: " + ex.getMessage());
            ex.printStackTrace();
        }
        return list;
    }
}
