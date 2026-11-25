package hu.mik.pte.prog5.mozi.ui;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Belepo oldal: /admin/login (a vaadin.url-mapping miatt a /admin ele kerul).
 * Ugyanaz, mint a ZH-kban, csak a felhasznalonev itt az e-mail cim.
 */
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        // A form a /admin/login cimre kuldi az adatokat, azt a Spring Security dolgozza fel
        this.loginForm.setAction("login");
        this.loginForm.setForgotPasswordButtonVisible(false);
        this.loginForm.setI18n(this.forditas());

        this.setSizeFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.add(this.loginForm);
    }

    private LoginI18n forditas() {
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle(this.getTranslation("login.leiras"));
        i18n.getForm().setUsername(this.getTranslation("login.felhasznalonev"));
        i18n.getForm().setPassword(this.getTranslation("login.jelszo"));
        i18n.getForm().setSubmit(this.getTranslation("login.gomb"));
        i18n.getErrorMessage().setTitle(this.getTranslation("login.hiba.cim"));
        i18n.getErrorMessage().setMessage(this.getTranslation("login.hiba.szoveg"));
        return i18n;
    }

    // Rossz jelszo utan a Spring Security a /admin/login?error cimre iranyit vissza
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            this.loginForm.setError(true);
        }
    }

    @Override
    public String getPageTitle() {
        return this.getTranslation("login.cim");
    }
}
