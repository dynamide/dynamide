/*
  source with:
   \c template1
   create database dynamide;
   \c dynamide
   \i src/db/postgresql/dynamide_schema.sql

 or

   \c template1
   create database dynamide;
   \c dynamide
   \i src/db/postgresql/dynamide.sql
   \i src/db/postgresql/dynamide_schema.sql

  If you wish to be able to see the objects in your session,
  execute a statement like this:

    SET search_path = public, pg_catalog, DynamideUser;

 note the diff between session_user and current_user
*/

BEGIN;

CREATE SCHEMA DynamideUser;

CREATE SEQUENCE DynamideUser.groupprefs_id_seq;
CREATE TABLE DynamideUser.groupprefs (
  id INT4 DEFAULT nextval('DynamideUser.groupprefs_id_seq'),
  name varchar(32) DEFAULT '',
  value varchar(128) DEFAULT ''
);
CREATE UNIQUE INDEX groupprefs_pkey ON DynamideUser.groupprefs (id); --equivalent to "PRIMARY KEY (id)" in tabledef

INSERT INTO DynamideUser.groupprefs VALUES (1,'announce_email','');
INSERT INTO DynamideUser.groupprefs VALUES (2,'authorize_members','false');


CREATE SEQUENCE DynamideUser.permissions_id_seq;

CREATE TABLE DynamideUser.permissions (
  code varchar(32) DEFAULT NULL,
  description varchar(64) DEFAULT NULL
);


CREATE TABLE DynamideUser.user_permissions (
  permission_code varchar(16) NOT NULL DEFAULT '0',
  user_id INT4 NOT NULL DEFAULT '0'
);

CREATE UNIQUE INDEX id_permissions_index ON DynamideUser.permissions (code);

CREATE SEQUENCE DynamideUser.users_id_seq;

CREATE TABLE DynamideUser.users (
  id INT4 DEFAULT nextval('DynamideUser.users_id_seq') UNIQUE,
  familiarName varchar DEFAULT '',
  Surname varchar DEFAULT '',
  othername varchar DEFAULT '',
  password varchar DEFAULT '',
  login varchar NOT NULL DEFAULT '' UNIQUE,
  email varchar DEFAULT ''
);
--not needed, since UNIQUE. CREATE UNIQUE INDEX users_pkey ON DynamideUser.users(id); --equivalent to "PRIMARY KEY (id)" in tabledef

CREATE or replace FUNCTION DynamideUser.is_admin (text) RETURNS boolean
SECURITY DEFINER
AS '
    DECLARE
        paramLogin ALIAS FOR $1;
        foo varchar;
    BEGIN
        select into foo * from DynamideUser.view_permissions where login = paramLogin and permission_code = ''ADMIN'';
        if FOUND = false then
          return false;
        end if;
        return true;
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION DynamideUser.authorize_user (text) RETURNS boolean
SECURITY DEFINER
AS '
    DECLARE
        paramLogin ALIAS FOR $1;
        res boolean;
    BEGIN
        select into res * from DynamideUser.authorize_user_inner(paramLogin, ''MEMBER'');
        return (res = TRUE);
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION DynamideUser.authorize_user (varchar) RETURNS boolean
SECURITY DEFINER
AS '
    DECLARE
        paramLogin ALIAS FOR $1;
        res boolean;
    BEGIN
        raise DEBUG ''begin authorize_user'';
        select into res * from DynamideUser.authorize_user_inner(paramLogin, ''MEMBER'');
        return (res = TRUE);
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION DynamideUser.unauthorize_user (text) RETURNS boolean
SECURITY DEFINER
AS '
    DECLARE
        paramLogin ALIAS FOR $1;
        res boolean;
    BEGIN
        select into res * from DynamideUser.authorize_user_inner(paramLogin, ''MEMBER'');
        return (res = TRUE);
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION DynamideUser.authorize_user_inner (text, text) RETURNS boolean AS '
    DECLARE
        paramLogin ALIAS FOR $1;
        paramPermission ALIAS FOR $2;
        userIDLookedUp integer;
        deleteCount integer;
        res boolean;
        us varchar;
    BEGIN
        raise DEBUG ''begin authorize_user_inner'';
        select into us session_user;
        res := DynamideUser.is_admin(us);
        if res = false then
          raise WARNING ''session_user % not allowed to call authorize_user_inner'', us;
          return false;
        end if;

        select into userIDLookedUp
            u.id from DynamideUser.users u
            where paramLogin = u.login;

        if userIDLookedUp is null then
          raise notice ''user % was not found.'', paramLogin;
          return false;
        end if ;

        delete from DynamideUser.user_permissions
             where permission_code = paramPermission
             and user_id = userIDLookedUp;

        insert into DynamideUser.user_permissions
            (permission_code, user_id)
            values
            (paramPermission, userIDLookedUp);

        raise DEBUG ''end authorize_user'';
        return FOUND;
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION DynamideUser.user_has_permission (text, text) RETURNS boolean AS '
    DECLARE
        paramLogin ALIAS FOR $1;
        paramPermissionCode ALIAS for $2;
        res boolean;
        c integer;
    BEGIN
        select into c count(*) from DynamideUser.view_permissions
          where login = paramLogin and permission_code = paramPermissionCode;
        return (c > 0);
    END;
