CREATE TABLE instance
(
  instance character varying NOT NULL,
  nodes integer,
  nvehicles integer,
  capacity numeric,
  CONSTRAINT instance_pkey PRIMARY KEY (instance)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE node
(
  instance character varying NOT NULL,
  node integer NOT NULL,
  xcoor numeric,
  ycoor numeric,
  demand numeric,
  early numeric,
  late numeric,
  service numeric,
  CONSTRAINT node_pkey PRIMARY KEY (instance, node),
  CONSTRAINT node_instance_fkey FOREIGN KEY (instance)
      REFERENCES instance (instance) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE TABLE result
(
  instance character varying NOT NULL,
  algorithm character varying NOT NULL,
  ejec integer NOT NULL,
  objective numeric,
  runtime numeric,
  iterations integer,
  status character varying,
  CONSTRAINT result_pkey PRIMARY KEY (algorithm, ejec, instance),
  CONSTRAINT result_instance_fkey FOREIGN KEY (instance)
      REFERENCES instance (instance) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE TABLE solution
(
  instance character varying NOT NULL,
  algorithm character varying NOT NULL,
  ejec integer NOT NULL,
  route integer,
  position integer,
  location integer,
  time numeric,
  CONSTRAINT solution_pkey PRIMARY KEY (instance, algorithm, ejec, route, position),
  CONSTRAINT solution_result_fkey FOREIGN KEY (algorithm, ejec, instance)
      REFERENCES result (algorithm, ejec, instance) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE TABLE output
(
  instance character varying NOT NULL,
  algorithm character varying NOT NULL,
  ejec integer NOT NULL,
  output text,
  CONSTRAINT output_pkey PRIMARY KEY (instance, algorithm, ejec),
  CONSTRAINT output_result_fkey FOREIGN KEY (algorithm, ejec, instance)
      REFERENCES result (algorithm, ejec, instance) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE TABLE transition
(
  instance character varying NOT NULL,
  algorithm character varying NOT NULL,
  ejec integer NOT NULL,
  low_level1 integer NOT NULL,
  low_level2 integer NOT NULL,
  probability numeric, 
  CONSTRAINT transition_pkey PRIMARY KEY (instance, algorithm, ejec, low_level1, low_level2),
  CONSTRAINT transition_result_fkey FOREIGN KEY (instance, algorithm, ejec)
      REFERENCES result (instance, algorithm, ejec) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE TABLE apply
(
  instance character varying NOT NULL,
  algorithm character varying NOT NULL,
  ejec integer NOT NULL,
  low_level integer NOT NULL,
  probability numeric, 
  CONSTRAINT apply_pkey PRIMARY KEY (instance, algorithm, ejec, low_level),
  CONSTRAINT apply_result_fkey FOREIGN KEY (instance, algorithm, ejec)
      REFERENCES result (instance, algorithm, ejec) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE TABLE minimum
(
  instance character varying NOT NULL,
  min_value numeric,
  CONSTRAINT minimum_pkey PRIMARY KEY (instance),
  CONSTRAINT minimum_instance_fkey FOREIGN KEY (instance)
      REFERENCES instance (instance) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
