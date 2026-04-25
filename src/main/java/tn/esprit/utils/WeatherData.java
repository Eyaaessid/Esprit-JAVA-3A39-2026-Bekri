package tn.esprit.utils;

public class WeatherData {
    private final String city;
    private final String description;
    private final double temperatureCelsius;
    private final int humidity;
    private final double windSpeed;
    private final boolean fetchSuccess;

    public WeatherData(String city, String description, double temperatureCelsius, int humidity, double windSpeed, boolean fetchSuccess) {
        this.city = city == null ? "Unknown" : city;
        this.description = description == null ? "Inconnu" : description;
        this.temperatureCelsius = temperatureCelsius;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.fetchSuccess = fetchSuccess;
    }

    public static WeatherData unavailable() {
        return new WeatherData("Tunis", "indisponible", 0.0, 0, 0.0, false);
    }

    public String getCity() {
        return city;
    }

    public String getDescription() {
        return description;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public boolean isFetchSuccess() {
        return fetchSuccess;
    }

    public String getEmoji() {
        String d = description.toLowerCase();
        if (d.contains("thunder")) return "⛈️";
        if (d.contains("storm")) return "⛈️";
        if (d.contains("rain") || d.contains("drizzle")) return "🌧️";
        if (d.contains("snow")) return "❄️";
        if (d.contains("cloud")) return "☁️";
        if (d.contains("mist") || d.contains("fog") || d.contains("haze")) return "🌫️";
        if (d.contains("clear") || d.contains("sun")) return "☀️";
        return "🌤️";
    }

    public String getTemperatureCategory() {
        if (temperatureCelsius < 10) return "cold";
        if (temperatureCelsius < 25) return "mild";
        if (temperatureCelsius < 33) return "warm";
        return "hot";
    }

    public String getSummary() {
        return String.format("%s %.0f°C, humidite %d%%, vent %.1f m/s",
                getEmoji(), temperatureCelsius, humidity, windSpeed);
    }
}
