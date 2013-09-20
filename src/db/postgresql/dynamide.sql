/*
 See also dynamide_schema.sql which defines the user login components.
 First:
   
 Source with:
   (Be sure to also pull in dynamide_schema)

    \i /c/dynamide/src/db/postgresql/dynamide.sql
    \i /c/dynamide/src/db/postgresql/dynamide_schema.sql

*/
/* AS of 8.1.4, pspgsql seems to be installed already.
BEGIN;
  CREATE FUNCTION plpgsql_call_handler () RETURNS language_handler
    AS '$libdir/plpgsql', 'plpgsql_call_handler'
    LANGUAGE C;

  CREATE TRUSTED PROCEDURAL LANGUAGE plpgsql HANDLER plpgsql_call_handler;
COMMIT;
*/
SET search_path = public, pg_catalog;

BEGIN TRANSACTION;

CREATE SEQUENCE errorlog_id_seq START 1;

CREATE TABLE errorlog (
  id INT8 DEFAULT nextval('errorlog_id_seq'),
  host varchar DEFAULT '',
  sessionid varchar DEFAULT '',
  threadid varchar DEFAULT '',
  applicationid varchar DEFAULT '',
  level varchar DEFAULT '',
  login varchar DEFAULT '',
  account varchar DEFAULT'',
  resourcename varchar DEFAULT '',       -- e.g. xml filename
  time timestamp DEFAULT current_timestamp,
  stacktrace BYTEA,
  message BYTEA
);
CREATE UNIQUE INDEX errorlog_pkey ON errorlog (id); --equivalent to "PRIMARY KEY (id)" in tabledef

COMMIT;