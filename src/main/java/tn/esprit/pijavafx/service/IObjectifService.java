package tn.esprit.pijavafx.service;

import tn.esprit.pijavafx.model.ObjectifBienEtreDto;
import java.util.List;

public interface IObjectifService {
    List<ObjectifBienEtreDto> getAll() throws Exception;
    ObjectifBienEtreDto create(ObjectifBienEtreDto dto) throws Exception;
    ObjectifBienEtreDto update(int id, ObjectifBienEtreDto dto) throws Exception;
    void delete(int id) throws Exception;
}