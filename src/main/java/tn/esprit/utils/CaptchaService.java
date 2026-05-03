package tn.esprit.utils;

import java.util.Random;

public class CaptchaService {

    public record CaptchaChallenge(String question, int answer) {}

    private static final Random RANDOM = new Random();

    public CaptchaChallenge generate() {
        int a = RANDOM.nextInt(9) + 1;
        int b = RANDOM.nextInt(9) + 1;

        String[] templates = {
                "Si votre score bien-être est %d et vous progressez de %d, quel est votre nouveau score ?",
                "Vous buvez %d verres d'eau et en ajoutez %d. Combien au total ?",
                "Score du jour : %d. Vous gagnez %d points. Nouveau score ?",
                "Vous dormez %d heures et ajoutez %d heures de repos. Total ?",
                "Objectif : %d séances. Vous en ajoutez %d. Total ?"
        };

        String question = String.format(templates[RANDOM.nextInt(templates.length)], a, b);
        return new CaptchaChallenge(question, a + b);
    }

    public boolean verify(int userAnswer, int correctAnswer) {
        return userAnswer == correctAnswer;
    }
}
