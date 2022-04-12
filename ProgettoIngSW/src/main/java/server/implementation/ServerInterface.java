package server.implementation;

import server.DTOs.GeneralitaUtente;
import server.DTOs.UserData;
import server.DTOs.UserInfo;
import server.entities.Cliente;
import server.entities.Prenotazione;
import server.entities.Promozione;
import server.entities.Volo;
import server.exceptions.FlightAlreadyBookedException;
import server.support.MyOptional;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServerInterface extends Remote {

    List<Volo> getVoli(Timestamp fromD, Timestamp toD, String from, String to) throws RemoteException;

    int createReservation(Collection<Integer> seatIds, int volo, GeneralitaUtente utente, int fedeltaId,float pricePaid) throws RemoteException, FlightAlreadyBookedException;

    boolean modifyReservation(int idPrenotazione, String email, Collection<Integer> seatIds) throws RemoteException;

    int createUser(Cliente clienteDTO) throws RemoteException;

    boolean login(UserInfo info) throws RemoteException;

    //riservati a clienti fedeltà
    List<Prenotazione> getPrenotazioni(UserInfo info) throws RemoteException;

    MyOptional<Prenotazione> getPrenotazioneSingola(int userId, String email) throws RemoteException;

    float getCurrentFee(int idVolo, Collection<Integer> seatIds) throws RemoteException;

    float getCancellationFee(int idPrenotazione) throws RemoteException;


    MyOptional<UserData> displayUserData(UserInfo info) throws RemoteException;

    boolean checkIn(int idPrenotazione, String email) throws RemoteException;

    boolean deleteReservation(int idPrenotazione, String email) throws RemoteException;

    List<Promozione> getPromos() throws RemoteException;
}