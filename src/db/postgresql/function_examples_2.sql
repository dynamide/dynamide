drop type authorize_user_result cascade;

create type authorize_user_result as (permission_id int, user_id int);

CREATE or replace FUNCTION authorize_user (text) RETURNS setof authorize_user_result  AS '
    DECLARE
        plogin ALIAS FOR $1;
        userIDLookedUp integer;
        SCHWARMANIZER_NONE integer;
        SCHWARMANIZER integer;
        res authorize_user_result%ROWTYPE;
        deleteCount integer;
    BEGIN
        raise DEBUG ''begin authorize_user'';
        SELECT id from users
            into userIDLookedUp
            where plogin = login;
        select id from permissions
            into SCHWARMANIZER_NONE
            where code = ''SCHWARMANIZER_NONE'';
        select id from permissions
            into SCHWARMANIZER
            where code = ''SCHWARMANIZER'';
        raise DEBUG ''userIDLookedUp: % SCHWARMANIZER_NONE: % SCHWARMANIZER: %'',
              userIDLookedUp,SCHWARMANIZER_NONE,SCHWARMANIZER;

        delete from user_permissions
        where user_id = userIDLookedUp and permission_id = SCHWARMANIZER_NONE;
        GET DIAGNOSTICS deleteCount = ROW_COUNT;
        raise DEBUG ''1: deleteCount: % for userIDLookedUp: % and permission_id: %'',deleteCount, userIDLookedUp, SCHWARMANIZER_NONE;

        delete from user_permissions
        where user_id = userIDLookedUp and permission_id = SCHWARMANIZER;
        GET DIAGNOSTICS deleteCount = ROW_COUNT;
        raise DEBUG ''2: deleteCount: % for userIDLookedUp: % and permission_id: %'',deleteCount, userIDLookedUp, SCHWARMANIZER;

        insert into user_permissions
        (permission_id, user_id)
        values
        (SCHWARMANIZER, userIDLookedUp);

        insert into user_permissions
        (permission_id, user_id)
        values
        (SCHWARMANIZER_NONE, userIDLookedUp);

        raise DEBUG ''before loop plogin: %'', plogin;

        FOR res IN SELECT * FROM user_permissions where user_id = userIDLookedUp LOOP
           raise DEBUG ''res: % userIDLookedUp: %'',res.user_id, userIDLookedUp;
           RETURN next res;
        END LOOP;

        raise DEBUG ''end authorize_user'';
        return;
    END;
' LANGUAGE 'plpgsql';

CREATE or replace FUNCTION authorize_user_outer (text) RETURNS setof authorize_user_result  AS '
DECLARE
res RECORD;
BEGIN
        FOR res IN SELECT * from authorize_user($1) LOOP
           raise DEBUG ''outer.... res: % '',res.user_id;
           RETURN next res;
        END LOOP;
        RETURN;

END;
' LANGUAGE 'plpgsql';
