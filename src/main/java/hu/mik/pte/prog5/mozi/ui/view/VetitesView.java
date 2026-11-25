package hu.mik.pte.prog5.mozi.ui.view;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import hu.mik.pte.prog5.mozi.model.Film;
import hu.mik.pte.prog5.mozi.model.Terem;
import hu.mik.pte.prog5.mozi.model.Vetites;
import hu.mik.pte.prog5.mozi.model.nezet.MusorElem;
import hu.mik.pte.prog5.mozi.service.AdminService;
import hu.mik.pte.prog5.mozi.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "vetitesek", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class VetitesView extends AdminCrudNezet<Vetites> {

    private final AdminService adminService;
    private final Map<Long, Film> filmek = new HashMap<>();
    private final Map<Long, Terem> termek = new HashMap<>();
    // vetites_id -> a v_musor nezet sora, ebbol jon a foglaltsag
    private final Map<Long, MusorElem> musor = new HashMap<>();
    private final ComboBox<Film> film = new ComboBox<>();
    private final ComboBox<Terem> terem = new ComboBox<>();

    public VetitesView(AdminService adminService) {
        super(Vetites.class, Vetites::new, "vetites.cim");
        this.adminService = adminService;

        var kezdesOszlop = this.grid.addColumn(v -> IDO_FORMATUM.format(v.getKezdesIdo()))
                .setHeader(this.getTranslation("vetites.kezdes"))
                .setComparator(Vetites::getKezdesIdo).setSortable(true);
        this.grid.addColumn(v -> this.filmCim(v.getFilmId()))
                .setHeader(this.getTranslation("vetites.film")).setSortable(true);
        this.grid.addColumn(v -> this.teremNev(v.getTeremId()))
                .setHeader(this.getTranslation("vetites.terem")).setSortable(true);
        this.grid.addColumn(this::foglaltsag).setHeader(this.getTranslation("vetites.szabad"));
        this.auditOszlopok();
        this.grid.sort(List.of(new GridSortOrder<>(kezdesOszlop, SortDirection.DESCENDING)));

        this.film.setLabel(this.getTranslation("vetites.film"));
        this.film.setItemLabelGenerator(f -> f.getCim() + " (" + f.getHosszPerc() + " perc)");
        this.terem.setLabel(this.getTranslation("vetites.terem"));
        this.terem.setItemLabelGenerator(t -> t.getNev() + " (" + t.getKapacitas() + ")");
        DateTimePicker kezdes = new DateTimePicker(this.getTranslation("vetites.kezdes"));
        kezdes.setStep(Duration.ofMinutes(15));

        this.binder.forField(this.film)
                .asRequired(this.getTranslation("validacio.hibas"))
                .withConverter(f -> f == null ? null : f.getFilmId(), this.filmek::get)
                .bind("filmId");
        this.binder.forField(this.terem)
                .asRequired(this.getTranslation("validacio.hibas"))
                .withConverter(t -> t == null ? null : t.getTeremId(), this.termek::get)
                .bind("teremId");
        this.binder.forField(kezdes).bind("kezdesIdo");
        this.urlap.add(this.film, this.terem, kezdes);

        this.frissit();
    }

    private String filmCim(Long filmId) {
        Film f = this.filmek.get(filmId);
        return f == null ? "" : f.getCim();
    }

    private String teremNev(Long teremId) {
        Terem t = this.termek.get(teremId);
        return t == null ? "" : t.getNev();
    }

    private String foglaltsag(Vetites v) {
        MusorElem m = this.musor.get(v.getVetitesId());
        return m == null ? "" : this.getTranslation("vetites.foglaltsag", m.szabadHelyek(), m.kapacitas());
    }

    @Override
    protected void frissit() {
        this.filmek.clear();
        this.adminService.filmek().forEach(f -> this.filmek.put(f.getFilmId(), f));
        this.termek.clear();
        this.adminService.termek().forEach(t -> this.termek.put(t.getTeremId(), t));
        this.musor.clear();
        this.adminService.musor().forEach(m -> this.musor.put(m.vetitesId(), m));
        this.film.setItems(this.filmek.values());
        this.terem.setItems(this.termek.values());
        super.frissit();
    }

    @Override
    protected List<Vetites> betolt() {
        return this.adminService.vetitesek();
    }

    @Override
    protected void ment(Vetites elem) {
        this.adminService.vetitesMentes(elem);
    }

    @Override
    protected void torol(Vetites elem) {
        this.adminService.vetitesTorles(elem.getVetitesId());
    }

    @Override
    protected Long azonosito(Vetites elem) {
        return elem.getVetitesId();
    }

    @Override
    protected String megnevezes(Vetites elem) {
        return this.filmCim(elem.getFilmId()) + ", " + IDO_FORMATUM.format(elem.getKezdesIdo());
    }
}
