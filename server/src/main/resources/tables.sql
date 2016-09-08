CREATE TABLE "fiddle" (
  "id"          VARCHAR NOT NULL,
  "version"     INTEGER NOT NULL,
  "name"        VARCHAR NOT NULL,
  "description" VARCHAR NOT NULL,
  "sourcecode"  VARCHAR NOT NULL,
  "libraries"   VARCHAR NOT NULL,
  "user"        VARCHAR NOT NULL,
  "parent"      VARCHAR,
  "created"     BIGINT  NOT NULL,
  "removed"     BOOLEAN NOT NULL
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

ALTER TABLE "fiddle"
  ADD CONSTRAINT "pk_fiddle" PRIMARY KEY ("id", "version");

ALTER TABLE "user"
  ADD CONSTRAINT "pk_user" PRIMARY KEY ("user_id");
