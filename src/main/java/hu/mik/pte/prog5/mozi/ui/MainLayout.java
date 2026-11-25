package hu.mik.pte.prog5.mozi.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import hu.mik.pte.prog5.mozi.ui.view.FilmView;
import hu.mik.pte.prog5.mozi.ui.view.HibanaploView;
import hu.mik.pte.prog5.mozi.ui.view.KategoriaView;
import hu.mik.pte.prog5.mozi.ui.view.StatisztikaView;
import hu.mik.pte.prog5.mozi.ui.view.TeremView;
import hu.mik.pte.prog5.mozi.ui.view.VetitesView;
import jakarta.annotation.security.PermitAll;

import java.util.Locale;

/**
 * Az admin felulet kerete: fejlec (felhasznalo, nyelv, kijelentkezes) es oldalso menu.
 * A menupontokhoz tartozo nezetek csak ADMIN-nak erhetok el.
 */
@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext authenticationContext) {
        H1 cim = new H1(this.getTranslation("app.cim"));
        cim.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        Span felhasznalo = new Span(this.getTranslation("fejlec.felhasznalo") + " "
                + authenticationContext.getPrincipalName().orElse(""));

        ComboBox<Locale> nyelv = new ComboBox<>();
        nyelv.setPlaceholder(this.getTranslation("fejlec.nyelv"));
        nyelv.setItems(Locale.of("hu"), Locale.ENGLISH);
        nyelv.setItemLabelGenerator(l -> l.getDisplayLanguage(l));
        nyelv.setValue(Locale.of(UI.getCurrent().getLocale().getLanguage()));
        nyelv.setWidth("9em");
        nyelv.addValueChangeListener(e -> {
            if (e.getValue() != null && e.isFromClient()) {
                VaadinSession.getCurrent().setLocale(e.getValue());
                UI.getCurrent().getPage().reload();
            }
        });

        Button kijelentkezes = new Button(this.getTranslation("gomb.kijelentkezes"), e -> authenticationContext.logout());

        HorizontalLayout fejlec = new HorizontalLayout(cim, felhasznalo, nyelv, kijelentkezes);
        fejlec.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        fejlec.expand(cim);
        // A menugomb melletti maradek helyet tolti ki. (100% szelesseggel a menugomb miatt kilogna.)
        fejlec.getStyle().set("flex", "1").set("min-width", "0").set("padding", "0 var(--lumo-space-m)");
        felhasznalo.getStyle().set("white-space", "nowrap").set("overflow", "hidden").set("text-overflow", "ellipsis");

        SideNav menu = new SideNav();
        menu.addItem(new SideNavItem(this.getTranslation("menu.vetitesek"), VetitesView.class));
        menu.addItem(new SideNavItem(this.getTranslation("menu.filmek"), FilmView.class));
        menu.addItem(new SideNavItem(this.getTranslation("menu.termek"), TeremView.class));
        menu.addItem(new SideNavItem(this.getTranslation("menu.kategoriak"), KategoriaView.class));
        menu.addItem(new SideNavItem(this.getTranslation("menu.statisztika"), StatisztikaView.class));
        menu.addItem(new SideNavItem(this.getTranslation("menu.hibanaplo"), HibanaploView.class));

        this.addToNavbar(new DrawerToggle(), fejlec);
        this.addToDrawer(menu);
    }
}
