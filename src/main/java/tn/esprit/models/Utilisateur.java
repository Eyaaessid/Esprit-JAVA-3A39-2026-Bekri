package tn.esprit.models;

/**
 * Représente une ligne de la table {@code utilisateur}.
 * Les colonnes {@code nom} et {@code prénom} sont utilisées pour l’affichage dans les listes ;
 * adaptez les getters si votre schéma utilise d’autres noms (ex. {@code email} seul).
 */
public class Utilisateur {

    private int id;
    private String nom;
    private String prenom;

    public Utilisateur() {
    }

    public Utilisateur(int id, String nom, String prenom) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    /** Libellé court pour le ComboBox : ex. « Aziz » ou « Aziz Ben ». */
    public String getNomAffichage() {
        String p = prenom != null ? prenom.trim() : "";
        String n = nom != null ? nom.trim() : "";
        if (!p.isEmpty() && !n.isEmpty()) {
            return p + " " + n;
        }
        if (!n.isEmpty()) {
            return n;
        }
        if (!p.isEmpty()) {
            return p;
        }
        return "Utilisateur";
    }

    /** Format demandé : « id - Nom ». */
    public String getLibelleListe() {
        return id + " - " + getNomAffichage();
    }

    @Override
    public String toString() {
        return getLibelleListe();
    }
}
