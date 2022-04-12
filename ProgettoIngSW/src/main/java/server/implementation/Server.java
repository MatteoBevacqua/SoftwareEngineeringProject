package server.implementation;

import server.DB.DAOAbstraction;
import server.DB.MySQLDAO;
import server.DB.SQLDao;
import server.DTOs.GeneralitaUtente;
import server.DTOs.UserData;
import server.DTOs.UserInfo;
import server.entities.Cliente;
import server.entities.Prenotazione;
import server.entities.Promozione;
import server.entities.Volo;
import server.exceptions.NoSuchReservationException;
import server.exceptions.UnauthorizedException;
import server.DB.MailHelper;
import server.strategy.Argon2GHashingStrategy;
import server.support.MyOptional;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.awt.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Server implements ServerInterface {
    private final DAOAbstraction database;
    private MailHelper mailHelper;

    public List<Promozione> getPromos() {
        try {
            return database.getActivePromos();
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    public Server(DAOAbstraction dao) {
        this.database = dao;
    }

    public Server(DAOAbstraction daoInterfaceImpl, MailHelper mailHelper) {
        this.database = daoInterfaceImpl;
        this.mailHelper = mailHelper;
    }

    private void sendEmail(String to, String message) {
        if (mailHelper == null)
            return;
        mailHelper.sendEmail(to, message);
    }


    @Override
    public List<Volo> getVoli(Timestamp fromD, Timestamp toD, String from, String to) throws RemoteException {
        try {
            return database.getFlights(fromD, toD, from, to);
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }


    @Override
    public int createReservation(Collection<Integer> seatIds, int volo, GeneralitaUtente utente, int fedeltaId, float p) {
        try {
            int id = database.createBooking(seatIds, volo, utente, fedeltaId, p);
            if (id > 0)
                sendEmail(utente.getEmail(), "Reservation #" + id + " on flight #" + volo + " has been created");
            return id;
        } catch (SQLException e) {
            return -1;
        }
    }


    @Override
    public boolean modifyReservation(int idPrenotazione, String email, Collection<Integer> seatIds) {
        try {
            boolean res = database.modifyReservation(idPrenotazione, email, seatIds);
            if (res) sendEmail(email, "Reservation #" + idPrenotazione + " has been modified");
            return res;
        } catch (SQLException e) {
            return false;
        }
    }


    @Override
    public int createUser(Cliente clienteDTO) {
        try {
            int user = database.createUser(clienteDTO);
            sendEmail(clienteDTO.getEmail(), "Welcome aboard,< .".replaceAll("<", clienteDTO.getNome()));
            return user;
        } catch (SQLException throwables) {
            return -1;
        }

    }


    @Override
    public boolean login(UserInfo info) throws RemoteException {
        try {
            return database.checkUserInfo(info.userId(), info.password());
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<Prenotazione> getPrenotazioni(UserInfo info) throws RemoteException {
        try {
            return database.getPrenotazioni(info.userId(), info.password());
        } catch (SQLException | UnauthorizedException throwables) {
            return Collections.emptyList();
        }
    }

    @Override
    public MyOptional<Prenotazione> getPrenotazioneSingola(int userId, String email) throws RemoteException {
        try {
            return MyOptional.of(database.getPrenotazioneSingola(userId, email));
        } catch (SQLException throwables) {
            return MyOptional.empty();
        }
    }

    @Override
    public float getCurrentFee(int idVolo, Collection<Integer> seatIds) throws RemoteException {
        try {
            return database.getCurrentFee(idVolo, seatIds);
        } catch (SQLException e) {
            return -1;
        }
    }

    @Override
    public float getCancellationFee(int idPrenotazione) throws RemoteException {
        try {
            return database.getCancellationFee(idPrenotazione);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public MyOptional<UserData> displayUserData(UserInfo info) throws RemoteException {
        try {
            return MyOptional.of(database.displayUserData(info.userId(), info.password()));
        } catch (SQLException | UnauthorizedException e) {
            e.printStackTrace();
            return MyOptional.empty();
        }
    }

    @Override
    public boolean checkIn(int idPrenotazione, String email) throws RemoteException {
        try {
            boolean res = database.checkIn(idPrenotazione, email);
            if (res) sendEmail(email, "Check-in completed, booking#" + idPrenotazione);
            return res;
        } catch (SQLException | UnauthorizedException e) {
            return false;
        }
    }

    @Override
    public boolean deleteReservation(int idPrenotazione, String email) throws RemoteException {
        try {
            boolean res = database.deleteReservation(idPrenotazione, email);
            if (res) sendEmail(email, "Reservation #" + idPrenotazione + " has been deleted.");
            return res;
        } catch (SQLException | NoSuchReservationException | UnauthorizedException e) {
            return false;
        }
    }


    public static void main(String... args) throws Exception {
        //String oldURL = "jdbc:mysql://localhost:3306/AIRLINE_RESERVATION_SYSTEM?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        if (args.length < 4)
            throw new RuntimeException("Need a database url,name,password and the location of the server.policy file\nIf you want to generate a DB sample set a 5th argument (can be a random string)");
        final String dbURL = args[0];
        final String name = args[1], password = args[2];
        System.setProperty("java.security.policy", args[3]);
        Registry registry = LocateRegistry.createRegistry(1099);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        //MailHelper helper = new MailHelper(abstraction);
        ServerInterface server = new Server(abstraction, null);
        //metodo che permette di creare una versione di prova del db con dati fasulli per testare il client
        if (args.length>=5) {
            System.out.println("Generating database sample...");
            d.generateDBSample();
            System.out.println("Done,you can start the client now");
        }
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new SecurityManager());
        SslRMIClientSocketFactory c = new SslRMIClientSocketFactory();
        SslRMIServerSocketFactory s = new SslRMIServerSocketFactory();
        //  ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server,2525,c,s);
        ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, 2525);
        Naming.bind("airlineServer", stub);
        System.out.println("Server published");
    }

}
