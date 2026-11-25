package hu.mik.pte.prog5.mozi.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import hu.mik.pte.prog5.mozi.model.nezet.HibaNaploSor;
import hu.mik.pte.prog5.mozi.service.AdminService;
import hu.mik.pte.prog5.mozi.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

/**
 * A hiba_naplo tabla utolso 100 sora. Ide ir a HibaNaploService (pl. foglalt szekre vasarlasnal).
 */
@Route(value = "hibanaplo", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class HibanaploView extends VerticalLayout implements HasDynamicTitle {

    private static final DateTimeFormatter IDO = DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm:ss");

    public HibanaploView(AdminService adminService) {
        Grid<HibaNaploSor> grid = new Grid<>(HibaNaploSor.class, false);
        grid.addColumn(h -> IDO.format(h.idopont())).setHeader(this.getTranslation("hibanaplo.idopont")).setAutoWidth(true);
        grid.addColumn(HibaNaploSor::eljarasNeve).setHeader(this.getTranslation("hibanaplo.eljaras")).setAutoWidth(true);
        grid.addColumn(HibaNaploSor::hibaKod).setHeader(this.getTranslation("hibanaplo.kod")).setAutoWidth(true);
        grid.addColumn(HibaNaploSor::hibaUzenet).setHeader(this.getTranslation("hibanaplo.uzenet")).setFlexGrow(1);
        grid.addColumn(HibaNaploSor::felhasznalo).setHeader(this.getTranslation("hibanaplo.felhasznalo")).setAutoWidth(true);
        grid.setItems(adminService.hibak());
        grid.setEmptyStateText(this.getTranslation("lista.ures"));

        this.setSizeFull();
        this.add(new H2(this.getTranslation("hibanaplo.cim")), grid);
    }

    @Override
    public String getPageTitle() {
        return this.getTranslation("hibanaplo.cim");
    }
}
