package com.therapeutica.therapeutica_app.chestionare;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.therapeutica.therapeutica_app.raport_chestionar.RezultatScoring;
import com.therapeutica.therapeutica_app.raport_chestionar.ScoringService;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionare;
import com.therapeutica.therapeutica_app.raspunsuri_chestionare.RaspunsuriChestionareRepository;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebari;
import com.therapeutica.therapeutica_app.raspunsuri_intrebari.RaspunsuriIntrebariService;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RaportGeneratorService {

    private static final DateTimeFormatter FORMAT_DATA =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RaspunsuriChestionareRepository raspunsuriChestionareRepository;
    private final RaspunsuriIntrebariService raspunsuriIntrebariService;
    private final ScoringService scoringService;

    @Transactional(readOnly = true)
    public byte[] genereazaPdf(UUID raspunsChestionarId) throws IOException {

        RaspunsuriChestionare chestionar = raspunsuriChestionareRepository
                .findByIdForCompletare(raspunsChestionarId)
                .orElseThrow(() ->
                        new NotFoundException("Chestionarul nu a fost găsit."));

        boolean finalizat =
                chestionar.getStatus()
                        == RaspunsuriChestionare.StatusRaspuns.COMPLETAT
                        || chestionar.getStatus()
                        == RaspunsuriChestionare.StatusRaspuns.REVIZUIT;

        if (!finalizat) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Raportul este disponibil după completarea chestionarului."
            );
        }

        if (chestionar.getPacient() == null
                || chestionar.getPacient().getUser() == null
                || chestionar.getChestionar() == null) {
            throw new NotFoundException(
                    "Datele necesare raportului sunt incomplete."
            );
        }

        RezultatScoring rezultat = scoringService
                .getScoruriCalculate(raspunsChestionarId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Deschide raportul în aplicație pentru calcularea scorurilor."
                ));

        if (Boolean.TRUE.equals(rezultat.getNecesitaRecalculare())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Redeschide raportul în aplicație pentru actualizarea scorurilor."
            );
        }

        List<RaspunsuriIntrebari> raspunsuri =
                raspunsuriIntrebariService
                        .getRaspunsuriDetaliate(raspunsChestionarId);

        Font normal = creeazaFont("LiberationSans-Regular.ttf", 10);
        Font bold = creeazaFont("LiberationSans-Bold.ttf", 10);
        Font titlu = creeazaFont("LiberationSans-Bold.ttf", 18);
        Font subtitlu = creeazaFont("LiberationSans-Bold.ttf", 12);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 40, 40, 40, 45);

        try {
            PdfWriter.getInstance(pdf, output);
            pdf.addTitle("Raport chestionar");
            pdf.addCreator("Therapeutica");
            pdf.setFooter(new HeaderFooter(new Phrase("Pagina ", normal), true));
            pdf.open();

            adaugaParagraf(pdf, "Raport chestionar", titlu);
            adaugaParagraf(
                    pdf,
                    chestionar.getChestionar().getNume(),
                    subtitlu
            );

            var pacientUser = chestionar.getPacient().getUser();

            adaugaParagraf(
                    pdf,
                    "Pacient: " + pacientUser.getNume()
                            + " " + pacientUser.getPrenume(),
                    normal
            );

            adaugaParagraf(
                    pdf,
                    "Completat la: " + formateazaData(chestionar.getCompletatLa()),
                    normal
            );

            String status = chestionar.getStatus()
                    == RaspunsuriChestionare.StatusRaspuns.REVIZUIT
                    ? "Revizuit de medic"
                    : "Completat";

            adaugaParagraf(pdf, "Status: " + status, normal);
            adaugaParagraf(pdf, "Identificator: " + chestionar.getId(), normal);

            adaugaParagraf(
                    pdf,
                    "Scor total: " + text(rezultat.getScorTotal()),
                    subtitlu
            );

            adaugaParagraf(
                    pdf,
                    "Interpretare: " + text(rezultat.getInterpretareTotal()),
                    normal
            );

            adaugaParagraf(
                    pdf,
                    "Scor calculat la: " + formateazaData(rezultat.getCalculatLa()),
                    normal
            );

            adaugaParagraf(pdf, "Scoruri pe categorii", subtitlu);

            PdfPTable scoruri = creeazaTabel(
                    new float[]{3, 1, 3},
                    bold,
                    "Categorie", "Scor", "Interpretare"
            );

            var scoruriCategorii = rezultat.getScoruriCategorii();

            if (scoruriCategorii != null) {
                for (var entry : new TreeMap<>(scoruriCategorii).entrySet()) {
                    scoruri.addCell(celula(entry.getKey(), normal));
                    scoruri.addCell(celula(text(entry.getValue()), normal));
                    scoruri.addCell(celula(
                            interpretareCategorie(rezultat, entry.getKey()),
                            normal
                    ));
                }
            }

            pdf.add(scoruri);

            adaugaParagraf(pdf, "Răspunsurile pacientului", subtitlu);

            PdfPTable detalii = creeazaTabel(
                    new float[]{2, 4, 3, 1},
                    bold,
                    "Categorie", "Întrebare", "Răspuns", "Scor"
            );

            for (RaspunsuriIntrebari raspuns : raspunsuri) {
                String categorie = raspuns.getCategorie() != null
                        ? raspuns.getCategorie().getNume()
                        : "Fără categorie";

                detalii.addCell(celula(categorie, normal));
                detalii.addCell(celula(
                        raspuns.getIntrebare().getTextIntrebare(),
                        normal
                ));
                detalii.addCell(celula(formateazaRaspuns(raspuns), normal));
                detalii.addCell(celula(text(raspuns.getScor()), normal));
            }

            pdf.add(detalii);
        } finally {
            pdf.close();
        }

        return output.toByteArray();
    }

    private Font creeazaFont(String fisier, int dimensiune) throws IOException {
        BaseFont baseFont = BaseFont.createFont(
                "liberation/" + fisier,
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
        );

        return new Font(baseFont, dimensiune);
    }

    private PdfPTable creeazaTabel(
            float[] latimi,
            Font fontAntet,
            String... antete) {

        PdfPTable tabel = new PdfPTable(latimi);
        tabel.setWidthPercentage(100);
        tabel.setSpacingBefore(6);
        tabel.setSpacingAfter(14);
        tabel.setSplitLate(false);

        for (String antet : antete) {
            tabel.addCell(celula(antet, fontAntet));
        }

        tabel.setHeaderRows(1);
        return tabel;
    }

    private PdfPCell celula(String continut, Font font) {
        PdfPCell celula = new PdfPCell(new Phrase(text(continut), font));
        celula.setPadding(6);
        return celula;
    }

    private void adaugaParagraf(Document pdf, String continut, Font font) {
        Paragraph paragraf = new Paragraph(text(continut), font);
        paragraf.setSpacingAfter(8);
        pdf.add(paragraf);
    }

    private String interpretareCategorie(
            RezultatScoring rezultat,
            String categorie) {

        Map<String, String> interpretari = rezultat.getInterpretariCategorii();
        return interpretari == null ? "—" : text(interpretari.get(categorie));
    }

    private String formateazaRaspuns(RaspunsuriIntrebari raspuns) {
        if (raspuns.getRaspunsText() != null) {
            return raspuns.getRaspunsText();
        }
        if (raspuns.getRaspunsNumeric() != null) {
            return raspuns.getRaspunsNumeric().toPlainString();
        }
        if (raspuns.getRaspunsData() != null) {
            return formateazaData(raspuns.getRaspunsData());
        }
        if (raspuns.getRaspunsJson() != null) {
            return raspuns.getRaspunsJson();
        }
        return text(raspuns.getScor());
    }

    private String formateazaData(LocalDateTime data) {
        return data == null ? "—" : data.format(FORMAT_DATA);
    }

    private String text(Object valoare) {
        return valoare == null ? "—" : valoare.toString();
    }
}