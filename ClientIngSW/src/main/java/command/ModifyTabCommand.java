package command;

import pannelli.GetUserDataNoAccount;
import pannelli.ParteGrafica;
import pannelli.PaymentPanel;
import server.DTOs.GeneralitaUtente;
import server.DTOs.SeatDTO;
import server.entities.Prenotazione;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModifyTabCommand implements Command {
    private final Prenotazione prenotazione;
    private final float newPrice;
    private final Set<SeatDTO> seatsToBook;
    private String email;

    public ModifyTabCommand(Prenotazione p, float newPrice, Set<SeatDTO> seatsToBook) {
        this.prenotazione = p;
        this.newPrice = newPrice;
        this.seatsToBook = seatsToBook;
    }

    public ModifyTabCommand(String email, Prenotazione p, float newPrice, Set<SeatDTO> seatsToBook) {
        this.prenotazione = p;
        this.newPrice = newPrice;
        this.seatsToBook = seatsToBook;
        this.email = email;
    }

    @Override
    public void execute() {
        Set<Integer> seatsIDs = seatsToBook.stream().map(SeatDTO::seatId).collect(Collectors.toSet());
        PaymentPanel panel;
        JOptionPane.showConfirmDialog(null, panel = new PaymentPanel(newPrice), "Pay",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (panel.hasCardHasBeenValidated()) {
            boolean out = false;
            if (ParteGrafica.INSTANCE. isUserLoggedIn())
                email = ParteGrafica.INSTANCE.getUserData().email();
            else
                email = ParteGrafica.INSTANCE.getEmail();
            try {
                out = ParteGrafica.INSTANCE. getServerStub().modifyReservation(prenotazione.id(), email, seatsIDs);
                if (out)
                    JOptionPane.showMessageDialog(null, "Modification successful!");
                else JOptionPane.showMessageDialog(null, "Modification failed,retry!");
                ConcreteCommandHandler.INSTANCE.handleCommand(new RefreshReservationsCommand());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
