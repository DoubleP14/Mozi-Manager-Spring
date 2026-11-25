package hu.mik.pte.prog5.mozi.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import hu.mik.pte.prog5.mozi.ui.view.VetitesView;
import jakarta.annotation.security.PermitAll;

/**
 * A /admin cim. Belepes utan ide jut mindenki:
 * az ADMIN-t tovabbkuldi a vetitesekhez, az ugyfelnek pedig kiirja, hogy ez nem neki valo.
 */
@Route("")
@PermitAll
public class KezdoView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    private final AuthenticationContext authenticationContext;

    public KezdoView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        this.setSizeFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.add(new H2(this.getTranslation("nincsjog.cim")),
                new Paragraph(this.getTranslation("nincsjog.szoveg")),
                new Button(this.getTranslation("gomb.kijelentkezes"), e -> authenticationContext.logout()));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (this.authenticationContext.hasRole("ADMIN")) {
            event.forwardTo(VetitesView.class);
        }
    }

    @Override
    public String getPageTitle() {
        return this.getTranslation("app.cim");
    }
}
