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


public interface DAOImplementor {

    float getCurrentFee(int idVolo, Collection<Integer> seatIds) throws SQLException;

    float getCancellationFee(int idPrenotazione) throws SQLException;

    boolean modifyReservation(int idPrenotazione, String email, Collection<Integer> seatIds) throws SQLException, UnauthorizedException;

    List<Volo> getFlights(Timestamp fromD, Timestamp toD, String from, String to) throws SQLException;

    int createUser(Cliente DTO) throws SQLException;

    List<String[]> getEmailsToNotify() throws SQLException;

    int createBooking(Collection<Integer> seatIds, int volo, GeneralitaUtente utente, int idFedelta,float pricePaid) throws SQLException, UnauthorizedException, FlightAlreadyBookedException;

    Prenotazione getPrenotazioneSingola(int userId, String email) throws SQLException, UnauthorizedException;

    boolean checkUserInfo(int userId, String password) throws SQLException;

    List<Prenotazione> getPrenotazioni(int userId, String password) throws SQLException, UnauthorizedException;

    UserData displayUserData(int userId, String password) throws SQLException, UnauthorizedException;

    boolean checkIn(int idPrenotazione, String email) throws SQLException, UnauthorizedException;

    List<String> getEmailForExpiringReservations() throws SQLException;

    AbstractMap.SimpleEntry<Collection<String>, Collection<String>> getEmailsForPromotions() throws SQLException;

    boolean deleteReservation(int idPrenotazione, String email) throws SQLException, UnauthorizedException;

    List<String> getEmailsNonLoyalCLients() throws SQLException;

    List<Promozione> getActivePromos() throws SQLException;
}
