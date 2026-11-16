package com.therapeutica.therapeutica_app.analize_medicale.ui;

import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedical;
import com.therapeutica.therapeutica_app.analize_medicale.DocumentMedicalRepository;
import com.therapeutica.therapeutica_app.audit.AuditLog;
import com.therapeutica.therapeutica_app.audit.AuditService;
import com.therapeutica.therapeutica_app.security.AccesPacientService;
import com.therapeutica.therapeutica_app.util.AccesInterzisException;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/documente")
@RequiredArgsConstructor
public class DocumentSursaController {

    private static final Set<String> TIPURI_PREVIZUALIZABILE = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final DocumentMedicalRepository documentMedicalRepository;
    private final AccesPacientService accesPacientService;
    private final AuditService auditService;

    @GetMapping("/{documentId}/sursa")
    public String afiseazaSursa(
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "0") int index,
            HttpSession session,
            Authentication authentication,
            Model model) throws IOException {

        DocumentMedical document =
                incarcaDocumentAutorizat(documentId, session, authentication);

        List<Path> fisiere = listeazaFisiere(document);
        verificaIndex(index, fisiere.size());

        Path fisierSelectat = fisiere.get(index);
        String tip = detecteazaTip(fisierSelectat);

        model.addAttribute("document", document);
        model.addAttribute(
                "numeFisiere",
                fisiere.stream()
                        .map(path -> path.getFileName().toString())
                        .toList()
        );
        model.addAttribute("indexSelectat", index);
        model.addAttribute(
                "previzualizabil",
                TIPURI_PREVIZUALIZABILE.contains(tip)
        );

        return "documente/sursa";
    }

    @GetMapping("/{documentId}/fisier")
    public ResponseEntity<Resource> deschideFisier(
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "0") int index,
            @RequestParam(defaultValue = "false") boolean descarcare,
            HttpSession session,
            Authentication authentication) throws IOException {

        DocumentMedical document =
                incarcaDocumentAutorizat(documentId, session, authentication);

        List<Path> fisiere = listeazaFisiere(document);
        verificaIndex(index, fisiere.size());

        Path fisier = fisiere.get(index);

        if (!Files.isReadable(fisier)) {
            throw new NotFoundException("Fișierul nu poate fi citit.");
        }

        String tip = detecteazaTip(fisier);
        String dispozitie = !descarcare && TIPURI_PREVIZUALIZABILE.contains(tip)
                ? "inline"
                : "attachment";

        ContentDisposition contentDisposition = ContentDisposition
                .builder(dispozitie)
                .filename(
                        fisier.getFileName().toString(),
                        StandardCharsets.UTF_8
                )
                .build();

        auditService.inregistreaza(
                auditService.utilizatorCurent(session),
                AuditLog.ActiuneAudit.VIZUALIZARE,
                AuditLog.TipResursa.DOCUMENT_MEDICAL,
                documentId,
                document.getPacient().getId()
        );

        Resource resource = new UrlResource(fisier.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tip))
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .body(resource);
    }

    private DocumentMedical incarcaDocumentAutorizat(
            UUID documentId,
            HttpSession session,
            Authentication authentication) {

        if (authentication == null) {
            throw new AccesInterzisException("Autentificare necesară.");
        }

        DocumentMedical document = documentMedicalRepository
                .findByIdWithPacientSiUser(documentId)
                .orElseThrow(() ->
                        new NotFoundException("Documentul nu a fost găsit."));

        boolean esteMedic = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_MEDIC".equals(authority.getAuthority()));

        if (esteMedic) {
            accesPacientService.verificaAccesMedic(
                    session,
                    document.getPacient().getId()
            );
        } else {
            accesPacientService.verificaAccesPropriu(
                    session,
                    document.getPacient().getId()
            );
        }

        if (document.getStatus()
                == DocumentMedical.StatusDocument.STERS_DE_PACIENT) {
            throw new NotFoundException("Documentul a fost retras din dosar.");
        }

        return document;


    }

    private List<Path> listeazaFisiere(DocumentMedical document)
            throws IOException {

        Path locatie = Path.of(document.getCaleFisierStocare());

        // Suportă și documentele vechi care indică direct un fișier.
        if (Files.isRegularFile(locatie, LinkOption.NOFOLLOW_LINKS)) {
            return List.of(locatie);
        }

        if (!Files.isDirectory(locatie, LinkOption.NOFOLLOW_LINKS)) {
            throw new NotFoundException(
                    "Fișierele documentului nu au fost găsite."
            );
        }

        try (var continut = Files.list(locatie)) {
            return continut
                    .filter(path -> Files.isRegularFile(
                            path,
                            LinkOption.NOFOLLOW_LINKS
                    ))
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()
                    ))
                    .toList();
        }
    }

    private void verificaIndex(int index, int numarFisiere) {
        if (index < 0 || index >= numarFisiere) {
            throw new NotFoundException("Fișierul solicitat nu există.");
        }
    }

    private String detecteazaTip(Path fisier) throws IOException {
        String tip = Files.probeContentType(fisier);

        return tip != null
                ? tip
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}