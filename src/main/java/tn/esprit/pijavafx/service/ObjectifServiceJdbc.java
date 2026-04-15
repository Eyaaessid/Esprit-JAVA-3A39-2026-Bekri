package tn.esprit.pijavafx.service;

import tn.esprit.pijavafx.model.ObjectifBienEtreDto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of IObjectifService.
 * Connects directly to MySQL — no Spring Boot backend needed.
 */
public class ObjectifServiceJdbc implements IObjectifService {

    private static final String URL      = "jdbc:mysql://127.0.0.1:3306/bekri_db"
            + "?useSSL=false&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────
    @Override
    public List<ObjectifBienEtreDto> getAll() throws Exception {
        String sql = "SELECT * FROM objectif_bien_etre WHERE utilisateur_id = 1 ORDER BY id DESC";
        List<ObjectifBienEtreDto> list = new ArrayList<>();
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    @Override
    public ObjectifBienEtreDto create(ObjectifBienEtreDto dto) throws Exception {
        // Validation
        validate(dto);

        String sql = "INSERT INTO objectif_bien_etre "
                + "(titre, description, type, valeur_cible, valeur_actuelle, "
                + "date_debut, date_fin, statut, created_at, utilisateur_id, slug) "
                + "VALUES (?,?,?,?,?,?,?,?,NOW(),1,?)";

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, dto.getTitre());
            ps.setString(2, dto.getDescription());
            ps.setString(3, dto.getType());
            ps.setDouble(4, dto.getValeurCible());
            if (dto.getValeurActuelle() != null) ps.setDouble(5, dto.getValeurActuelle());
            else ps.setObject(5, null);
            ps.setDate(6, dto.getDateDebut() != null ? Date.valueOf(dto.getDateDebut()) : null);
            ps.setDate(7, dto.getDateFin()   != null ? Date.valueOf(dto.getDateFin())   : null);
            ps.setString(8, dto.getStatut());
            ps.setString(9, dto.getSlug());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) dto.setId(keys.getInt(1));
            }
        }
        return dto;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    public ObjectifBienEtreDto update(int id, ObjectifBienEtreDto dto) throws Exception {
        validate(dto);

        String sql = "UPDATE objectif_bien_etre "
                + "SET titre=?, description=?, type=?, valeur_cible=?, valeur_actuelle=?, "
                + "date_debut=?, date_fin=?, statut=?, updated_at=NOW(), slug=? "
                + "WHERE id=?";

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, dto.getTitre());
            ps.setString(2, dto.getDescription());
            ps.setString(3, dto.getType());
            ps.setDouble(4, dto.getValeurCible());
            if (dto.getValeurActuelle() != null) ps.setDouble(5, dto.getValeurActuelle());
            else ps.setObject(5, null);
            ps.setDate(6, dto.getDateDebut() != null ? Date.valueOf(dto.getDateDebut()) : null);
            ps.setDate(7, dto.getDateFin()   != null ? Date.valueOf(dto.getDateFin())   : null);
            ps.setString(8, dto.getStatut());
            ps.setString(9, dto.getSlug());
            ps.setInt(10, id);
            ps.executeUpdate();
        }
        dto.setId(id);
        return dto;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM objectif_bien_etre WHERE id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── ResultSet → DTO ───────────────────────────────────────────────────────
    private ObjectifBienEtreDto mapRow(ResultSet rs) throws SQLException {
        ObjectifBienEtreDto dto = new ObjectifBienEtreDto();
        dto.setId(rs.getInt("id"));
        dto.setTitre(rs.getString("titre"));
        dto.setDescription(rs.getString("description"));
        dto.setType(rs.getString("type"));
        dto.setValeurCible(rs.getDouble("valeur_cible"));
        dto.setValeurActuelle(rs.getObject("valeur_actuelle") != null ? rs.getDouble("valeur_actuelle") : null);
        dto.setDateDebut(rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null);
        dto.setDateFin(rs.getDate("date_fin")     != null ? rs.getDate("date_fin").toLocalDate()   : null);
        dto.setStatut(rs.getString("statut"));
        dto.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        dto.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        dto.setUtilisateurId(rs.getInt("utilisateur_id"));
        dto.setSlug(rs.getString("slug"));
        return dto;
    }

    // ── Validation (replaces @Valid from Spring) ───────────────────────────────
    private void validate(ObjectifBienEtreDto dto) throws Exception {
        if (dto.getTitre() == null || dto.getTitre().isBlank())
            throw new Exception("Le titre est obligatoire.");
        if (dto.getTitre().trim().length() < 3)
            throw new Exception("Le titre doit contenir au moins 3 caractères.");
        if (dto.getTitre().trim().length() > 150)
            throw new Exception("Le titre ne peut pas dépasser 150 caractères.");
        if (dto.getType() == null || dto.getType().isBlank())
            throw new Exception("Le type est obligatoire.");
        if (dto.getStatut() == null || dto.getStatut().isBlank())
            throw new Exception("Le statut est obligatoire.");
        if (dto.getValeurCible() == null)
            throw new Exception("La valeur cible est obligatoire.");
        if (dto.getValeurCible() <= 0)
            throw new Exception("La valeur cible doit être un nombre positif.");
        if (dto.getDateDebut() == null)
            throw new Exception("La date de début est obligatoire.");
        if (dto.getDateFin() == null)
            throw new Exception("La date de fin est obligatoire.");
        if (!dto.getDateFin().isAfter(dto.getDateDebut()))
            throw new Exception("La date de fin doit être postérieure à la date de début.");
    }
}