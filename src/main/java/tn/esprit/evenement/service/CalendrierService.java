package tn.esprit.evenement.service;

import tn.esprit.evenement.entity.Evenement;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CalendrierService {
    private static final DateTimeFormatter ICS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public String genererFichierICS(Evenement e) {
        String start = e.getDate_debut().atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(ICS_FMT);
        String end = e.getDate_fin().atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(ICS_FMT);
        return "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "PRODID:-//BEKRI//EVENTS//FR\n" +
                "BEGIN:VEVENT\n" +
                "UID:event-" + e.getId() + "@bekri\n" +
                "DTSTAMP:" + start + "\n" +
                "DTSTART:" + start + "\n" +
                "DTEND:" + end + "\n" +
                "SUMMARY:" + safe(e.getTitre()) + "\n" +
                "DESCRIPTION:" + safe(e.getDescription()) + "\n" +
                "LOCATION:" + safe("") + "\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR\n";
    }

    public String genererLienGoogleCalendar(Evenement e) {
        String start = e.getDate_debut().atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(ICS_FMT);
        String end = e.getDate_fin().atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(ICS_FMT);
        return "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                "&text=" + enc(e.getTitre()) +
                "&details=" + enc(e.getDescription()) +
                "&location=" + enc("") +
                "&dates=" + start + "/" + end;
    }

    private String enc(String s) {
        return URLEncoder.encode(safe(s), StandardCharsets.UTF_8);
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ").trim();
    }
}
