-- Oprim migrarea dacă există statusuri care necesită analiză separată.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.documente_medicale
        WHERE status IS NULL
           OR status NOT IN (
               'INCARCAT',
               'VALIDAT',
               'PROCESAT',
               'STANDARDIZAT',
               'INTERPRETAT',
               'FHIR_GENERAT',
               'STERS_DE_PACIENT'
           )
    ) THEN
        RAISE EXCEPTION
            'Există documente cu status NULL, EROARE sau necunoscut. Migrarea necesită verificarea lor.';
    END IF;
END
$$;

UPDATE public.documente_medicale
SET status = 'INCARCAT'
WHERE status IN (
    'VALIDAT',
    'PROCESAT',
    'STANDARDIZAT',
    'INTERPRETAT',
    'FHIR_GENERAT'
);

ALTER TABLE public.documente_medicale
    DROP CONSTRAINT documente_medicale_status_check;

ALTER TABLE public.documente_medicale
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'INCARCAT',
    ADD CONSTRAINT documente_medicale_status_check
        CHECK (status IN ('INCARCAT', 'STERS_DE_PACIENT'));

ALTER TABLE public.documente_medicale
    DROP COLUMN date_brute,
    DROP COLUMN date_validate,
    DROP COLUMN date_standardizate_loinc,
    DROP COLUMN date_interpretate_fhir,
    DROP COLUMN candidati_loinc,
    DROP COLUMN date_fhir_standard;

ALTER TABLE public.intrebari
    DROP COLUMN hpo_code,
    DROP COLUMN hpo_term,
    DROP COLUMN hpo_trigger_code,
    DROP COLUMN hpo_trigger_term;

DROP TABLE public.istoric_diagnoze;