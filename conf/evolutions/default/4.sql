# Add slug to event and location

# --- !Ups

ALTER TABLE EVENT
ADD SLUG VARCHAR(64)

# --- !Downs

ALTER TABLE EVENT
DROP COLUMN SLUG


