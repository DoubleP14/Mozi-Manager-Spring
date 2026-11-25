package hu.mik.pte.prog5.mozi.ui.view;

import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import hu.mik.pte.prog5.mozi.model.Terem;
import hu.mik.pte.prog5.mozi.service.AdminService;
import hu.mik.pte.prog5.mozi.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "termek", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class TeremView extends AdminCrudNezet<Terem> {

    private final AdminService adminService;

    public TeremView(AdminService adminService) {
        super(Terem.class, Terem::new, "terem.cim");
        this.adminService = adminService;

        this.grid.addColumn(Terem::getNev).setHeader(this.getTranslation("terem.nev")).setSortable(true);
        this.grid.addColumn(Terem::getKapacitas).setHeader(this.getTranslation("terem.kapacitas")).setSortable(true);
        this.auditOszlopok();

        TextField nev = new TextField(this.getTranslation("terem.nev"));
        IntegerField kapacitas = new IntegerField(this.getTranslation("terem.kapacitas"));
        kapacitas.setMin(1);
        kapacitas.setStepButtonsVisible(true);
        this.binder.forField(nev).bind("nev");
        this.binder.forField(kapacitas).bind("kapacitas");
        this.urlap.add(nev, kapacitas);

        this.frissit();
    }

    @Override
    protected List<Terem> betolt() {
        return this.adminService.termek();
    }

    @Override
    protected void ment(Terem elem) {
        this.adminService.teremMentes(elem);
    }

    @Override
    protected void torol(Terem elem) {
        this.adminService.teremTorles(elem.getTeremId());
    }

    @Override
    protected Long azonosito(Terem elem) {
        return elem.getTeremId();
    }

    @Override
    protected String megnevezes(Terem elem) {
        return elem.getNev();
    }
}
