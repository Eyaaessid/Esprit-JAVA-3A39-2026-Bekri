package tn.esprit.objectif.ui;

import tn.esprit.objectif.entity.ObjectifBienEtre;
import tn.esprit.objectif.entity.QuestionEvaluation;
import tn.esprit.objectif.model.ObjectifBienEtreDto;
import tn.esprit.objectif.model.QuestionEvaluationDto;

final class ObjectifEntityMapper {
    private ObjectifEntityMapper() {}

    static ObjectifBienEtreDto toDto(ObjectifBienEtre o) {
        if (o == null) return null;
        ObjectifBienEtreDto d = new ObjectifBienEtreDto();
        d.setId(o.getId());
        d.setTitre(o.getTitre());
        d.setDescription(o.getDescription());
        d.setType(o.getType());
        d.setValeurCible(o.getValeurCible());
        d.setValeurActuelle(o.getValeurActuelle());
        d.setDateDebut(o.getDateDebut());
        d.setDateFin(o.getDateFin());
        d.setStatut(o.getStatut());
        d.setCreatedAt(o.getCreatedAt());
        d.setUpdatedAt(o.getUpdatedAt());
        d.setUtilisateurId(o.getUtilisateurId());
        d.setSlug(o.getSlug());
        return d;
    }

    static ObjectifBienEtre toEntity(ObjectifBienEtreDto d) {
        if (d == null) return null;
        ObjectifBienEtre o = new ObjectifBienEtre();
        o.setId(d.getId());
        o.setTitre(d.getTitre());
        o.setDescription(d.getDescription());
        o.setType(d.getType());
        o.setValeurCible(d.getValeurCible());
        o.setValeurActuelle(d.getValeurActuelle());
        o.setDateDebut(d.getDateDebut());
        o.setDateFin(d.getDateFin());
        o.setStatut(d.getStatut());
        o.setCreatedAt(d.getCreatedAt());
        o.setUpdatedAt(d.getUpdatedAt());
        o.setUtilisateurId(d.getUtilisateurId());
        o.setSlug(d.getSlug());
        return o;
    }

    static QuestionEvaluationDto toDto(QuestionEvaluation q) {
        if (q == null) return null;
        QuestionEvaluationDto d = new QuestionEvaluationDto();
        d.setId(q.getId());
        d.setTexte(q.getTexte());
        d.setCategory(q.getCategory());
        d.setTypeReponse(q.getTypeReponse());
        d.setOption1(q.getOption1());
        d.setOption2(q.getOption2());
        d.setOption3(q.getOption3());
        return d;
    }

    static QuestionEvaluation toEntity(QuestionEvaluationDto d) {
        if (d == null) return null;
        QuestionEvaluation q = new QuestionEvaluation();
        q.setId(d.getId());
        q.setTexte(d.getTexte());
        q.setCategory(d.getCategory());
        q.setTypeReponse(d.getTypeReponse());
        q.setOption1(d.getOption1());
        q.setOption2(d.getOption2());
        q.setOption3(d.getOption3());
        return q;
    }
}
