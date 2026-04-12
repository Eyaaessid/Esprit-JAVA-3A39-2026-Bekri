package tn.esprit.pijavafx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionEvaluationDto {

    private Integer id;
    private String texte;
    private String category;
    private String typeReponse;
    private String option1;
    private String option2;
    private String option3;

    public QuestionEvaluationDto() {}

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
}
