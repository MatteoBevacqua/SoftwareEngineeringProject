package command;

import pannelli.MyReservationPanel;
import pannelli.ParteGrafica;
import server.DTOs.UserData;
import server.entities.Prenotazione;
import server.support.MyOptional;

import java.awt.*;
import java.util.List;
import java.util.Optional;

public class RefreshReservationsCommand implements Command {


    @Override
    public void execute() {
        try {
            if (!ParteGrafica.INSTANCE.isUserLoggedIn()) return;
            //refresh dei dati utente nel caso sia cambiato il punteggio cumulativo dei punti
            MyOptional<UserData> userData = ParteGrafica.INSTANCE.getServerStub().displayUserData(ParteGrafica.INSTANCE.getAuthInfo());
            if (userData.isEmpty()) return;
            ParteGrafica.INSTANCE.setUserData(userData.get());
            Optional<Component> toRemove = ParteGrafica.INSTANCE.getActiveTabs().stream().filter(tab -> tab.getClass().equals(MyReservationPanel.class)).findFirst();
            if (toRemove.isEmpty()) return;
            Component target = toRemove.get();
            List<Prenotazione> prenotazioni = ParteGrafica.INSTANCE.getServerStub().getPrenotazioni(ParteGrafica.INSTANCE.getAuthInfo());
            ParteGrafica.INSTANCE.removeTab(target);
            ParteGrafica.INSTANCE.addTab("My Reservations", new MyReservationPanel(prenotazioni));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
