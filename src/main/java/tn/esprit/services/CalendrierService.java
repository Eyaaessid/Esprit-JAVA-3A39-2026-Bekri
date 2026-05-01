package tn.esprit.services;

import tn.esprit.models.Evenement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CalendrierService {

    private static final DateTimeFormatter ICS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public File genererFichierICS(Evenement evenement) {
        if (evenement == null || evenement.getDate_debut() == null) {
            return null;
        }
        try {
            String fileName = "evenement_" + evenement.getId() + ".ics";
            File file = new File(System.getProperty("java.io.tmpdir"), fileName);

            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                String icsContent = genererContenuICS(evenement);
                writer.write(icsContent);
            }

            System.out.println("✓ Fichier ICS généré: " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            System.err.println("✗ Erreur lors de la génération du fichier ICS: " + e.getMessage());
            return null;
        }
    }

    private String genererContenuICS(Evenement evenement) {
        StringBuilder ics = new StringBuilder();

        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Bekri//Gestion Evenements//FR\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");

        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:").append(UUID.randomUUID()).append("@bekri.tn\r\n");
        ics.append("DTSTAMP:").append(toUtc(LocalDateTime.now())).append("\r\n");
        ics.append("DTSTART:").append(toUtc(evenement.getDate_debut())).append("\r\n");
        if (evenement.getDate_fin() != null) {
            ics.append("DTEND:").append(toUtc(evenement.getDate_fin())).append("\r\n");
        }
        ics.append("SUMMARY:").append(escaperTexte(evenement.getTitre())).append("\r\n");
        ics.append("DESCRIPTION:")
                .append(escaperTexte((evenement.getDescription() == null ? "" : evenement.getDescription())
                        + " | Type: " + (evenement.getType() == null ? "" : evenement.getType())))
                .append("\r\n");
        ics.append("LOCATION:").append(escaperTexte(evenement.getLieu())).append("\r\n");
        ics.append("CATEGORIES:").append(escaperTexte(evenement.getType())).append("\r\n");
        ics.append("STATUS:").append(convertirStatut(evenement.getStatut())).append("\r\n");

        ics.append("BEGIN:VALARM\r\n");
        ics.append("TRIGGER:-PT1H\r\n");
        ics.append("ACTION:DISPLAY\r\n");
        ics.append("DESCRIPTION:Rappel: ").append(escaperTexte(evenement.getTitre())).append("\r\n");
        ics.append("END:VALARM\r\n");

        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");

        return ics.toString();
    }

    private String toUtc(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(ICS_FORMATTER);
    }

    private String escaperTexte(String texte) {
        if (texte == null) return "";
        return texte.replace("\n", "\\n")
                   .replace(",", "\\,")
                   .replace(";", "\\;");
    }

    private String convertirStatut(String statut) {
        if (statut == null) return "TENTATIVE";

        switch (statut.toLowerCase()) {
            case "ouvert":
            case "planifié":
            case "planifie":
                return "CONFIRMED";
            case "annulé":
            case "annule":
                return "CANCELLED";
            default:
                return "TENTATIVE";
        }
    }

    public String genererLienGoogleCalendar(Evenement evenement) {
        try {
            String titre = java.net.URLEncoder.encode(evenement.getTitre(), "UTF-8");
            String description = java.net.URLEncoder.encode(
                    (evenement.getDescription() == null ? "" : evenement.getDescription())
                            + " | Type: " + (evenement.getType() == null ? "" : evenement.getType()),
                    "UTF-8"
            );
            String lieu = java.net.URLEncoder.encode(evenement.getLieu(), "UTF-8");

            DateTimeFormatter googleFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
            String dateDebut = toUtc(evenement.getDate_debut());
            String dateFin = evenement.getDate_fin() != null ? toUtc(evenement.getDate_fin()) : dateDebut;

            return "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                   "&text=" + titre +
                   "&dates=" + dateDebut + "/" + dateFin +
                   "&details=" + description +
                   "&location=" + lieu;
        } catch (Exception e) {
            System.err.println("Erreur génération lien Google Calendar: " + e.getMessage());
            return null;
        }
    }
}
