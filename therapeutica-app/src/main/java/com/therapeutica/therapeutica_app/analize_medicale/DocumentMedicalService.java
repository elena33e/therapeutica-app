package com.therapeutica.therapeutica_app.analize_medicale;

import com.therapeutica.therapeutica_app.pacienti.Pacienti;
import com.therapeutica.therapeutica_app.pacienti.PacientiRepository;
import com.therapeutica.therapeutica_app.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMedicalService {

    private static final Set<String> EXTENSII_PERMISE =
            Set.of("pdf", "jpg", "jpeg", "png", "gif", "webp");

    private final PacientiRepository pacientiRepository;
    private final DocumentMedicalRepository documentMedicalRepository;

    private final Path directorDocumente =
            Path.of("upload-dir", "analize").toAbsolutePath().normalize();

    @Transactional(rollbackFor = Exception.class)
    public DocumentMedical incarcaDocumente(
            MultipartFile[] files,
            UUID userId) throws IOException {

        Pacienti pacient = pacientiRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new NotFoundException("Profilul pacientului nu există."));

        if (files == null) {
            throw new IllegalArgumentException("Selectează fișierele.");
        }

        List<MultipartFile> fisiere = Arrays.stream(files)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (fisiere.isEmpty()) {
            throw new IllegalArgumentException("Selectează cel puțin un fișier.");
        }

        // Validăm toate numele înainte de a scrie fișierele pe disc.
        List<String> numeFisiere = fisiere.stream()
                .map(file -> pregatesteNume(file.getOriginalFilename()))
                .toList();

        UUID documentId = UUID.randomUUID();
        Path folderDocument = directorDocumente.resolve(documentId.toString());

        Files.createDirectories(directorDocumente);
        Files.createDirectory(folderDocument);
        inregistreazaCuratareLaRollback(folderDocument);

        for (int index = 0; index < fisiere.size(); index++) {
            String numeStocat = String.format(
                    Locale.ROOT,
                    "%04d_%s",
                    index + 1,
                    numeFisiere.get(index)
            );

            Path destinatie = folderDocument.resolve(numeStocat);

            try (InputStream continut = fisiere.get(index).getInputStream()) {
                Files.copy(continut, destinatie);
            }
        }

        String numeAfisat = numeFisiere.get(0);
        if (fisiere.size() > 1) {
            numeAfisat += " (+ " + (fisiere.size() - 1) + " fișiere)";
        }

        DocumentMedical document = new DocumentMedical();
        document.setId(documentId);
        document.setPacient(pacient);
        document.setNumeFisier(numeAfisat);
        document.setCaleFisierStocare(folderDocument.toString());
        document.setStatus(DocumentMedical.StatusDocument.INCARCAT);

        return documentMedicalRepository.saveAndFlush(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentMedical> getDocumentePacient(UUID userId) {
        Pacienti pacient = pacientiRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new NotFoundException("Profilul pacientului nu există."));

        return getDocumenteDupaPacientId(pacient.getId());
    }

    @Transactional(readOnly = true)
    public List<DocumentMedical> getDocumenteDupaPacientId(UUID pacientId) {
        return documentMedicalRepository
                .findByPacient_IdAndStatusNotOrderByDataIncarcareDesc(
                        pacientId,
                        DocumentMedical.StatusDocument.STERS_DE_PACIENT
                );
    }

    @Transactional
    public void retrageDocument(UUID documentId) {
        DocumentMedical document = documentMedicalRepository.findById(documentId)
                .orElseThrow(() ->
                        new NotFoundException("Documentul nu a fost găsit."));

        document.setStatus(DocumentMedical.StatusDocument.STERS_DE_PACIENT);
        documentMedicalRepository.saveAndFlush(document);
    }

    private String pregatesteNume(String numeOriginal) {
        if (numeOriginal == null || numeOriginal.isBlank()) {
            throw new IllegalArgumentException("Un fișier are numele lipsă.");
        }

        String nume = numeOriginal.replace('\\', '/');
        nume = nume.substring(nume.lastIndexOf('/') + 1);

        int pozitieExtensie = nume.lastIndexOf('.');
        if (pozitieExtensie <= 0) {
            throw new IllegalArgumentException(
                    "Fișierele trebuie să fie PDF sau imagini."
            );
        }

        String extensie = nume.substring(pozitieExtensie + 1)
                .toLowerCase(Locale.ROOT);

        if (!EXTENSII_PERMISE.contains(extensie)) {
            throw new IllegalArgumentException(
                    "Formate acceptate: PDF, JPG, JPEG, PNG, GIF și WEBP."
            );
        }

        String baza = Normalizer.normalize(
                        nume.substring(0, pozitieExtensie),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        baza = baza.substring(0, Math.min(baza.length(), 120));

        return baza + "." + extensie;
    }

    private void inregistreazaCuratareLaRollback(Path folderDocument) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_ROLLED_BACK) {
                            return;
                        }

                        try {
                            List<Path> fisiere;
                            try (var continut = Files.list(folderDocument)) {
                                fisiere = continut.toList();
                            }

                            for (Path fisier : fisiere) {
                                Files.deleteIfExists(fisier);
                            }

                            Files.deleteIfExists(folderDocument);
                        } catch (IOException e) {
                            log.error(
                                    "Curățarea încărcării eșuate a întâmpinat o eroare: {}",
                                    folderDocument,
                                    e
                            );
                        }
                    }
                }
        );
    }
}