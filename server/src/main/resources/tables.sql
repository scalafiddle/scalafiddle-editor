create table "fiddle" ("id" VARCHAR NOT NULL,"version" INTEGER NOT NULL,"name" VARCHAR NOT NULL,
"description" VARCHAR NOT NULL,"sourcecode" VARCHAR NOT NULL,"libraries" VARCHAR NOT NULL,
"user" VARCHAR NOT NULL,"parent" VARCHAR,"created" BIGINT NOT NULL,"removed" BOOLEAN NOT NULL);

alter table "fiddle" add constraint "pk_fiddle" primary key("id","version");
