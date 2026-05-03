package tn.esprit.plan.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import tn.esprit.plan.model.WeeklyPlan;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeeklyPlanPdfExporter {

    public record ExportContext(
            String userName,
            WeeklyPlanService.FormData formData,
            LocalDateTime generatedAt
    ) {}

    public void export(File file, WeeklyPlan plan, ExportContext ctx) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PdfWriter w = new PdfWriter(doc);

            w.h1("Bekri — Plan hebdomadaire");
            w.p("Date : " + LocalDate.now());
            if (ctx != null) {
                w.p("Utilisateur : " + safe(ctx.userName));
                if (ctx.generatedAt != null) {
                    w.p("Généré le : " + ctx.generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
            }
            w.spacer(8);

            // Stats
            w.h2("Statistiques");
            w.p("IMC : " + fmtDouble(plan.getImc(), 1));
            w.p("Calories journalières : " + (plan.getCaloriesJournalieres() == null ? "—" : plan.getCaloriesJournalieres() + " kcal"));
            if (plan.getHydratation() != null) {
                w.p("Hydratation : " + fmtDouble(plan.getHydratation().getLitresParJour(), 1) + " L/jour");
            }
            if (plan.getSommeil() != null) {
                w.p("Sommeil : " + safe(plan.getSommeil().getHeuresRecommandees()));
            }
            w.spacer(10);

            // Resume
            w.h2("Résumé");
            w.paragraph(plan.getResume());
            w.spacer(10);

            // Conseils
            w.h2("Conseils généraux");
            w.bullets(plan.getConseilsGeneraux());
            w.spacer(10);

            // Repas
            w.h2("Plan repas (7 jours)");
            for (String day : days()) {
                WeeklyPlan.RepasDay r = plan.getRepas() == null ? null : plan.getRepas().get(day);
                w.h3(cap(day));
                if (r == null) {
                    w.p("—");
                } else {
                    w.p("Petit-déjeuner : " + safe(r.getPetitDejeuner()));
                    w.p("Déjeuner : " + safe(r.getDejeuner()));
                    w.p("Dîner : " + safe(r.getDiner()));
                    w.p("Collation : " + safe(r.getCollation()));
                }
                w.spacer(4);
            }
            w.spacer(6);

            // Exercices
            w.h2("Plan exercices (7 jours)");
            for (String day : days()) {
                WeeklyPlan.ExerciceDay e = plan.getExercices() == null ? null : plan.getExercices().get(day);
                w.h3(cap(day));
                if (e == null) {
                    w.p("—");
                } else {
                    w.p("Type : " + safe(e.getType()));
                    w.p("Durée : " + safe(e.getDuree()));
                    w.p("Intensité : " + safe(e.getIntensite()));
                    w.p("Description : " + safe(e.getDescription()));
                }
                w.spacer(4);
            }
            w.spacer(6);

            // Hydratation tips
            w.h2("Hydratation — conseils");
            if (plan.getHydratation() != null) {
                w.bullets(plan.getHydratation().getConseils());
            } else {
                w.p("—");
            }
            w.spacer(8);

            // Sommeil tips
            w.h2("Sommeil — conseils");
            if (plan.getSommeil() != null) {
                w.bullets(plan.getSommeil().getConseils());
            } else {
                w.p("—");
            }

            w.finish();
            doc.save(file);
        }
    }

    private static List<String> days() {
        return List.of("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche");
    }

    private static String safe(String s) {
        return s == null ? "—" : s;
    }

    private static String cap(String s) {
        if (s == null || s.isBlank()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String fmtDouble(Double d, int decimals) {
        if (d == null) return "—";
        double m = Math.pow(10, decimals);
        return String.valueOf(Math.round(d * m) / m);
    }

    private static final class PdfWriter {
        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream cs;
        private float x = 50;
        private float y;
        private float leading = 14f;
        private final float top = 760;
        private final float bottom = 55;

        PdfWriter(PDDocument doc) throws Exception {
            this.doc = doc;
            newPage();
        }

        void newPage() throws Exception {
            if (cs != null) {
                cs.close();
            }
            page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = top;
        }

        void ensureSpace(float needed) throws Exception {
            if (y - needed < bottom) {
                newPage();
            }
        }

        void h1(String text) throws Exception {
            ensureSpace(26);
            writeLine(text, PDType1Font.HELVETICA_BOLD, 18, 22);
            spacer(4);
        }

        void h2(String text) throws Exception {
            ensureSpace(22);
            writeLine(text, PDType1Font.HELVETICA_BOLD, 13, 18);
        }

        void h3(String text) throws Exception {
            ensureSpace(18);
            writeLine(text, PDType1Font.HELVETICA_BOLD, 11.5f, 16);
        }

        void p(String text) throws Exception {
            ensureSpace(leading);
            writeWrapped(text, PDType1Font.HELVETICA, 10.5f, leading);
        }

        void paragraph(String text) throws Exception {
            writeWrapped(text, PDType1Font.HELVETICA, 10.5f, leading);
        }

        void bullets(List<String> items) throws Exception {
            if (items == null || items.isEmpty()) {
                p("—");
                return;
            }
            for (String s : items) {
                writeWrapped("- " + (s == null ? "" : s), PDType1Font.HELVETICA, 10.5f, leading);
            }
        }

        void spacer(float px) {
            y -= px;
        }

        void finish() throws Exception {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        private void writeLine(String text, PDType1Font font, float size, float lineHeight) throws Exception {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y);
            cs.showText(sanitize(text));
            cs.endText();
            y -= lineHeight;
        }

        private void writeWrapped(String text, PDType1Font font, float size, float lineHeight) throws Exception {
            if (text == null) text = "";
            List<String> lines = wrap(text, font, size, 500);
            for (String line : lines) {
                ensureSpace(lineHeight);
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(x, y);
                cs.showText(sanitize(line));
                cs.endText();
                y -= lineHeight;
            }
        }

        private static List<String> wrap(String text, PDType1Font font, float size, float maxWidth) throws Exception {
            List<String> out = new ArrayList<>();
            String[] words = text.replace("\r", "").split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (w.isBlank()) continue;
                String candidate = line.isEmpty() ? w : line + " " + w;
                float width = font.getStringWidth(candidate) / 1000f * size;
                if (width > maxWidth && !line.isEmpty()) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(w);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            if (!line.isEmpty()) out.add(line.toString());
            if (out.isEmpty()) out.add("");
            return out;
        }

        private static String sanitize(String s) {
            if (s == null) return "";
            // PDFBox Type1 fonts are WinAnsi; best-effort replace unsupported chars.
            return s
                    .replace("’", "'")
                    .replace("“", "\"")
                    .replace("”", "\"")
                    .replace("—", "-");
        }
    }
}