' LANGUAGE 'plpgsql';

--you need to do these manually if you are debugging:
--drop VIEW view_pending;
--drop VIEW view_authorized;
--DROP VIEW view_permissions CASCADE;

CREATE VIEW DynamideUser.view_permissions AS
  select users.id as user_id, login, codeuser.permission_code
  from
    (
       select user_id, p.code, permission_code
       from
          DynamideUser.permissions p
       inner join
          DynamideUser.user_permissions up
       on up.permission_code = p.code
    )
    as codeuser
  right join
     DynamideUser.users
  on DynamideUser.users.id = codeuser.user_id
  order by login;
-- Usage:
-- select * from view_permissions;
-- select * from view_permissions where login = 'laramie';

--drop view view_all_permissions;
CREATE VIEW DynamideUser.view_all_permissions AS
  select users.id as user_id, login, codeuser.permission_code
  from
    (
       select user_id, code, permission_code
       from
          DynamideUser.permissions p
       inner join
          DynamideUser.user_permissions up
       on up.permission_code = p.code
    )
    as codeuser
  right join
     DynamideUser.users
  on DynamideUser.users.id = codeuser.user_id
  order by login;
-- Usage:
-- select * from view_permissions;
-- select * from view_permissions where login = 'laramie';

CREATE VIEW DynamideUser.view_authorized AS
  select  DISTINCT v.user_id, v.login from DynamideUser.view_permissions v
  where permission_code = 'MEMBER'
  ORDER BY v.login;

CREATE VIEW DynamideUser.view_pending AS
  select v.user_id, v.login from DynamideUser.view_permissions v
  where permission_code = ''
  or permission_code IS NULL
  ORDER BY v.login;

