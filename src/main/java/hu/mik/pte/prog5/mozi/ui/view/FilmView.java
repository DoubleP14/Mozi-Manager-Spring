package hu.mik.pte.prog5.mozi.ui.view;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import hu.mik.pte.prog5.mozi.model.Film;
import hu.mik.pte.prog5.mozi.model.Kategoria;
import hu.mik.pte.prog5.mozi.service.AdminService;
import hu.mik.pte.prog5.mozi.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "filmek", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class FilmView extends AdminCrudNezet<Film> {

    private final AdminService adminService;
    // kategoria_id -> kategoria; a listaban es a legordulo menuben ebbol jon a nev
    private final Map<Long, Kategoria> kategoriak = new HashMap<>();
    private final ComboBox<Kategoria> kategoria = new ComboBox<>();

    public FilmView(AdminService adminService) {
        super(Film.class, Film::new, "film.cim");
        this.adminService = adminService;

        this.grid.addColumn(Film::getCim).setHeader(this.getTranslation("film.filmcim")).setSortable(true);
        this.grid.addColumn(Film::getHosszPerc).setHeader(this.getTranslation("film.hossz")).setSortable(true);
        this.grid.addColumn(f -> this.kategoriaNev(f.getKategoriaId()))
                .setHeader(this.getTranslation("film.kategoria")).setSortable(true);
        this.auditOszlopok();

        TextField cim = new TextField(this.getTranslation("film.filmcim"));
        IntegerField hossz = new IntegerField(this.getTranslation("film.hossz"));
        hossz.setMin(1);
        this.kategoria.setLabel(this.getTranslation("film.kategoria"));
        this.kategoria.setItemLabelGenerator(Kategoria::getMegnevezes);

        this.binder.forField(cim).bind("cim");
        this.binder.forField(hossz).bind("hosszPerc");
        // A film csak a kategoria azonositojat tarolja, a menu viszont Kategoria objektumokat mutat:
        // a converter alakit at a ketto kozott
        this.binder.forField(this.kategoria)
                .asRequired(this.getTranslation("validacio.hibas"))
                .withConverter(k -> k == null ? null : k.getKategoriaId(), this.kategoriak::get)
                .bind("kategoriaId");
        this.urlap.add(cim, hossz, this.kategoria);

        this.frissit();
    }

    private String kategoriaNev(Long kategoriaId) {
        Kategoria k = this.kategoriak.get(kategoriaId);
        return k == null ? "" : k.getMegnevezes();
    }

    // A kategoriak is valtozhattak, ezert a filmekkel egyutt ujratoltjuk oket
    @Override
    protected void frissit() {
        List<Kategoria> lista = this.adminService.kategoriak();
        this.kategoriak.clear();
        lista.forEach(k -> this.kategoriak.put(k.getKategoriaId(), k));
        this.kategoria.setItems(lista);
        super.frissit();
    }

    @Override
    protected List<Film> betolt() {
        return this.adminService.filmek();
    }

    @Override
    protected void ment(Film elem) {
        this.adminService.filmMentes(elem);
    }

    @Override
    protected void torol(Film elem) {
        this.adminService.filmTorles(elem.getFilmId());
    }

    @Override
    protected Long azonosito(Film elem) {
        return elem.getFilmId();
    }

    @Override
    protected String megnevezes(Film elem) {
        return elem.getCim();
    }
}
