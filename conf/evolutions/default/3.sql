# Add event to location relation

# --- !Ups

ALTER TABLE EVENT
ADD LOCATION_ID BIGINT(20);

# --- !Downs

ALTER TABLE EVENT
DROP COLUMN LOCATION_ID;
