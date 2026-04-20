package tn.esprit.user.dao;

import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UtilisateurDao {
    private final Connection cnx = MyDataBase.getInstance().getCnx();

    public Optional<Utilisateur> findByEmail(String email) {
        String sql = "SELECT * FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
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

    public Optional<Utilisateur> findById(Integer id) {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
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

    public List<Utilisateur> findAll() {
        String sql = "SELECT * FROM utilisateur ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Utilisateur> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Utilisateur save(Utilisateur u) {
        try {
            if (u.getId() == null) {
                String sql = "INSERT INTO utilisateur "
                        + "(nom, prenom, email, mot_de_passe, role, statut, avatar, telephone, date_naissance, created_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,NOW())";
                try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, u.getNom());
                    ps.setString(2, u.getPrenom());
                    ps.setString(3, u.getEmail());
                    ps.setString(4, u.getMotDePasse());
                    ps.setString(5, u.getRole() != null ? u.getRole().name() : null);
                    ps.setString(6, u.getStatut() != null ? u.getStatut().name() : null);
                    ps.setString(7, u.getPhotoProfil());
                    ps.setString(8, u.getTelephone());
                    if (u.getDateNaissance() != null) {
                        ps.setDate(9, Date.valueOf(u.getDateNaissance()));
                    } else {
                        ps.setObject(9, null);
                    }
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            u.setId(keys.getInt(1));
                        }
                    }
                }
                return u;
            }

            String sql = "UPDATE utilisateur SET nom=?, prenom=?, email=?, mot_de_passe=?, role=?, "
                    + "statut=?, avatar=?, telephone=?, date_naissance=?, updated_at=NOW() WHERE id=?";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, u.getNom());
                ps.setString(2, u.getPrenom());
                ps.setString(3, u.getEmail());
                ps.setString(4, u.getMotDePasse());
                ps.setString(5, u.getRole() != null ? u.getRole().name() : null);
                ps.setString(6, u.getStatut() != null ? u.getStatut().name() : null);
                ps.setString(7, u.getPhotoProfil());
                ps.setString(8, u.getTelephone());
                if (u.getDateNaissance() != null) {
                    ps.setDate(9, Date.valueOf(u.getDateNaissance()));
                } else {
                    ps.setObject(9, null);
                }
                ps.setInt(10, u.getId());
                ps.executeUpdate();
            }
            return u;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(Integer id) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
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

    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        String roleStr = rs.getString("role");
        if (roleStr != null && !roleStr.isBlank()) {
            u.setRole(UtilisateurRole.valueOf(roleStr.trim().toUpperCase()));
        }
        String statutStr = rs.getString("statut");
        if (statutStr != null && !statutStr.isBlank()) {
            u.setStatut(UtilisateurStatut.valueOf(statutStr.trim().toUpperCase()));
        }
        u.setPhotoProfil(rs.getString("avatar"));
        Timestamp ca = rs.getTimestamp("created_at");
        u.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        u.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        try {
            u.setTelephone(rs.getString("telephone"));
        } catch (SQLException ignored) {
            u.setTelephone(null);
        }
        try {
            Date d = rs.getDate("date_naissance");
            u.setDateNaissance(d != null ? d.toLocalDate() : null);
        } catch (SQLException ignored) {
            u.setDateNaissance(null);
        }
        return u;
    }
}
