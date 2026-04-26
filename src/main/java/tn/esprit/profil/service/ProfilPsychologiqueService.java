package tn.esprit.profil.service;

import tn.esprit.profil.dao.ProfilPsychologiqueDao;
import tn.esprit.profil.entity.ProfilPsychologique;

import java.util.List;
import java.util.Optional;

public class ProfilPsychologiqueService {
    private final ProfilPsychologiqueDao dao = new ProfilPsychologiqueDao();

    public ProfilPsychologique submitProfil(Integer utilisateurId, int scoreGlobal,
                                            String profilType, String aiFeedback) {
        ProfilPsychologique profil = new ProfilPsychologique();
        profil.setUtilisateurId(utilisateurId);
        profil.setScoreGlobal(scoreGlobal);
        profil.setProfilType(profilType);
        profil.setAiFeedback(aiFeedback);
        return dao.save(profil);
    }

    public Optional<ProfilPsychologique> getProfilForUser(Integer utilisateurId) {
        return dao.findByUtilisateurId(utilisateurId);
    }

    public List<ProfilPsychologique> getAllProfils() {
        return dao.findAll();
    }

    public ProfilPsychologique updateAiFeedback(Integer utilisateurId, String newFeedback) {
        Optional<ProfilPsychologique> opt = dao.findByUtilisateurId(utilisateurId);
        if (opt.isEmpty()) throw new RuntimeException("Profil introuvable");
        ProfilPsychologique p = opt.get();
        p.setAiFeedback(newFeedback);
        return dao.save(p);
    }
}
