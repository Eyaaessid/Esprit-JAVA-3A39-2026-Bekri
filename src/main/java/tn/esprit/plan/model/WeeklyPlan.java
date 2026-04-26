package tn.esprit.plan.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyPlan {
    private String resume;
    private Double imc;

    @JsonProperty("calories_journalieres")
    private Integer caloriesJournalieres;

    @JsonProperty("conseils_generaux")
    private List<String> conseilsGeneraux;

    /**
     * Keys: lundi..dimanche
     */
    private Map<String, RepasDay> repas = new LinkedHashMap<>();

    /**
     * Keys: lundi..dimanche
     */
    private Map<String, ExerciceDay> exercices = new LinkedHashMap<>();

    private Hydratation hydratation;
    private Sommeil sommeil;

    public WeeklyPlan() {}

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public Double getImc() {
        return imc;
    }

    public void setImc(Double imc) {
        this.imc = imc;
    }

    public Integer getCaloriesJournalieres() {
        return caloriesJournalieres;
    }

    public void setCaloriesJournalieres(Integer caloriesJournalieres) {
        this.caloriesJournalieres = caloriesJournalieres;
    }

    public List<String> getConseilsGeneraux() {
        return conseilsGeneraux;
    }

    public void setConseilsGeneraux(List<String> conseilsGeneraux) {
        this.conseilsGeneraux = conseilsGeneraux;
    }

    public Map<String, RepasDay> getRepas() {
        return repas;
    }

    public void setRepas(Map<String, RepasDay> repas) {
        this.repas = repas;
    }

    public Map<String, ExerciceDay> getExercices() {
        return exercices;
    }

    public void setExercices(Map<String, ExerciceDay> exercices) {
        this.exercices = exercices;
    }

    public Hydratation getHydratation() {
        return hydratation;
    }

    public void setHydratation(Hydratation hydratation) {
        this.hydratation = hydratation;
    }

    public Sommeil getSommeil() {
        return sommeil;
    }

    public void setSommeil(Sommeil sommeil) {
        this.sommeil = sommeil;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hydratation {
        @JsonProperty("litres_par_jour")
        private Double litresParJour;

        private List<String> conseils;

        public Hydratation() {}

        public Double getLitresParJour() {
            return litresParJour;
        }

        public void setLitresParJour(Double litresParJour) {
            this.litresParJour = litresParJour;
        }

        public List<String> getConseils() {
            return conseils;
        }

        public void setConseils(List<String> conseils) {
            this.conseils = conseils;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sommeil {
        @JsonProperty("heures_recommandees")
        private String heuresRecommandees;

        private List<String> conseils;

        public Sommeil() {}

        public String getHeuresRecommandees() {
            return heuresRecommandees;
        }

        public void setHeuresRecommandees(String heuresRecommandees) {
            this.heuresRecommandees = heuresRecommandees;
        }

        public List<String> getConseils() {
            return conseils;
        }

        public void setConseils(List<String> conseils) {
            this.conseils = conseils;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepasDay {
        @JsonProperty("petit_dejeuner")
        private String petitDejeuner;
        private String dejeuner;
        private String diner;
        private String collation;

        public RepasDay() {}

        public String getPetitDejeuner() {
            return petitDejeuner;
        }

        public void setPetitDejeuner(String petitDejeuner) {
            this.petitDejeuner = petitDejeuner;
        }

        public String getDejeuner() {
            return dejeuner;
        }

        public void setDejeuner(String dejeuner) {
            this.dejeuner = dejeuner;
        }

        public String getDiner() {
            return diner;
        }

        public void setDiner(String diner) {
            this.diner = diner;
        }

        public String getCollation() {
            return collation;
        }

        public void setCollation(String collation) {
            this.collation = collation;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExerciceDay {
        private String type;
        private String duree;
        private String intensite;
        private String description;

        public ExerciceDay() {}

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDuree() {
            return duree;
        }

        public void setDuree(String duree) {
            this.duree = duree;
        }

        public String getIntensite() {
            return intensite;
        }

        public void setIntensite(String intensite) {
            this.intensite = intensite;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
