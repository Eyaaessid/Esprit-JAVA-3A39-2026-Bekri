package tn.esprit.objectif.dao;

import tn.esprit.objectif.entity.ObjectifBienEtre;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ObjectifBienEtreDao {
    private final Connection cnx = MyDataBase.getInstance().getCnx();

    public List<ObjectifBienEtre> findByUtilisateurId(Integer utilisateurId) {
        String sql = "SELECT * FROM objectif_bien_etre WHERE utilisateur_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ObjectifBienEtre> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<ObjectifBienEtre> findById(Integer id) {
        String sql = "SELECT * FROM objectif_bien_etre WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectifBienEtre save(ObjectifBienEtre o) {
        try {
            if (o.getId() == null) {
                String sql = "INSERT INTO objectif_bien_etre "
                        + "(titre, description, type, valeur_cible, valeur_actuelle, "
                        + "date_debut, date_fin, statut, created_at, utilisateur_id, slug) "
                        + "VALUES (?,?,?,?,?,?,?,?,NOW(),?,?)";
                try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, o.getTitre());
                    ps.setString(2, o.getDescription());
                    ps.setString(3, o.getType());
                    ps.setDouble(4, o.getValeurCible());
                    if (o.getValeurActuelle() != null) {
                        ps.setDouble(5, o.getValeurActuelle());
                    } else {
                        ps.setObject(5, null);
                    }
                    ps.setDate(6, o.getDateDebut() != null ? Date.valueOf(o.getDateDebut()) : null);
                    ps.setDate(7, o.getDateFin() != null ? Date.valueOf(o.getDateFin()) : null);
                    ps.setString(8, o.getStatut());
                    ps.setInt(9, o.getUtilisateurId());
                    ps.setString(10, o.getSlug());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            o.setId(keys.getInt(1));
                        }
                    }
                }
                return o;
            }

            String sql = "UPDATE objectif_bien_etre "
                    + "SET titre=?, description=?, type=?, valeur_cible=?, "
                    + "valeur_actuelle=?, date_debut=?, date_fin=?, "
                    + "statut=?, updated_at=NOW(), utilisateur_id=?, slug=? "
                    + "WHERE id=?";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, o.getTitre());
                ps.setString(2, o.getDescription());
                ps.setString(3, o.getType());
                ps.setDouble(4, o.getValeurCible());
                if (o.getValeurActuelle() != null) {
                    ps.setDouble(5, o.getValeurActuelle());
                } else {
                    ps.setObject(5, null);
                }
                ps.setDate(6, o.getDateDebut() != null ? Date.valueOf(o.getDateDebut()) : null);
                ps.setDate(7, o.getDateFin() != null ? Date.valueOf(o.getDateFin()) : null);
                ps.setString(8, o.getStatut());
                ps.setInt(9, o.getUtilisateurId());
                ps.setString(10, o.getSlug());
                ps.setInt(11, o.getId());
                ps.executeUpdate();
            }
            return o;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsById(Integer id) {
        String sql = "SELECT COUNT(*) FROM objectif_bien_etre WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(Integer id) {
        String sql = "DELETE FROM objectif_bien_etre WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectifBienEtre mapRow(ResultSet rs) throws SQLException {
        ObjectifBienEtre o = new ObjectifBienEtre();
        o.setId(rs.getInt("id"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        o.setType(rs.getString("type"));
        o.setValeurCible(rs.getDouble("valeur_cible"));
        o.setValeurActuelle(rs.getObject("valeur_actuelle") != null ? rs.getDouble("valeur_actuelle") : null);
        o.setDateDebut(rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null);
        o.setDateFin(rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : null);
        o.setStatut(rs.getString("statut"));
        o.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        o.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        o.setUtilisateurId(rs.getInt("utilisateur_id"));
        o.setSlug(rs.getString("slug"));
        return o;
    }
}