/*
 test:
   delete from users where login  = 'mojo2';
   select * from DynamideUser.addUser('mojo2', 'mojopass', 'mo', 'jo', '', 'mojo@mojo.com');
*/
--params: addUser(login, password, familiarName, surname, otherName, eMail)
--return values: ALLOWED, PENDING, DENIED
CREATE or replace FUNCTION DynamideUser.addUser (varchar, varchar, varchar, varchar, varchar, varchar)
RETURNS varchar AS '
    DECLARE
        paramLogin ALIAS for $1;
        paramPassword ALIAS for $2;
        paramFamiliarName ALIAS for $3;
        paramSurname ALIAS for $4;
        paramOtherName ALIAS for $5;
        paramEmail ALIAS for $6;
        res varchar;
        c integer;
    BEGIN
        select into c count(*) from DynamideUser.users where login = paramLogin;
        if (c > 0) then
           raise WARNING ''User exists %'', paramLogin;
           return ''DENIED'';
        end if;

        insert into DynamideUser.users (login,
                           password,
                           familiarName,
                           surname,
                           otherName,
                           eMail)
            values (
                   paramLogin,
                   paramPassword,
                   paramFamiliarName,
                   paramSurname,
                   paramOtherName,
                   paramEmail
                   );
        raise notice ''adding  % %  % '', paramLogin, paramSurname, paramEmail;
         return DynamideUser.pendUser(paramLogin);
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION loginToID (text) RETURNS int4
AS '
    DECLARE
        paramLogin ALIAS FOR $1;
        res int4;
    BEGIN
        select into res id from DynamideUser.users where login = paramLogin;
        return res;
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION DynamideUser.pendUser (varchar)
RETURNS varchar AS '
    DECLARE
        paramLogin ALIAS for $1;
        res varchar;
        iuser_id integer;
        c integer;
        authorize varchar;
        thePermission varchar;
        ires integer;
    BEGIN
        raise notice ''DynamideUser.pendUser %  '', paramLogin;

        thePermission = ''MEMBER'';
        select into ires loginToID(''citizensvote@b-rant.com'');
        raise notice ''DynamideUser.pendUser ires: %  '', ires;
        select into ires loginToID(paramLogin);
        raise notice ''DynamideUser.pendUser ires: %  '', ires;

        select into iuser_id
            id from DynamideUser.users
            where login = paramLogin;
        raise notice ''adding user_id  %  with perm % '', iuser_id, thePermission;

        authorize := DynamideUser.authorize_members();

        if (authorize = ''true'') then
            return ''PENDING'';
        end if;

        insert into DynamideUser.user_permissions (permission_code, user_id) values (thePermission, iuser_id);
        raise notice ''added user_id  %  with perm % '', iuser_id, thePermission;
        return ''ALLOWED'';
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION DynamideUser.authorize_members () RETURNS varchar AS '
    DECLARE
        res varchar;
    BEGIN
        select into res
            value from DynamideUser.groupprefs
            where name = ''authorize_members'';
        return res;
    END;
' LANGUAGE 'plpgsql';


CREATE TABLE DynamideUser.cookies (
  LOGIN varchar DEFAULT '',
  NAME varchar DEFAULT '',
  VALUE varchar DEFAULT ''
);
CREATE INDEX cookies_key ON DynamideUser.cookies(LOGIN);

CREATE FUNCTION DynamideUser.getUserCookie (varchar, varchar) RETURNS text
SECURITY DEFINER
AS '
  SELECT c.VALUE
  from DynamideUser.cookies c
  where c.LOGIN = $1
  and c.NAME = $2
  limit 1;
' LANGUAGE 'SQL';

CREATE or replace FUNCTION DynamideUser.setUserCookie (varchar, varchar, varchar)
RETURNS varchar AS '
    DECLARE
        paramLogin ALIAS for $1;
        paramName ALIAS for $2;
        paramValue ALIAS for $3;
        cookieLookedUp varchar;
    BEGIN
        select into cookieLookedUp
            c.name from DynamideUser.cookies c
            where paramLogin = c.login
            and paramName = c.name;

        if cookieLookedUp is null then
          raise notice ''user % cookie % was not found.'', paramLogin, paramName;
          insert into DynamideUser.cookies(login, name, value)
          values (paramLogin, paramName, paramValue);
          return ''INSERTED'';
        end if ;

        update DynamideUser.cookies
        set value = paramValue
        where login = paramLogin and name = paramName;
        return ''UPDATED'';
    END;
' LANGUAGE 'plpgsql';



-- ******************* DATA ***********************************************

INSERT INTO DynamideUser.permissions (code, description) VALUES ('ADMIN','Administrator for dynamide schema');
INSERT INTO DynamideUser.permissions (code, description) VALUES ('MEMBER','Member for application');

select * from DynamideUser.addUser('admin', 'password', 'Local', 'Admin', '', 'application-admin@yourcompany.com');
select * from DynamideUser.addUser('laramie', 'password', 'Local', 'Admin', '', 'laramie@dynamide.com');

-- This uses user by id not login:
INSERT INTO DynamideUser.user_permissions(permission_code, user_id) VALUES ('ADMIN',1);
INSERT INTO DynamideUser.user_permissions(permission_code, user_id) VALUES ('ADMIN',2);

SELECT DynamideUser.authorize_user('admin'::varchar);

SET search_path = public, pg_catalog, DynamideUser;

--select * from addUser('mojo2', 'mojopass', 'mo', 'jo', '', 'mojo@mojo.com');
select * from view_permissions;
select * from view_authorized;
select * from view_pending;
select user_has_permission('admin', 'ADMIN');
select is_admin('admin');

COMMIT;