--
-- PostgreSQL database setup script
--
END; -- Otherwise it's impossible to create a database in a sql script with autocommit enabled
CREATE DATABASE four_events_db;

--
-- Name: four_events_db; Type: DATABASE; Schema: -; Owner: postgres
--

\connect four_events_db

--
-- Name: default_event; Type: TABLE; Schema: public; Owner: postgres
--

BEGIN;
CREATE TABLE public.default_event (
    eventID character(36) NOT NULL,
    creatorID character(36) NOT NULL,
    eventType text NOT NULL,
    published boolean NOT NULL,
    registeredUsers character(36)[], -- A event can have no registered users if created by a user which does not comply with event-specific constraints
    currentState text NOT NULL,
    participantsMax integer NOT NULL,
    title text,
    participantsMin integer NOT NULL,
    participantsSurplus integer,
    registrationDeadline timestamp without time zone NOT NULL,
    location text NOT NULL,
    startDate timestamp without time zone NOT NULL,
	duration numeric,
    deregistrationDeadline timestamp without time zone NOT NULL,
    cost numeric(12,2) NOT NULL,
    inQuota text,
    endDate timestamp without time zone,
    notes text,
    CONSTRAINT default_event_cost_check CHECK ((cost >= 0.0)),
    CONSTRAINT default_event_participants_num_check CHECK ((participantsMin > 0)),
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
-- Name: soccer_game; Type: TABLE; Schema: public; Owner: postgres; Inherits: default_event
--

CREATE TABLE public.mountain_hiking (
    length integer NOT NULL,
    heightDiff integer,
    coachID character(36),
    coachAmount int,
    lodgeID character(36),
    lodgeAmount int,
    lunchID character(36),
    lunchAmount int
)
INHERITS (public.default_event);

--
-- Name: categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categories (
	event_type text,
	CONSTRAINT categories_pkey PRIMARY KEY (event_type)
);

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
	username text NOT NULL,
    hashedPassword character(128) NOT NULL,
    userID character(36) NOT NULL,
    gender character(1) NOT NULL,
    age integer,
    favoriteCategories text[],
    CONSTRAINT users_pkey PRIMARY KEY (username)
);

--
-- Name: eventNotifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.eventNotifications (
    notificationID character(36) NOT NULL,
    eventID character(36) NOT NULL,
    recipientID character(36) NOT NULL,
    read boolean NOT NULL,
    title varchar,
    content varchar,
    CONSTRAINT eventNotifications_pkey PRIMARY KEY (notificationID)
);

CREATE TABLE public.optionalCost (
    costID character(36) NOT NULL,
    eventID character(36) NOT NULL,
    userID character(36) NOT NULL
);

--
-- Populating table with the only available category
--
INSERT INTO public.categories VALUES ('soccer_game');
INSERT INTO public.categories VALUES ('mountain_hiking');
END;