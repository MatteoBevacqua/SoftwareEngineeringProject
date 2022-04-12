package command;

import client.ErrorHandler;
import exceptions.UnreachableServerException;
import pannelli.GetUserDataNoAccount;
import pannelli.ParteGrafica;
import pannelli.PaymentPanel;
import server.DTOs.GeneralitaUtente;
import server.DTOs.SeatDTO;
import server.exceptions.FlightAlreadyBookedException;
import server.exceptions.UnauthorizedException;

import javax.swing.*;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlaceOrderCommand implements Command {
    private Set<SeatDTO> seats;
    private final int volo;
    private float price;

    public PlaceOrderCommand(Set<SeatDTO> seatsToBook, int volo) {
        this.seats = seatsToBook;
        this.volo = volo;
    }

    public void execute() {
        List<Integer> seatsDTO = seats.stream().map(SeatDTO::seatId).collect(Collectors.toList());
        try {
            price = ParteGrafica.INSTANCE.getServerStub().getCurrentFee(volo, seatsDTO);
        } catch (RemoteException e) {
            ErrorHandler.showErrorMessage("Failed to reach the server,try again in a bit");
            return;
        }
        GetUserDataNoAccount p = new GetUserDataNoAccount(true);
        int fedelta = -1;
        if (!ParteGrafica.INSTANCE.isUserLoggedIn()) { //richiesta generalità dal momento che l'utente non possiede un account
            int option = JOptionPane.showConfirmDialog(null, p, "Insert your information",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) return;
        } else fedelta = ParteGrafica.INSTANCE.getUserId();
        GeneralitaUtente userData = null;
        System.out.println((!ParteGrafica.INSTANCE.isUserLoggedIn() + " " + (p.loyalty() > 0)));
        if (!ParteGrafica.INSTANCE.isUserLoggedIn() && p.loyalty() > 0) fedelta = p.loyalty();
        else if (fedelta < 0) {
            try {
                userData = p.getUserData();
            } catch (ParseException e) {
                ErrorHandler.showErrorMessage("Insert a valid date format DD/MM/YYYY");
                return;
            }
        }
        PaymentPanel panel;
        do {
            int option = JOptionPane.showConfirmDialog(null, panel = new PaymentPanel(price), "Pay",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option == JOptionPane.CANCEL_OPTION) return;
        } while (!panel.hasCardHasBeenValidated());
        int idPrenotazione;
        try {
            idPrenotazione = ParteGrafica.INSTANCE.getServerStub().createReservation(seatsDTO, volo, userData, fedelta, price);
        } catch (UnauthorizedException e) {
            JOptionPane.showMessageDialog(null, "Unathorized");
            return;
        } catch (FlightAlreadyBookedException e) {
            JOptionPane.showMessageDialog(null, "You already made a reservation for this flight");
            return;
        } catch (RemoteException e) {
            throw new UnreachableServerException();
        }
        String textToShow;
        if (idPrenotazione > 0) {
            textToShow = "Reservation placed successfully!\nYour reservation id is : " + idPrenotazione;
            if (ParteGrafica.INSTANCE.isUserLoggedIn())
                ConcreteCommandHandler.INSTANCE.handleCommand(new RefreshReservationsCommand());
        } else
            textToShow = "Looks like someone booked the seats before you,reclick on the flight page to display the updated seats";
        JOptionPane.showMessageDialog(null, textToShow, "Booking attempt outcome", JOptionPane.INFORMATION_MESSAGE);

    }
}
