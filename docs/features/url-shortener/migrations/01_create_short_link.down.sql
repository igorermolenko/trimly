-- Rollback for 01_create_short_link.up.sql.
-- Flyway in this repo is forward-only (repo ADR-0003): this file is NOT promoted into
-- src/main/resources/db/migration/. It is a manual rollback script, run by hand before the table holds
-- real links (afterwards, rolling back destroys every issued short code). On a database Flyway has
-- already migrated, also delete the matching row from flyway_schema_history.
-- Dropping the table also drops pk_short_link, uq_short_link_short_code and the CHECK constraints.

DROP TABLE IF EXISTS short_link;
