CREATE or replace FUNCTION foo (TEXT, RECORD) RETURNS TIMESTAMP AS '
    DECLARE
        logtxt ALIAS FOR $1;
        curtime timestamp;
        t bl alias for $2;
    BEGIN
        raise notice ''hello, world!'';
        raise notice ''hello'' || tbl.id;
        curtime := ''now'';
        --INSERT INTO logtable VALUES (logtxt, curtime);
        RETURN curtime;
    END;
' LANGUAGE 'plpgsql';

drop type authorize_user_result cascade;
create type authorize_user_result as (permission_id int, user_id int);

CREATE or replace FUNCTION authorize_user (text) RETURNS setof authorize_user_result  AS '
    DECLARE
        plogin ALIAS FOR $1;
        USERS_ID integer;
        SCHWARMANIZER_NONE integer;
        SCHWARMANIZER integer;
        res authorize_user_result%ROWTYPE;
        deleteCount integer;
    BEGIN
        raise notice ''begin authorize_user'';
        SELECT id from users
            into USERS_ID
            where plogin = login;
        select id from permissions
            into SCHWARMANIZER_NONE
            where code = ''SCHWARMANIZER_NONE'';
        select id from permissions
            into SCHWARMANIZER
            where code = ''SCHWARMANIZER'';
        raise notice ''USERS_ID: % SCHWARMANIZER_NONE: % SCHWARMANIZER: %'',USERS_ID,SCHWARMANIZER_NONE,SCHWARMANIZER;

        delete from user_permissions
        where user_id = USERS_ID and permission_id = SCHWARMANIZER_NONE;
        GET DIAGNOSTICS deleteCount = ROW_COUNT;
        raise notice ''deleteCount: %'',deleteCount;

        delete from user_permissions
        where user = USERS_ID and permission_id = SCHWARMANIZER;
        GET DIAGNOSTICS deleteCount = ROW_COUNT;
        raise notice ''deleteCount: %'',deleteCount;

        /*insert into user_permissions
        (permission_id, user_id)
        values
        (SCHWARMANIZER, USERS_ID);
        */
        --select into res * from user_permissions where user_id = 1;

        raise notice ''before loop plogin: %'', plogin;

        FOR res IN SELECT * FROM user_permissions where user_id = USERS_ID LOOP
           raise notice ''res: % USERS_ID: %'',res.user_id, USERS_ID;
           RETURN next res;
        END LOOP;

        raise notice ''end authorize_user'';
        return;
    END;
' LANGUAGE 'plpgsql';

drop type footype cascade;
create type footype as (f1 int, f2 text);
create or replace function retfoo(int, text) returns footype as '
 declare
  result footype%ROWTYPE;
 begin
  select into result $1, $2;
  return result;
 end;
' language 'plpgsql';

CREATE or replace FUNCTION authorize_user_outer (text) RETURNS setof authorize_user_result  AS '
DECLARE
res RECORD;
BEGIN
        FOR res IN SELECT * from authorize_user($1) LOOP
           raise notice ''outer.... res: % '',res.user_id;
           RETURN next res;
        END LOOP;
        RETURN;

END;
' LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION permissiontest() RETURNS setof authorize_user_result AS'
  DECLARE
     reg record;
  BEGIN
     FOR reg IN SELECT * FROM user_permissions LOOP
        raise notice ''reg: %'',reg.user_id;
            RETURN next reg;
     END LOOP;
    RETURN;
  END;
' LANGUAGE 'plpgsql';




/*
## params:
 ##   USER : String : should be the id of the user in the users table

use net_roots;
select @USERS_ID := id from users where login = '$USER';
select @SCHWARMANIZER_NONE := id from permissions where code = 'SCHWARMANIZER_NONE';
select @SCHWARMANIZER := id from permissions where code = 'SCHWARMANIZER';

delete from user_permissions
where user = @USERS_ID and permission = @SCHWARMANIZER_NONE;

delete from user_permissions
where user = @USERS_ID and permission = @SCHWARMANIZER;

insert into user_permissions
(permission, user)
values
(@SCHWARMANIZER, @USERS_ID);





USERS_ID id := SELECT from users where login = 'laramie';
select @SCHWARMANIZER_NONE := id from permissions where code = 'SCHWARMANIZER_NONE';
select @SCHWARMANIZER := id from permissions where code = 'SCHWARMANIZER';

 */