-- Mozi Manager - fuggvenyek es triggerek (PL/pgSQL)

-- =====================================================================
-- 1. AUDIT MEZOK (az eredeti *_biu triggerek)
-- Egy kozos fuggveny minden tablahoz, nem kell tablankent megirni.
-- Ha az alkalmazas kitolti a letrehozta / modositotta mezot (a bejelentkezett
-- felhasznalo nevevel), azt megtartja, kulonben az adatbazis felhasznalojat irja be.
-- =====================================================================
create or replace function audit_mezok() returns trigger
    language plpgsql as
$$
begin
    if TG_OP = 'INSERT' then
        NEW.letrehozva := now();
        NEW.letrehozta := coalesce(NEW.letrehozta, current_user);
    else
        -- modositasnal a letrehozas adatai nem valtozhatnak
        NEW.letrehozva := OLD.letrehozva;
        NEW.letrehozta := OLD.letrehozta;
    end if;
    NEW.modositva := now();
    NEW.modositotta := coalesce(NEW.modositotta, current_user);
    return NEW;
end;
$$;

-- =====================================================================
-- 2. NAPLOZAS A HISTORY TABLAKBA (az eredeti *_aiud triggerek)
-- Dinamikus SQL: a tabla nevebol (TG_TABLE_NAME) rakja ossze a history tabla nevet.
-- A sort JSON-na alakitja, es a history tabla oszlopaival egyezo mezoket menti el.
-- =====================================================================
create or replace function naplozas() returns trigger
    language plpgsql as
$$
declare
    v_sor     jsonb;
    v_history text := TG_TABLE_NAME || '_history';
begin
    if TG_OP = 'DELETE' then
        v_sor := to_jsonb(OLD);
    else
        v_sor := to_jsonb(NEW);
    end if;

    v_sor := v_sor || jsonb_build_object(
            'muvelet', TG_OP,
            'modositas_ideje', now(),
            'modosito_felhasznalo', coalesce(v_sor ->> 'modositotta', current_user));

    execute format('insert into %I select * from jsonb_populate_record(null::%I, $1)', v_history, v_history)
        using v_sor;
    return null;
end;
$$;

-- A triggerek bekotese mind a hat tablara
do
$$
    declare
        v_tabla text;
    begin
        foreach v_tabla in array array ['kategoria', 'terem', 'film', 'vetites', 'ugyfel', 'jegy']
            loop
                execute format('create trigger trg_%s_biu before insert or update on %I
                                for each row execute function audit_mezok()', v_tabla, v_tabla);
                execute format('create trigger trg_%s_aiud after insert or update or delete on %I
                                for each row execute function naplozas()', v_tabla, v_tabla);
            end loop;
    end;
$$;

-- =====================================================================
-- 3. SZEK ELLENORZESE (uj)
-- A szekszam nem lehet nagyobb a terem kapacitasanal.
-- =====================================================================
create or replace function jegy_szek_ellenorzes() returns trigger
    language plpgsql as
$$
declare
    v_kapacitas integer;
begin
    select t.kapacitas
    into v_kapacitas
    from vetites v
             join terem t on t.terem_id = v.terem_id
    where v.vetites_id = NEW.vetites_id;

    if NEW.szek_szam > v_kapacitas then
        raise exception 'A %. szek nem letezik, a teremben % hely van.', NEW.szek_szam, v_kapacitas
            using errcode = 'check_violation';
    end if;
    return NEW;
end;
$$;

create trigger trg_jegy_szek
    before insert or update of szek_szam, vetites_id
    on jegy
    for each row
execute function jegy_szek_ellenorzes();

-- =====================================================================
-- 4. PONTGYUJTES (az eredeti trg_pontgyujtes)
-- Fizetett jegy utan 10 torzsvasarloi pont jar.
-- =====================================================================
create or replace function pontgyujtes() returns trigger
    language plpgsql as
$$
begin
    if NEW.status = 'FIZETVE' and (TG_OP = 'INSERT' or OLD.status <> 'FIZETVE') then
        update ugyfel
        set torzsvasarloi_pontok = torzsvasarloi_pontok + 10
        where ugyfel_id = NEW.ugyfel_id;
    end if;
    return null;
end;
$$;

create trigger trg_pontgyujtes
    after insert or update of status
    on jegy
    for each row
execute function pontgyujtes();

-- =====================================================================
-- 5. SZABAD HELYEK (az eredeti mozi_manager.szabad_helyek)
-- Nem letezo vetitesnel 0-t ad vissza, mint az eredeti.
-- =====================================================================
create or replace function szabad_helyek(p_vetites_id bigint) returns integer
    language plpgsql
    stable as
$$
declare
    v_kapacitas integer;
    v_foglalt   integer;
begin
    select t.kapacitas
    into v_kapacitas
    from vetites v
             join terem t on t.terem_id = v.terem_id
    where v.vetites_id = p_vetites_id;

    if not found then
        return 0;
    end if;

    select count(*)
    into v_foglalt
    from jegy
    where vetites_id = p_vetites_id
      and status <> 'TOROLT';

    return v_kapacitas - v_foglalt;
end;
$$;
