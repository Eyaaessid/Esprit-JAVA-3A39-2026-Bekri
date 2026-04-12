package tn.esprit.pijava.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "question_evaluation")
public class QuestionEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Le texte de la question est obligatoire.")
    @Size(min = 5, max = 255, message = "Le texte doit contenir entre 5 et 255 caractères.")
    @Column(nullable = false, length = 255)
    private String texte;

    @NotBlank(message = "La catégorie est obligatoire.")
    @Column(nullable = false, length = 50)
    private String category;

    @NotBlank(message = "Le type de réponse est obligatoire.")
    @Column(name = "type_reponse", nullable = false, length = 50)
    private String typeReponse;

    @NotBlank(message = "L'option 1 est obligatoire.")
    @Size(max = 100, message = "L'option 1 ne peut pas dépasser 100 caractères.")
    @Column(nullable = false, length = 255)
    private String option1;

    @NotBlank(message = "L'option 2 est obligatoire.")
    @Size(max = 100, message = "L'option 2 ne peut pas dépasser 100 caractères.")
    @Column(nullable = false, length = 255)
    private String option2;

    @NotBlank(message = "L'option 3 est obligatoire.")
    @Size(max = 100, message = "L'option 3 ne peut pas dépasser 100 caractères.")
    @Column(nullable = false, length = 255)
    private String option3;

    @Column(name = "min_value")
    private Integer minValue;

    @Column(name = "max_value")
    private Integer maxValue;

    // Getters/Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTypeReponse() { return typeReponse; }
    public void setTypeReponse(String typeReponse) { this.typeReponse = typeReponse; }

    public String getOption1() { return option1; }
    public void setOption1(String option1) { this.option1 = option1; }

    public String getOption2() { return option2; }
    public void setOption2(String option2) { this.option2 = option2; }

    public String getOption3() { return option3; }
    public void setOption3(String option3) { this.option3 = option3; }

    public Integer getMinValue() { return minValue; }
    public void setMinValue(Integer minValue) { this.minValue = minValue; }

    public Integer getMaxValue() { return maxValue; }
    public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }
}