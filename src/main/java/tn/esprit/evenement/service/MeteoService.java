package tn.esprit.evenement.service;

import tn.esprit.evenement.entity.MeteoInfo;
import tn.esprit.utils.WeatherData;
import tn.esprit.utils.WeatherService;

import java.util.concurrent.CompletableFuture;

public class MeteoService {
    public MeteoInfo getMeteo(String ville) {
        WeatherData data = new WeatherService().fetchCurrentWeather();
        MeteoInfo info = new MeteoInfo();
        info.setVille(ville);
        if (data == null || !data.isFetchSuccess()) {
            info.setDescription("Météo indisponible");
            info.setConseil("Impossible de récupérer la météo pour le moment.");
            return info;
        }
        info.setTemperature(data.getTemperatureCelsius());
        info.setDescription(data.getDescription());
        info.setConseil("Prévoyez une tenue adaptée: " + data.getEmoji());
        return info;
    }

    public CompletableFuture<MeteoInfo> getMeteoAsync(String ville) {
        return CompletableFuture.supplyAsync(() -> getMeteo(ville));
    }
}
