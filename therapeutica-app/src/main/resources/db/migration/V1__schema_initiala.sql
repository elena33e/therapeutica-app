--
-- PostgreSQL database dump
--


-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: role_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.role_type AS ENUM (
    'ADMIN',
    'MEDIC',
    'PACIENT'
);


--
-- Name: status_cod; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.status_cod AS ENUM (
    'NEUTILIZAT',
    'UTILIZAT'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    id uuid NOT NULL,
    actiune character varying(255) NOT NULL,
    adresa_ip character varying(45),
    data_ora timestamp(6) without time zone NOT NULL,
    pacient_id uuid,
    resursa_id uuid,
    tip_resursa character varying(255) NOT NULL,
    utilizator_id uuid,
    CONSTRAINT audit_log_actiune_check CHECK (((actiune)::text = ANY (ARRAY[('VIZUALIZARE'::character varying)::text, ('CREARE'::character varying)::text, ('MODIFICARE'::character varying)::text, ('STERGERE'::character varying)::text, ('EXPORT'::character varying)::text]))),
    CONSTRAINT audit_log_tip_resursa_check CHECK (((tip_resursa)::text = ANY (ARRAY[('DOSAR_PACIENT'::character varying)::text, ('DOCUMENT_MEDICAL'::character varying)::text, ('CHESTIONAR'::character varying)::text, ('DIAGNOSTIC'::character varying)::text, ('PROFIL_PACIENT'::character varying)::text])))
);


--
-- Name: categorii_chestionare; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categorii_chestionare (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    chestionar_id uuid,
    nume character varying(100) NOT NULL,
    descriere character varying(500),
    ordine integer,
    este_evaluabila boolean,
    sex_tinta character varying(255) DEFAULT 'AMBELE'::character varying NOT NULL
);


