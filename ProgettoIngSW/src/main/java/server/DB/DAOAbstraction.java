package server.DB;

import server.DTOs.GeneralitaUtente;
import server.DTOs.UserData;
import server.entities.Cliente;
import server.entities.Prenotazione;
import server.entities.Promozione;
import server.entities.Volo;
import server.exceptions.FlightAlreadyBookedException;
import server.exceptions.UnauthorizedException;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;

public abstract class DAOAbstraction {
    private DAOImplementor impl;

    public DAOAbstraction(DAOImplementor impl) {
        this.impl = impl;
    }

    public float getCurrentFee(int idVolo, Collection<Integer> seatIds) throws SQLException {
        return impl.getCurrentFee(idVolo, seatIds);
    }

    public float getCancellationFee(int idPrenotazione) throws SQLException {
        return impl.getCancellationFee(idPrenotazione);
    }

    public boolean modifyReservation(int idPrenotazione, String email, Collection<Integer> seatIds) throws SQLException, UnauthorizedException {
        return impl.modifyReservation(idPrenotazione, email, seatIds);
    }

    public List<Volo> getFlights(Timestamp fromD, Timestamp toD, String from, String to) throws SQLException {
        return impl.getFlights(fromD, toD, from, to);
    }

    public int createUser(Cliente DTO) throws SQLException {
        return impl.createUser(DTO);
    }


    List<String[]> getEmailsToNotify() throws SQLException {
        return impl.getEmailsToNotify();
    }

    public int createBooking(Collection<Integer> seatIds, int volo, GeneralitaUtente utente, int idFedelta, float price) throws SQLException, UnauthorizedException, FlightAlreadyBookedException {
        return impl.createBooking(seatIds, volo, utente, idFedelta, price);
    }


    public Prenotazione getPrenotazioneSingola(int userId, String email) throws SQLException, UnauthorizedException {
        return impl.getPrenotazioneSingola(userId, email);
    }

    public boolean checkUserInfo(int userId, String password) throws SQLException {
        return impl.checkUserInfo(userId, password);
    }

    public List<Prenotazione> getPrenotazioni(int userId, String password) throws SQLException, UnauthorizedException {
        return impl.getPrenotazioni(userId, password);
    }

    public UserData displayUserData(int userId, String password) throws SQLException, UnauthorizedException {
        return impl.displayUserData(userId, password);
    }

    public boolean checkIn(int idPrenotazione, String email) throws SQLException, UnauthorizedException {
        return impl.checkIn(idPrenotazione, email);
    }

    List<String> getEmailForExpiringReservations() throws SQLException {
        return impl.getEmailForExpiringReservations();
    }

    public AbstractMap.SimpleEntry<Collection<String>, Collection<String>> getEmailsForPromotions() throws SQLException {
        return impl.getEmailsForPromotions();
    }

    public boolean deleteReservation(int idPrenotazione, String email) throws SQLException, UnauthorizedException {
        return impl.deleteReservation(idPrenotazione, email);
    }

   public List<String> getEmailsNonLoyalCLients() throws SQLException {
        return impl.getEmailsNonLoyalCLients();
    }

    public List<Promozione> getActivePromos() throws SQLException {
        return impl.getActivePromos();
    }

}
