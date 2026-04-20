package tn.esprit.objectif.service;

import tn.esprit.objectif.dao.ObjectifBienEtreDao;
import tn.esprit.objectif.entity.ObjectifBienEtre;
import tn.esprit.objectif.validation.ObjectifBienEtreValidator;

import java.util.List;
import java.util.Optional;

public class ObjectifBienEtreService {
    private final ObjectifBienEtreDao dao;
    private final ObjectifBienEtreValidator validator;

    public ObjectifBienEtreService() {
        this(new ObjectifBienEtreDao(), new ObjectifBienEtreValidator());
    }

    public ObjectifBienEtreService(ObjectifBienEtreDao dao, ObjectifBienEtreValidator validator) {
        this.dao = dao;
        this.validator = validator;
    }

    public List<ObjectifBienEtre> findByUtilisateurId(Integer utilisateurId) {
        return dao.findByUtilisateurId(utilisateurId);
    }

    public Optional<ObjectifBienEtre> findById(Integer id) {
        return dao.findById(id);
    }

    public ObjectifBienEtre save(ObjectifBienEtre o) {
        validator.validate(o);
        return dao.save(o);
    }

    public boolean existsById(Integer id) {
        return dao.existsById(id);
    }

    public void deleteById(Integer id) {
        dao.deleteById(id);
    }
}
