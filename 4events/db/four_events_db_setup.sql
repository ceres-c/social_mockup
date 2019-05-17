--
-- PostgreSQL database setup script
--

--
-- Name: four_events_db; Type: DATABASE; Schema: -; Owner: postgres
--

\connect four_events_db

--
-- Name: default_event; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.default_event (
    eventID character(36) NOT NULL,
    creatorID character(36) NOT NULL,
    eventType text NOT NULL,
    published boolean NOT NULL,
    registeredUsers character(36)[], -- A event can have no registered users if created by a user which does not comply with event-specific constraints
    currentState text NOT NULL,
    title text,
    participantsNum integer NOT NULL,
    deadline timestamp without time zone NOT NULL,
    location text NOT NULL,
    startDate timestamp without time zone NOT NULL,
	duration numeric,
    cost numeric(12,2) NOT NULL,
    inQuota text,
    endDate timestamp without time zone,
    notes text,
    CONSTRAINT default_event_cost_check CHECK ((cost >= 0.0)),
    CONSTRAINT default_event_participants_num_check CHECK ((participantsNum > 0)),
    CONSTRAINT default_event_pkey PRIMARY KEY (eventID)
);

--
-- Name: soccer_game; Type: TABLE; Schema: public; Owner: postgres; Inherits: default_event
--

CREATE TABLE public.soccer_game (
    gender character(1) NOT NULL,
    ageMin smallint NOT NULL,
    ageMax smallint NOT NULL
)
INHERITS (public.default_event);

--
-- Name: categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categories (
	event_type text,
	name text,
	descr text,
	CONSTRAINT categories_pkey PRIMARY KEY (event_type)
);

--
-- POPULATING TABLES WITH STUB VALUES FOR TESTING PURPOSES
--
INSERT INTO public.categories VALUES ('soccer_game', 'Partita di calcio', 'Sport più pagato al mondo che consiste nel rincorrere un pezzo di cuoio');

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    username text NOT NULL,
    hashedPassword character(128) NOT NULL,
    userID character(36) NOT NULL,
    gender character(1) NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (username)
);