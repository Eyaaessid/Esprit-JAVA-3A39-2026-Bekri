package tn.esprit.evenement.service;

import tn.esprit.evenement.entity.Evenement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FavoriService {
    private static final Map<Integer, Set<Integer>> store = new HashMap<>();

    private Set<Integer> favs(int userId) {
        return store.computeIfAbsent(userId, k -> new HashSet<>());
    }

    public void ajouterFavori(int userId, int evenementId) {
        favs(userId).add(evenementId);
    }

    public void retirerFavori(int userId, int evenementId) {
        favs(userId).remove(evenementId);
    }

    public boolean estEnFavori(int userId, int evenementId) {
        return favs(userId).contains(evenementId);
    }

    public List<Evenement> getMesFavoris(int userId) {
        Set<Integer> ids = favs(userId);
        if (ids.isEmpty()) return new ArrayList<>();
        EvenementService svc = new EvenementService();
        List<Evenement> all = svc.afficherAll();
        List<Evenement> result = new ArrayList<>();
        for (Evenement e : all) {
            if (ids.contains(e.getId())) result.add(e);
        }
        return result;
    }

    public int getNombreFavoris(int evenementId) {
        int count = 0;
        for (Set<Integer> s : store.values()) {
            if (s.contains(evenementId)) count++;
        }
        return count;
    }
}
