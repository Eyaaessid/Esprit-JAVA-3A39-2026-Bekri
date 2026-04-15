package tn.esprit.pijava.service;

import tn.esprit.pijava.entity.ObjectifBienEtre;
import tn.esprit.pijava.repository.ObjectifBienEtreDao;
import tn.esprit.pijava.validation.ObjectifBienEtreValidator;

import java.util.List;
import java.util.Optional;

public class ObjectifBienEtreService {
    private final ObjectifBienEtreDao dao;
    private final ObjectifBienEtreValidator validator;

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