--
-- Name: chestionare; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chestionare (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nume character varying(100) NOT NULL,
    descriere text,
    instructiuni text,
    creat_la timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: cod_inregistrare; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cod_inregistrare (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cod_unic text NOT NULL,
    status character varying DEFAULT 'NEUTILIZAT'::public.status_cod NOT NULL,
    generat_de uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    atribuit uuid,
    cnp_destinatar text,
    email_destinatar text NOT NULL,
    rol_destinatar character varying NOT NULL,
    telefon_destinatar text,
    nume_destinatar text,
    prenume_destinatar text,
    CONSTRAINT cod_inregistrare_rol_destinatar_check CHECK (((rol_destinatar)::text = ANY (ARRAY[('ADMIN'::character varying)::text, ('MEDIC'::character varying)::text, ('PACIENT'::character varying)::text])))
);


--
-- Name: diagnostic_analize_ref; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.diagnostic_analize_ref (
    diagnostic_id uuid NOT NULL,
    analiza_id uuid
);


--
-- Name: diagnostic_chestionare_ref; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.diagnostic_chestionare_ref (
    diagnostic_id uuid NOT NULL,
    chestionar_id uuid
);


--
-- Name: diagnostic_medic; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.diagnostic_medic (
    id uuid NOT NULL,
    data_stabilire timestamp(6) without time zone,
    diagnostic_final character varying(255) NOT NULL,
    observatii_clinice text,
    pacient_id uuid NOT NULL
);


--
-- Name: documente_medicale; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documente_medicale (
    id uuid NOT NULL,
    cale_fisier_stocare character varying(255) NOT NULL,
    data_incarcare timestamp(6) without time zone,
    date_brute jsonb,
    date_validate jsonb,
    nume_fisier character varying(255),
    pacient_id uuid NOT NULL,
    status character varying(255),
    date_standardizate_loinc jsonb,
    date_interpretate_fhir jsonb,
    version bigint,
    candidati_loinc jsonb,
    date_fhir_standard jsonb,
    CONSTRAINT documente_medicale_status_check CHECK (((status)::text = ANY (ARRAY[('INCARCAT'::character varying)::text, ('VALIDAT'::character varying)::text, ('PROCESAT'::character varying)::text, ('STANDARDIZAT'::character varying)::text, ('INTERPRETAT'::character varying)::text, ('FHIR_GENERAT'::character varying)::text, ('EROARE'::character varying)::text, ('STERS_DE_PACIENT'::character varying)::text])))
);


--
-- Name: intrebari; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.intrebari (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    categorie_id uuid,
    text_intrebare text NOT NULL,
    creat_la timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tip_intrebare character varying(20) DEFAULT 'SCOR_0_3'::character varying,
    obligatorie boolean DEFAULT true,
    optiuni_json text,
    ordine integer,
    hpo_code character varying(50),
    hpo_term character varying(255),
    hpo_trigger_code character varying(255),
    hpo_trigger_term character varying(255)
);


--
-- Name: istoric_diagnoze; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.istoric_diagnoze (
    id uuid NOT NULL,
    data_rulare timestamp(6) without time zone NOT NULL,
    rezultat_json jsonb NOT NULL,
    pacient_id uuid NOT NULL
);


--
-- Name: medici; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medici (
    user_id uuid NOT NULL,
    specializare text NOT NULL,
    telefon character varying(20)
);


--
-- Name: notificari; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificari (
    id bigint NOT NULL,
    citita boolean NOT NULL,
    data_creare timestamp(6) without time zone NOT NULL,
    link_actiune character varying(255),
    mesaj character varying(500) NOT NULL,
    titlu character varying(100) NOT NULL,
    utilizator_destinatar_id uuid NOT NULL
);


--
-- Name: notificari_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.notificari ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.notificari_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pacienti; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pacienti (
    user_id uuid NOT NULL,
    medic_id uuid NOT NULL,
    id uuid NOT NULL,
    cnp character varying(255),
    data_nasterii date,
    greutate double precision,
    grupa_sangvina character varying(255),
    inaltime integer,
    sex character varying(255),
    CONSTRAINT pacienti_sex_check CHECK (((sex)::text = ANY (ARRAY[('MASCULIN'::character varying)::text, ('FEMININ'::character varying)::text, ('NEPRECIZAT'::character varying)::text])))
);


--
-- Name: raspunsuri_chestionare; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.raspunsuri_chestionare (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    pacient_id uuid,
    medic_id uuid,
    chestionar_id uuid,
    status character varying(20) DEFAULT 'IN_PROGRESS'::character varying,
    completat_la timestamp without time zone,
    scor_total_general numeric(5,2)
);


--
-- Name: raspunsuri_intrebari; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.raspunsuri_intrebari (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    raspuns_chestionar_id uuid,
    intrebare_id uuid,
    categorie_id uuid,
    scor integer,
    raspuns_text text,
    raspuns_numeric numeric(10,2),
    raspuns_data timestamp(6) without time zone,
    raspuns_json text,
    creat_la timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT raspunsuri_intrebari_scor_check CHECK (((scor >= 0) AND (scor <= 3)))
);


--
-- Name: rezultate_scoring; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rezultate_scoring (
    id uuid NOT NULL,
    calculat_la timestamp(6) without time zone NOT NULL,
    culoare_total_hex character varying(7) NOT NULL,
    culoare_total_nume character varying(20) NOT NULL,
    culori_categorii jsonb,
    interpretare_total character varying(100) NOT NULL,
    interpretari_categorii jsonb,
    necesita_recalculare boolean,
    numar_intrebari_raspunse integer NOT NULL,
    numar_total_intrebari integer NOT NULL,
    procentaj_completare numeric(5,2) NOT NULL,
    scor_total numeric(5,2) NOT NULL,
    scor_total_normalizat numeric(5,2),
    scoruri_categorii jsonb,
    versiune_formula character varying(20),
    raspuns_chestionar_id uuid NOT NULL
);


--
-- Name: tokenuri_resetare; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tokenuri_resetare (
    id uuid NOT NULL,
    data_expirare timestamp(6) without time zone,
    token character varying(255) NOT NULL,
    utilizator_id uuid NOT NULL
);


--
-- Name: utilizatori; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utilizatori (
    id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    email text NOT NULL,
    nume text NOT NULL,
    prenume text NOT NULL,
    rol character varying(50) NOT NULL,
    parola text NOT NULL
);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- Name: categorii_chestionare categorii_chestionare_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorii_chestionare
    ADD CONSTRAINT categorii_chestionare_pkey PRIMARY KEY (id);


--
-- Name: chestionare chestionare_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chestionare
    ADD CONSTRAINT chestionare_pkey PRIMARY KEY (id);


--
-- Name: cod_inregistrare cod_inregistrare_cod_unic_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cod_inregistrare
    ADD CONSTRAINT cod_inregistrare_cod_unic_key UNIQUE (cod_unic);


--
-- Name: cod_inregistrare cod_inregistrare_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cod_inregistrare
    ADD CONSTRAINT cod_inregistrare_pkey PRIMARY KEY (id);


--
-- Name: diagnostic_medic diagnostic_medic_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diagnostic_medic
    ADD CONSTRAINT diagnostic_medic_pkey PRIMARY KEY (id);


--
-- Name: documente_medicale documente_medicale_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documente_medicale
    ADD CONSTRAINT documente_medicale_pkey PRIMARY KEY (id);


--
-- Name: intrebari intrebari_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intrebari
    ADD CONSTRAINT intrebari_pkey PRIMARY KEY (id);


--
-- Name: istoric_diagnoze istoric_diagnoze_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.istoric_diagnoze
    ADD CONSTRAINT istoric_diagnoze_pkey PRIMARY KEY (id);


--
-- Name: medici medici_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medici
    ADD CONSTRAINT medici_pkey PRIMARY KEY (user_id);


--
-- Name: notificari notificari_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificari
    ADD CONSTRAINT notificari_pkey PRIMARY KEY (id);


--
-- Name: pacienti pacienti_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pacienti
    ADD CONSTRAINT pacienti_pkey PRIMARY KEY (id);


--
-- Name: pacienti pacienti_user_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pacienti
    ADD CONSTRAINT pacienti_user_id_unique UNIQUE (user_id);


--
-- Name: raspunsuri_chestionare raspunsuri_chestionare_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_chestionare
    ADD CONSTRAINT raspunsuri_chestionare_pkey PRIMARY KEY (id);


--
-- Name: raspunsuri_intrebari raspunsuri_intrebari_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_intrebari
    ADD CONSTRAINT raspunsuri_intrebari_pkey PRIMARY KEY (id);


--
-- Name: raspunsuri_intrebari raspunsuri_intrebari_raspuns_chestionar_id_intrebare_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_intrebari
    ADD CONSTRAINT raspunsuri_intrebari_raspuns_chestionar_id_intrebare_id_key UNIQUE (raspuns_chestionar_id, intrebare_id);


--
-- Name: rezultate_scoring rezultate_scoring_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rezultate_scoring
    ADD CONSTRAINT rezultate_scoring_pkey PRIMARY KEY (id);


--
-- Name: tokenuri_resetare tokenuri_resetare_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tokenuri_resetare
    ADD CONSTRAINT tokenuri_resetare_pkey PRIMARY KEY (id);


--
-- Name: rezultate_scoring uk2kqaane3rs4t9xvg1riu8b3l5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rezultate_scoring
    ADD CONSTRAINT uk2kqaane3rs4t9xvg1riu8b3l5 UNIQUE (raspuns_chestionar_id);


--
-- Name: tokenuri_resetare uk43j457hjsx6kpk9cwyurkf11o; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tokenuri_resetare
    ADD CONSTRAINT uk43j457hjsx6kpk9cwyurkf11o UNIQUE (utilizator_id);


--
-- Name: tokenuri_resetare uks8ivgeqhj2hsv9o03mf3vrplr; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tokenuri_resetare
    ADD CONSTRAINT uks8ivgeqhj2hsv9o03mf3vrplr UNIQUE (token);


--
-- Name: utilizatori utilizatori_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilizatori
    ADD CONSTRAINT utilizatori_email_key UNIQUE (email);


--
-- Name: utilizatori utilizatori_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilizatori
    ADD CONSTRAINT utilizatori_pkey PRIMARY KEY (id);


--
-- Name: idx_audit_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_data ON public.audit_log USING btree (data_ora);


--
-- Name: idx_audit_pacient; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_pacient ON public.audit_log USING btree (pacient_id);


--
-- Name: idx_audit_utilizator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_utilizator ON public.audit_log USING btree (utilizator_id);


--
-- Name: categorii_chestionare categorii_chestionare_chestionar_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorii_chestionare
    ADD CONSTRAINT categorii_chestionare_chestionar_id_fkey FOREIGN KEY (chestionar_id) REFERENCES public.chestionare(id) ON DELETE CASCADE;


--
-- Name: cod_inregistrare cod_inregistrare_atribuit_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cod_inregistrare
    ADD CONSTRAINT cod_inregistrare_atribuit_fkey FOREIGN KEY (atribuit) REFERENCES public.utilizatori(id);


--
-- Name: cod_inregistrare cod_inregistrare_generat_de_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cod_inregistrare
    ADD CONSTRAINT cod_inregistrare_generat_de_fkey FOREIGN KEY (generat_de) REFERENCES public.utilizatori(id);


--
-- Name: istoric_diagnoze fk2cvniujb91w2rgygtlcj0vxp0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.istoric_diagnoze
    ADD CONSTRAINT fk2cvniujb91w2rgygtlcj0vxp0 FOREIGN KEY (pacient_id) REFERENCES public.pacienti(id);


--
-- Name: diagnostic_analize_ref fk8eellsiwjyxwtyvxl1irk6f8k; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diagnostic_analize_ref
    ADD CONSTRAINT fk8eellsiwjyxwtyvxl1irk6f8k FOREIGN KEY (diagnostic_id) REFERENCES public.diagnostic_medic(id);


--
-- Name: documente_medicale fk97o3rlghhriem4o24s22pa8d7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documente_medicale
    ADD CONSTRAINT fk97o3rlghhriem4o24s22pa8d7 FOREIGN KEY (pacient_id) REFERENCES public.pacienti(id);


--
-- Name: notificari fk_notificari_utilizatori; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificari
    ADD CONSTRAINT fk_notificari_utilizatori FOREIGN KEY (utilizator_destinatar_id) REFERENCES public.utilizatori(id);


--
-- Name: notificari fkd8o2lbddoa8ngrqmwgft3vqfb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificari
    ADD CONSTRAINT fkd8o2lbddoa8ngrqmwgft3vqfb FOREIGN KEY (utilizator_destinatar_id) REFERENCES public.utilizatori(id);


--
-- Name: diagnostic_chestionare_ref fkkywirg14wqv2tr377jubwn7fu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diagnostic_chestionare_ref
    ADD CONSTRAINT fkkywirg14wqv2tr377jubwn7fu FOREIGN KEY (diagnostic_id) REFERENCES public.diagnostic_medic(id);


--
-- Name: tokenuri_resetare fkmr6cuxa6x4df9q4g8p7t93mc7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tokenuri_resetare
    ADD CONSTRAINT fkmr6cuxa6x4df9q4g8p7t93mc7 FOREIGN KEY (utilizator_id) REFERENCES public.utilizatori(id);


--
-- Name: diagnostic_medic fkmwgc34rg22241ny5rys492y1k; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diagnostic_medic
    ADD CONSTRAINT fkmwgc34rg22241ny5rys492y1k FOREIGN KEY (pacient_id) REFERENCES public.pacienti(id);


--
-- Name: rezultate_scoring fkoc6sv89eq0tpc3xd08bl91g8q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rezultate_scoring
    ADD CONSTRAINT fkoc6sv89eq0tpc3xd08bl91g8q FOREIGN KEY (raspuns_chestionar_id) REFERENCES public.raspunsuri_chestionare(id);


--
-- Name: intrebari intrebari_categorie_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intrebari
    ADD CONSTRAINT intrebari_categorie_id_fkey FOREIGN KEY (categorie_id) REFERENCES public.categorii_chestionare(id) ON DELETE CASCADE;


--
-- Name: medici medici_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medici
    ADD CONSTRAINT medici_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.utilizatori(id);


--
-- Name: pacienti pacienti_medic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pacienti
    ADD CONSTRAINT pacienti_medic_id_fkey FOREIGN KEY (medic_id) REFERENCES public.medici(user_id);


--
-- Name: pacienti pacienti_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pacienti
    ADD CONSTRAINT pacienti_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.utilizatori(id);


--
-- Name: raspunsuri_chestionare raspunsuri_chestionare_chestionar_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_chestionare
    ADD CONSTRAINT raspunsuri_chestionare_chestionar_id_fkey FOREIGN KEY (chestionar_id) REFERENCES public.chestionare(id);


--
-- Name: raspunsuri_chestionare raspunsuri_chestionare_medic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_chestionare
    ADD CONSTRAINT raspunsuri_chestionare_medic_id_fkey FOREIGN KEY (medic_id) REFERENCES public.medici(user_id);


--
-- Name: raspunsuri_chestionare raspunsuri_chestionare_pacient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_chestionare
    ADD CONSTRAINT raspunsuri_chestionare_pacient_id_fkey FOREIGN KEY (pacient_id) REFERENCES public.pacienti(id) ON DELETE CASCADE;


--
-- Name: raspunsuri_intrebari raspunsuri_intrebari_categorie_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_intrebari
    ADD CONSTRAINT raspunsuri_intrebari_categorie_id_fkey FOREIGN KEY (categorie_id) REFERENCES public.categorii_chestionare(id);


--
-- Name: raspunsuri_intrebari raspunsuri_intrebari_intrebare_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_intrebari
    ADD CONSTRAINT raspunsuri_intrebari_intrebare_id_fkey FOREIGN KEY (intrebare_id) REFERENCES public.intrebari(id);


--
-- Name: raspunsuri_intrebari raspunsuri_intrebari_raspuns_chestionar_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.raspunsuri_intrebari
    ADD CONSTRAINT raspunsuri_intrebari_raspuns_chestionar_id_fkey FOREIGN KEY (raspuns_chestionar_id) REFERENCES public.raspunsuri_chestionare(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--