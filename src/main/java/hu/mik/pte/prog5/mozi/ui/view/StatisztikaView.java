package hu.mik.pte.prog5.mozi.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import hu.mik.pte.prog5.mozi.model.nezet.BevetelSor;
import hu.mik.pte.prog5.mozi.service.AdminService;
import hu.mik.pte.prog5.mozi.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.text.NumberFormat;
import java.util.List;

/**
 * Bevetel filmenkent: a v_bevetel_filmenkent nezet (az Adatbazis 2-es projektbol).
 */
@Route(value = "statisztika", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class StatisztikaView extends VerticalLayout implements HasDynamicTitle {

    public StatisztikaView(AdminService adminService) {
        List<BevetelSor> sorok = adminService.bevetelek();
        NumberFormat szamFormatum = NumberFormat.getIntegerInstance(this.getLocale());

        Grid<BevetelSor> grid = new Grid<>(BevetelSor.class, false);
        grid.addColumn(BevetelSor::cim).setHeader(this.getTranslation("statisztika.film")).setSortable(true);
        grid.addColumn(BevetelSor::eladottJegyek).setHeader(this.getTranslation("statisztika.jegyek"))
                .setComparator(BevetelSor::eladottJegyek).setSortable(true);
        grid.addColumn(new NumberRenderer<>(BevetelSor::osszesBevetel, szamFormatum))
                .setHeader(this.getTranslation("statisztika.bevetel"))
                .setComparator(BevetelSor::osszesBevetel).setSortable(true);
        grid.setItems(sorok);
        grid.setEmptyStateText(this.getTranslation("lista.ures"));

        long osszBevetel = sorok.stream().mapToLong(BevetelSor::osszesBevetel).sum();
        long osszJegy = sorok.stream().mapToLong(BevetelSor::eladottJegyek).sum();
        Paragraph osszesen = new Paragraph(this.getTranslation("statisztika.osszesen",
                szamFormatum.format(osszBevetel), osszJegy));
        osszesen.getStyle().set("font-weight", "bold");

        this.setSizeFull();
        this.add(new H2(this.getTranslation("statisztika.cim")), osszesen, grid);
    }

    @Override
    public String getPageTitle() {
        return this.getTranslation("statisztika.cim");
    }
}
