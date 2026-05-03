package tn.esprit.utils;

import java.util.ArrayList;
import java.util.List;

public final class WeatherInsightHelper {
    private WeatherInsightHelper() {
    }

    public static List<String> generateTips(WeatherData weather, double moodScore, double stressScore, double sleepScore) {
        List<String> tips = new ArrayList<>();
        if (weather == null || !weather.isFetchSuccess()) {
            tips.add("Buvez de l'eau regulierement et gardez une routine douce aujourd'hui.");
            return tips;
        }

        String category = weather.getTemperatureCategory();
        if ("hot".equals(category) || "warm".equals(category)) {
            tips.add("Hydratez-vous davantage et evitez les efforts intenses aux heures chaudes.");
        } else if ("cold".equals(category)) {
            tips.add("Couvrez-vous bien et privilegiez un echauffement progressif avant l'activite.");
        } else {
            tips.add("La meteo est moderee: profitez d'une marche de 20-30 minutes.");
        }

        if (weather.getHumidity() >= 75) {
            tips.add("Humidite elevee: aerer la piece aide a reduire la sensation d'inconfort.");
        }
        if (weather.getWindSpeed() >= 8) {
            tips.add("Vent soutenu: adaptez votre tenue et protegez les voies respiratoires sensibles.");
        }

        if (moodScore >= 0 && moodScore < 45) {
            tips.add("Humeur basse: faites une pause courte et notez une action positive realisable.");
        }
        if (stressScore >= 0 && stressScore > 70) {
            tips.add("Stress eleve: prenez 3 minutes de respiration lente (inspire 4s, expire 6s).");
        }
        if (sleepScore >= 0 && sleepScore < 50) {
            tips.add("Sommeil faible: priorisez un coucher plus regulier ce soir.");
        }

        return tips;
    }

    public static String getWeatherContextLine(WeatherData weather) {
        if (weather == null || !weather.isFetchSuccess()) {
            return "Meteo indisponible pour le moment.";
        }
        return weather.getSummary();
    }

    public static String getWeatherStyleClass(WeatherData weather) {
        if (weather == null || !weather.isFetchSuccess()) return "weather-default";
        String t = weather.getTemperatureCategory();
        if ("hot".equals(t)) return "weather-hot";
        if ("cold".equals(t)) return "weather-cold";
        if ("warm".equals(t)) return "weather-warm";
        return "weather-mild";
    }
}
