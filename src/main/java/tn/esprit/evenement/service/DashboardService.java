package tn.esprit.evenement.service;

import tn.esprit.evenement.entity.DashboardStats;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardService {
    private Connection cnx() {
        return MyDataBase.getInstance().getCnx();
    }

    public DashboardStats getStatistiques() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalEvenements(count("SELECT COUNT(*) FROM evenement"));
        stats.setTotalOuverts(count("SELECT COUNT(*) FROM evenement WHERE statut='OPEN'"));
        stats.setTotalFermes(count("SELECT COUNT(*) FROM evenement WHERE statut='CLOSED' OR statut='ANNULE'"));
        stats.setTotalPlanifies(count("SELECT COUNT(*) FROM evenement WHERE statut='PLANIFIE'"));
        stats.setTotalParticipations(count("SELECT COUNT(*) FROM participation_evenement"));
        return stats;
    }

    public DashboardStats getStatistiquesCoach(int coachId) {
        DashboardStats stats = new DashboardStats();
        stats.setTotalMesEvenements(count("SELECT COUNT(*) FROM evenement WHERE coach_id=?", coachId));
        stats.setTotalParticipantsMesEvenements(
                count("SELECT COUNT(*) FROM participation_evenement p JOIN evenement e ON e.id=p.evenement_id WHERE e.coach_id=?", coachId)
        );
        return stats;
    }

    private int count(String sql, Object... params) {
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object value = params[i];
                if (value instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    ps.setObject(i + 1, value);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Erreur dashboard statistiques: " + ex.getMessage(), ex);
        }
    }
}
