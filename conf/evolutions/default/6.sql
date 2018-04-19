# Remove date information from event

# --- !Ups

ALTER TABLE EVENT DROP COLUMN STARTS_AT
ALTER TABLE EVENT DROP COLUMN ENDS_AT

# --- !Downs

ALTER TABLE EVENT ADD STARTS_AT TIMESTAMP NOT NULL
ALTER TABLE EVENT ADD ENDS_AT TIMESTAMP NULL

