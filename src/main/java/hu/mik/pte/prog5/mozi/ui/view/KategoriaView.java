package hu.mik.pte.prog5.mozi.ui.view;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import hu.mik.pte.prog5.mozi.model.Kategoria;
import hu.mik.pte.prog5.mozi.service.AdminService;
import hu.mik.pte.prog5.mozi.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "kategoriak", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class KategoriaView extends AdminCrudNezet<Kategoria> {

    private final AdminService adminService;

    public KategoriaView(AdminService adminService) {
        super(Kategoria.class, Kategoria::new, "kategoria.cim");
        this.adminService = adminService;

        this.grid.addColumn(Kategoria::getMegnevezes)
                .setHeader(this.getTranslation("kategoria.megnevezes")).setSortable(true);
        this.auditOszlopok();

        TextField megnevezes = new TextField(this.getTranslation("kategoria.megnevezes"));
        this.binder.forField(megnevezes).bind("megnevezes");
        this.urlap.add(megnevezes);

        this.frissit();
    }

    @Override
    protected List<Kategoria> betolt() {
        return this.adminService.kategoriak();
    }

    @Override
    protected void ment(Kategoria elem) {
        this.adminService.kategoriaMentes(elem);
    }

    @Override
    protected void torol(Kategoria elem) {
        this.adminService.kategoriaTorles(elem.getKategoriaId());
    }

    @Override
    protected Long azonosito(Kategoria elem) {
        return elem.getKategoriaId();
    }

    @Override
    protected String megnevezes(Kategoria elem) {
        return elem.getMegnevezes();
    }
}
