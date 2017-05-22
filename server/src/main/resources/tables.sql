CREATE ROLE scalafiddle;

CREATE DATABASE scalafiddle;

-- log in to scalafiddle DB

CREATE TABLE "fiddle" (
  "id"            VARCHAR NOT NULL,
  "version"       INTEGER NOT NULL,
  "name"          VARCHAR NOT NULL,
  "description"   VARCHAR NOT NULL,
  "sourcecode"    VARCHAR NOT NULL,
  "libraries"     VARCHAR NOT NULL,
  "scala_version" VARCHAR NOT NULL,
  "user"          VARCHAR NOT NULL,
  "parent"        VARCHAR,
  "created"       BIGINT  NOT NULL,
  "removed"       BOOLEAN NOT NULL
);

CREATE TABLE "user" (
  "user_id"    VARCHAR NOT NULL,
  "login_info" VARCHAR NOT NULL,
  "first_name" VARCHAR,
  "last_name"  VARCHAR,
  "full_name"  VARCHAR,
  "email"      VARCHAR,
  "avatar_url" VARCHAR,
  "activated"  BOOLEAN NOT NULL
);

CREATE TABLE "access" (
  "id"        SERIAL PRIMARY KEY,
  "fiddle_id" VARCHAR NOT NULL,
  "version"   INTEGER NOT NULL,
  "timestamp" BIGINT  NOT NULL,
  "user_id"   VARCHAR,
  "embedded"  BOOLEAN NOT NULL,
  "source_ip" VARCHAR NOT NULL
);

ALTER TABLE "fiddle"
  ADD CONSTRAINT "pk_fiddle" PRIMARY KEY ("id", "version");

ALTER TABLE "user"
  ADD CONSTRAINT "pk_user" PRIMARY KEY ("user_id");

CREATE INDEX "access_id_idx"
  ON "access" USING BTREE (fiddle_id);

CREATE INDEX "access_time_idx"
  ON "access" USING BTREE (timestamp);

GRANT ALL ON TABLE "fiddle" TO scalafiddle;

GRANT ALL ON TABLE "user" TO scalafiddle;

GRANT ALL ON TABLE "access" TO scalafiddle;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO scalafiddle;
