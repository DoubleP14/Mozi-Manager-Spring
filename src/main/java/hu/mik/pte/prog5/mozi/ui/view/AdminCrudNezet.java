package hu.mik.pte.prog5.mozi.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.HasDynamicTitle;
import hu.mik.pte.prog5.mozi.model.AuditaltEntitas;
import hu.mik.pte.prog5.mozi.service.UzletiSzabalyException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Kozos os a negy egyszeru admin nezetnek (kategoria, terem, film, vetites).
 * Felul a lista, alatta az urlap. Sorra kattintva szerkesztes, "Uj" gombbal uj elem.
 * A leszarmazott csak a mezoket, az oszlopokat es a service hivasokat adja meg.
 *
 * @param <T> az entitas tipusa
 */
public abstract class AdminCrudNezet<T extends AuditaltEntitas> extends VerticalLayout implements HasDynamicTitle {

    protected static final DateTimeFormatter IDO_FORMATUM = DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm");

    protected final Grid<T> grid;
    protected final BeanValidationBinder<T> binder;
    protected final FormLayout urlap = new FormLayout();

    private final Supplier<T> ujElem;
    private final String cimKulcs;
    private final Button torlesGomb = new Button();
    private final HorizontalLayout urlapGombok;
    private T aktualis;

    protected AdminCrudNezet(Class<T> tipus, Supplier<T> ujElem, String cimKulcs) {
        this.ujElem = ujElem;
        this.cimKulcs = cimKulcs;
        this.grid = new Grid<>(tipus, false);
        // A BeanValidationBinder az entitas annotacioit (@NotBlank, @Min...) is ellenorzi
        this.binder = new BeanValidationBinder<>(tipus);

        this.grid.setEmptyStateText(this.getTranslation("lista.ures"));
        this.grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                this.szerkeszt(e.getValue());
            }
        });

        Button ujGomb = new Button(this.getTranslation("gomb.uj"), e -> {
            this.grid.asSingleSelect().clear();
            this.szerkeszt(this.ujElem.get());
        });
        ujGomb.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button mentesGomb = new Button(this.getTranslation("gomb.mentes"), e -> this.mentes());
        mentesGomb.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.torlesGomb.setText(this.getTranslation("gomb.torles"));
        this.torlesGomb.addThemeVariants(ButtonVariant.LUMO_ERROR);
        this.torlesGomb.addClickListener(e -> this.torlesMegerositessel());
        Button megseGomb = new Button(this.getTranslation("gomb.megse"), e -> this.bezar());
        this.urlapGombok = new HorizontalLayout(mentesGomb, this.torlesGomb, megseGomb);

        HorizontalLayout felso = new HorizontalLayout(new H2(this.getTranslation(cimKulcs)), ujGomb);
        felso.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        this.urlap.setVisible(false);
        this.urlapGombok.setVisible(false);
        this.setSizeFull();
        this.add(felso, this.grid, this.urlap, this.urlapGombok);
    }

    /** A lista adatai a service-bol. */
    protected abstract List<T> betolt();

    protected abstract void ment(T elem);

    protected abstract void torol(T elem);

    /** Az elem azonositoja, uj elemnel null. */
    protected abstract Long azonosito(T elem);

    /** Rovid nev a torles megerositesehez. */
    protected abstract String megnevezes(T elem);

    /** Az audit oszlopok: ki es mikor modositotta utoljara (a trigger es az AuditorAware tolti). */
    protected void auditOszlopok() {
        this.grid.addColumn(e -> e.getModositva() == null ? "" : IDO_FORMATUM.format(e.getModositva()))
                .setHeader(this.getTranslation("audit.modositva")).setAutoWidth(true);
        this.grid.addColumn(AuditaltEntitas::getModositotta)
                .setHeader(this.getTranslation("audit.modositotta")).setAutoWidth(true);
    }

    protected void frissit() {
        this.grid.setItems(this.betolt());
    }

    private void szerkeszt(T elem) {
        this.aktualis = elem;
        this.binder.readBean(elem);
        this.torlesGomb.setEnabled(this.azonosito(elem) != null);
        this.urlap.setVisible(true);
        this.urlapGombok.setVisible(true);
    }

    private void bezar() {
        this.aktualis = null;
        this.grid.asSingleSelect().clear();
        this.urlap.setVisible(false);
        this.urlapGombok.setVisible(false);
    }

    private void mentes() {
        if (!this.binder.writeBeanIfValid(this.aktualis)) {
            this.hiba(this.getTranslation("validacio.hibas"));
            return;
        }
        try {
            this.ment(this.aktualis);
            this.siker(this.getTranslation("mentes.sikeres"));
            this.bezar();
            this.frissit();
        } catch (UzletiSzabalyException e) {
            this.uzletiHiba(e);
        }
    }

    private void torlesMegerositessel() {
        T elem = this.aktualis;
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(this.getTranslation("torles.megerosites.cim"));
        dialog.setText(this.getTranslation("torles.megerosites.szoveg", this.megnevezes(elem)));
        dialog.setCancelable(true);
        dialog.setCancelText(this.getTranslation("gomb.megse"));
        dialog.setConfirmText(this.getTranslation("gomb.torles"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            try {
                this.torol(elem);
                this.siker(this.getTranslation("torles.sikeres"));
                this.bezar();
                this.frissit();
            } catch (UzletiSzabalyException ex) {
                this.uzletiHiba(ex);
            }
        });
        dialog.open();
    }

    // A hibauzenet a kod alapjan a forditasbol jon, igy angolul is megjelenik
    protected void uzletiHiba(UzletiSzabalyException e) {
        this.hiba(this.getTranslation(e.getKod()));
    }

    protected void hiba(String szoveg) {
        Notification.show(szoveg, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    protected void siker(String szoveg) {
        Notification.show(szoveg, 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    @Override
    public String getPageTitle() {
        return this.getTranslation(this.cimKulcs);
    }
}
