package server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import server.DB.DAOAbstraction;
import server.DB.DAOImplementor;
import server.DB.MySQLDAO;
import server.DB.SQLDao;
import server.DTOs.GeneralitaUtente;
import server.DTOs.UserData;
import server.DTOs.UserInfo;
import server.entities.Cliente;
import server.exceptions.FlightAlreadyBookedException;
import server.implementation.Server;
import server.implementation.ServerInterface;
import server.strategy.Argon2GHashingStrategy;
import server.support.MyOptional;


import java.rmi.RemoteException;
import java.sql.Date;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/*
  IMPORTANTE
  Prima di effettuare i test bisogna svuotare o creare ex-novo il database con il ddl fornito altrimenti
  i test falliranno tentando di inserire nel db elementi in violazioen di vincoli di chiave primaria
 * Testing principle = white box testing
 * Statement coverage -> target 80%
 * */
class ServerTest {
    final static String dbURL = "jdbc:mysql://localhost:3306/AIRLINE_RESERVATION_SYSTEM?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    final static String name = "root", password = "125896";
    final static String[] cleanUpDatabase = {
            "DELETE FROM  PRENOTAZIONE_NOTIFIED WHERE TRUE;",
            "DELETE FROM  POSTI_PRENOTATI WHERE TRUE;",
            "DELETE FROM  PRENOTAZIONE WHERE TRUE;",
            "DELETE FROM  POSTO WHERE TRUE;",
            "DELETE FROM  VOLO WHERE TRUE",
            "DELETE FROM  AEREO WHERE TRUE",
            "DELETE FROM  CLIENTE WHERE TRUE;",
            "DELETE FROM  CITTA WHERE TRUE;",
            "DELETE FROM  PROMOZIONI WHERE TRUE",
            "DELETE FROM  VOLO_PROMO WHERE TRUE",
            "ALTER TABLE  PROMOZIONI AUTO_INCREMENT = 1;",
            "ALTER TABLE  PRENOTAZIONE AUTO_INCREMENT = 1;",
            "ALTER TABLE  POSTO AUTO_INCREMENT = 1;",
            "ALTER TABLE  VOLO AUTO_INCREMENT = 1;",
            "ALTER TABLE  AEREO AUTO_INCREMENT = 1;",
            "ALTER TABLE  CLIENTE AUTO_INCREMENT = 1;",
            "ALTER TABLE  CITTA AUTO_INCREMENT = 1;"

    };
    final static String[] mockValues = {
            "INSERT INTO CITTA VALUES ('ROMA','ITALIA')",
            "INSERT INTO CITTA VALUES ('MILANO','ITALIA')",
            "INSERT INTO AEREO VALUES (1,'AIRBUS 320',1,FALSE)",
            "INSERT INTO CLIENTE VALUES (1,'MARCO','ROSSI','VIA VERDI 12',TRUE,'TEST1@TEST.COM','0',NULL,CURRENT_DATE())",
            "INSERT INTO VOLO VALUES (1,'MILANO','ROMA',CURRENT_DATE(),1,'MILANO LINATE',150,100,50,'522','3') ;",
            "INSERT INTO VOLO VALUES (2,'ROMA','MILANO',CURRENT_DATE(),1,'MILANO LINATE',150,100,50,'522','3') ;",
            "INSERT INTO PRENOTAZIONE VALUES (1,1,1,3,FALSE,0);"
    };
    final static String insertSeat = "INSERT INTO POSTO values (NULL,1,'FIRST')";


    @org.junit.jupiter.api.BeforeEach
    void setUp() throws SQLException {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        for (String s : cleanUpDatabase)
            statement.executeUpdate(s);
        Statement voloEPosti = connection.createStatement();
        for (String update : mockValues)
            voloEPosti.executeUpdate(update);
        connection.commit();
    }

    @org.junit.jupiter.api.BeforeEach
    void tearDown() throws SQLException {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        for (String s : cleanUpDatabase)
            statement.executeUpdate(s);
        connection.commit();
    }

    @Test
    void testLogin() throws SQLException, RemoteException, ParseException {
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        ServerInterface server = new Server(abstraction);
        Cliente c = new Cliente();
        c.setNome("test");
        c.setCognome("test");
        c.setPassword("testp4ssword");
        DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = sourceFormat.parse("23/11/1999");
        c.setDataNascita(date);
        c.setFedelta(true);
        c.setEmail("testing@testing.com");
        c.setIndirizzo("test");
        int id = server.createUser(c);
        assert server.login(new UserInfo(id, c.getPassword()));
    }

    @Test
    void testLoginFail() throws SQLException, RemoteException, ParseException {
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        ServerInterface server = new Server(abstraction);
        Cliente c = new Cliente();
        c.setNome("test");
        c.setCognome("test");
        c.setPassword("testp4ssword");
        DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = sourceFormat.parse("23/11/1999");
        c.setDataNascita(date);
        c.setFedelta(true);
        c.setEmail("testing@testing.com");
        c.setIndirizzo("test");
        int id = server.createUser(c);
        byte[] newPass;
        String wrongPassword;
        do {
            newPass = new byte[c.getPassword().length()];
            Random random = new Random();
            random.nextBytes(newPass);
            wrongPassword = new String(newPass);
        } while (c.getPassword().equals(wrongPassword));
        assert !server.login(new UserInfo(id, wrongPassword));
    }

    @Test
    void testPrenotazioni() throws SQLException, RemoteException, ParseException {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        for (int i = 0; i < new Random().nextInt(100); i++)
            connection.prepareStatement(insertSeat.replaceAll("\\?", i + "")).executeUpdate();
        float fee = server.getCurrentFee(1, Collections.singletonList(1));
        assert server.createReservation(Collections.singletonList(1), 1, new GeneralitaUtente("test", "test", "test", "test", new Date(Instant.now().toEpochMilli())), -1, fee) > 0;
    }

    @Test
    void testPrenotazioniMultipleRetrieval() throws SQLException, RemoteException, ParseException {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        for (int i = 0; i < new Random().nextInt(25); i++)
            connection.prepareStatement(insertSeat.replaceAll("\\?", i + "")).executeUpdate();
        for (int i = 0; i < new Random().nextInt(25); i++)
            connection.prepareStatement(insertSeat.replaceAll("\\?", i + "")).executeUpdate();
        Cliente c = new Cliente();
        c.setNome("test");
        c.setCognome("test");
        c.setPassword("test");
        DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = sourceFormat.parse("23/11/1999");
        c.setDataNascita(date);
        c.setFedelta(true);
        c.setEmail("testing@testing.com");
        c.setIndirizzo("test");
        int id = server.createUser(c);
        float fee = server.getCurrentFee(1, Collections.singletonList(1));
        server.createReservation(Collections.singletonList(1), 1, new GeneralitaUtente("test", "test", "test", "test", new Date(Instant.now().toEpochMilli())), id, fee);
        fee = server.getCurrentFee(2, Collections.singletonList(1));
        server.createReservation(Collections.singletonList(2), 2, new GeneralitaUtente("test", "test", "test", "test", new Date(Instant.now().toEpochMilli())), id, fee);
        assert server.getPrenotazioni(new UserInfo(id, "test")).size() == 2;
    }

    @Test
    void testPrenotazioniMultipleSingoloVolo() throws Exception {
        Executable test = () -> {
            MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
            DAOAbstraction abstraction = new SQLDao(d);
            Server server = new Server(abstraction);
            server.createReservation(Collections.singletonList(1), 1, new GeneralitaUtente("test", "test", "test", "test", new Date(Instant.now().toEpochMilli())), -1, 0f);
            server.createReservation(List.of(3, 4, 5, 6, 7), 1, new GeneralitaUtente("test", "test", "test", "test", new Date(Instant.now().toEpochMilli())), -1, 0F);
        };
        Assertions.assertThrows(FlightAlreadyBookedException.class, test);
    }


    @Test
    void testPrenotazioneSingolaRetrieval() throws Exception {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        for (int i = 0; i < new Random().nextInt(100); i++)
            connection.prepareStatement(insertSeat.replaceAll("\\?", i + "")).executeUpdate();
        float fee = server.getCurrentFee(1, Collections.singletonList(1));
        int id = server.createReservation(Collections.singletonList(1), 1, new GeneralitaUtente("test", "test", "testMail@mail.com", "test", new Date(Instant.now().toEpochMilli())), -1, fee);
        assert server.getPrenotazioneSingola(id, "testMail@mail.com").get().id() == id;
    }

    @Test
    void testVoliRetrieval() throws SQLException, RemoteException {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        int quanti = new Random().nextInt(150);
        PreparedStatement insertRandomVoli = connection.prepareStatement("INSERT INTO VOLO VALUES (NULL,'MILANO','ROMA',CURRENT_DATE(),1,'MILANO LINATE',150,100,50,'522','3') ;");
        for (int i = 0; i < quanti; i++)
            insertRandomVoli.addBatch();
        insertRandomVoli.executeBatch();
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        ServerInterface server = new Server(abstraction);
        Timestamp aWhileAgo = Timestamp.from(Instant.now().minus(Duration.ofDays(2)));
        Timestamp inABit = Timestamp.from(Instant.now().plus(Duration.ofDays(2)));
        //un volo fa parte dei valori di default
        assert server.getVoli(aWhileAgo, inABit, "MILANO", "ROMA").size() == quanti + 1;
    }

    @Test
    void testRegistration() throws SQLException, RemoteException, ParseException {
        DAOImplementor daoImplementor = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        ServerInterface server = new Server(abstraction);
        Cliente c = new Cliente();
        c.setNome("test");
        c.setCognome("test");
        DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = sourceFormat.parse("23/11/1999");
        c.setPassword("passwordTestingPhase");
        c.setDataNascita(date);
        c.setFedelta(false);
        c.setEmail("test@test.com");
        c.setIndirizzo("test");
        int userID = server.createUser(c);
        MyOptional<UserData> dataOpt = server.displayUserData(new UserInfo(userID, c.getPassword()));
        if (dataOpt.isEmpty()) throw new RuntimeException();
        UserData data = dataOpt.get();
        assert data.cognome().equals(c.getCognome()) && data.nome().equals(c.getCognome()) &&
                data.email().equals(c.getEmail());
    }

    @Test
    void testRegistrationOfExistingUser() throws Exception {
        DAOImplementor daoImplementor = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        ServerInterface server = new Server(abstraction);
        Cliente c = new Cliente();
        c.setNome("test");
        c.setCognome("test");
        DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = sourceFormat.parse("23/11/1999");
        c.setPassword("passwordTestingPhase");
        c.setDataNascita(date);
        c.setFedelta(false);
        c.setEmail("test@test.com");
        c.setIndirizzo("test");
        int userID = server.createUser(c);
        MyOptional<UserData> dataOpt = server.displayUserData(new UserInfo(userID, c.getPassword()));
        if (dataOpt.isEmpty()) throw new RuntimeException();
        //viene tentata la registrazione una seconda volta
        //il sistema deve limitarsi a restituire l'id esistente
        assert server.createUser(c) == userID;
    }

    @Test
    void testCheckIn() throws SQLException, RemoteException, InterruptedException {
        DAOImplementor daoImplementor = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        ServerInterface server = new Server(abstraction);
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        PreparedStatement testFlight = connection.prepareStatement("INSERT INTO VOLO VALUES (3,'MILANO','ROMA',?,1,'MILANO LINATE',150,100,50,'522','3')");
        testFlight.setTimestamp(1, Timestamp.from(Instant.now().plus(Duration.ofDays(2))));
        testFlight.executeUpdate();
        PreparedStatement insertPosti = connection.prepareStatement(insertSeat);
        ArrayList<Integer> posti = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            insertPosti.executeUpdate();
            posti.add(i);
        }
        List<Integer> prenotati = posti.stream().filter(x -> Math.random() < 0.5f).collect(Collectors.toList());
        float price = server.getCurrentFee(1, prenotati);
        int pren = server.createReservation(prenotati, 3, new GeneralitaUtente("test", "test", "test", "test", new java.sql.Date(Instant.now().toEpochMilli())), -1, price);
        assert server.checkIn(pren, "test");
    }

    @Test
    void testModifica() throws SQLException {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        Random random = new Random();
        int availableSeats = random.nextInt(100) + 1;
        ArrayList<Integer> allSeats = new ArrayList<>(availableSeats);
        //si generano i posti sul volo
        for (int i = 1; i < availableSeats; i++) {
            connection.prepareStatement(insertSeat.replaceAll("\\?", i + "")).executeUpdate();
            allSeats.add(i);
        }
        List<Integer> postiDaPrenotare = new ArrayList<>();
        //se ne prende un sottoinsieme casuale
        int postoScelto = -1;
        for (int i = 0; i < random.nextInt(availableSeats - 1) + 1; i++) {
            do {
                postoScelto = random.nextInt(availableSeats - 1) + 1;
            } while (postiDaPrenotare.contains(postoScelto));
            postiDaPrenotare.add(postoScelto);
        }
        //viene effettuata la prenotazione
        float fee = abstraction.getCurrentFee(1, postiDaPrenotare);
        GeneralitaUtente fakeUser = new GeneralitaUtente("test", "test", "test", "test", new Date(Instant.now().toEpochMilli()));
        abstraction.createBooking(postiDaPrenotare, 1, fakeUser, -1, fee);
        //vengono rimossi i posti da prenotare lasciando solamente quelli liberi
        allSeats.removeAll(postiDaPrenotare);
        //viene aggiunta alla prenotazione i posti che erano rimasti liberi,pertanto ci si aspetta che vada a buon fine
        assert server.modifyReservation(1, "TEST1@TEST.COM", allSeats);
    }

    @Test
    void testModificaIllegale() throws SQLException, ParseException {
        Executable test = () -> {
            Connection connection = DriverManager.getConnection(dbURL, name, password);
            MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
            DAOAbstraction abstraction = new SQLDao(d);
            ServerInterface server = new Server(abstraction);
            int availableSeats = new Random().nextInt(100);
            //si generano i posti
            for (int i = 1; i < availableSeats; i++)
                connection.prepareStatement(insertSeat.replaceAll("\\?", i + "")).executeUpdate();
            Set<Integer> generatedSeats = new HashSet<>(), newSeats = new HashSet<>();
            Random random = new Random();
            int i = 1;
            for (; i < availableSeats / 2; i++)
                generatedSeats.add(i);
            for (; i < availableSeats; i++)
                newSeats.add(i);
            GeneralitaUtente fakeUser = new GeneralitaUtente("test", "test", "test", "test", new Date(Instant.now().toEpochMilli()));
            float fee = d.getCurrentFee(1, generatedSeats);
            abstraction.createBooking(generatedSeats, 1, fakeUser, -1, fee);
            Iterator<Integer> iterator = generatedSeats.iterator();
            for (i = 0; i < random.nextInt(generatedSeats.size() / 4 +1) + 1; i++)
                newSeats.add(iterator.next());
            //si aggiungono alla prenotazione da effettuare posti già riservati
            abstraction.modifyReservation(1, "TEST1@TEST.COM", newSeats);
        };
        //sarà l'oggetto server a catturare l'eccezione notificando al client la situazione di errore
        Assertions.assertThrows(SQLException.class, test);
    }

    @Test
    void testDeletion() throws Exception {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        for (int i = 0; i < new Random().nextInt(100); i++)
            connection.prepareStatement(insertSeat.replaceAll("\\?", i + "")).executeUpdate();
        float fee = server.getCurrentFee(1, Collections.singletonList(1));
        int id = server.createReservation(Collections.singletonList(1), 1, new GeneralitaUtente("test", "test", "testmail", "test", new Date(Instant.now().toEpochMilli())), -1, fee);
        assert server.deleteReservation(id, "testmail");
    }

    @Test
    void testDeletionOfNonExistingReservation() throws Exception {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        int id = new Random().nextInt(100);
        //il database è vuoto
        assert !server.deleteReservation(id, "test");
    }

    @Test
    void testNonLoyalClients() throws Exception {
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        PreparedStatement insertVolo = connection.prepareStatement("INSERT INTO VOLO VALUES (4,'MILANO','ROMA',?,1,'MILANO LINATE',150,100,50,'522','3') ;");
        insertVolo.setTimestamp(1, Timestamp.from(Instant.now().minus(Duration.ofDays(3 * 365))));
        insertVolo.executeUpdate();
        Cliente c = new Cliente();
        c.setNome("test");
        c.setCognome("test");
        c.setPassword("testp4ssword");
        DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = sourceFormat.parse("23/11/1999");
        c.setDataNascita(date);
        c.setFedelta(true);
        c.setEmail("expectedemail@testing.com");
        c.setIndirizzo("test");
        int id = server.createUser(c);
        PreparedStatement fakePrenotazione = connection.prepareStatement("INSERT INTO PRENOTAZIONE VALUES (NULL,4,?,0,0,0)");
        fakePrenotazione.setInt(1, id);
        fakePrenotazione.executeUpdate();
        List<String> emails = abstraction.getEmailsNonLoyalCLients();
        assert emails.size() == 1 && emails.get(0).equals(c.getEmail());
    }

    @Test
    void testPromoRetrieval() throws Exception {
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        PreparedStatement promo = connection.prepareStatement("INSERT INTO promozioni values (NULL,FALSE,?,?,0.25,0,'s')");
        Random r = new Random();
        int howMany = r.nextInt(150)+1;
        Timestamp start, end;
        start = Timestamp.from(Instant.now().minus(Duration.ofDays(3)));
        end = Timestamp.from(Instant.now().plus(Duration.ofDays(7)));
        for (int i = 0; i < howMany; i++) {
            promo.setTimestamp(1, start);
            promo.setTimestamp(2, end);
            promo.addBatch();
        }
        promo.executeBatch();
        int valid = howMany;
        //promozioni non ancora valide
        start = Timestamp.from(Instant.now().plus(Duration.ofDays(2)));
        howMany = r.nextInt(20)+1;
        for (int i = 0; i < howMany; i++) {
            promo.setTimestamp(1, start);
            promo.setTimestamp(2, end);
            promo.addBatch();
        }
        promo.executeBatch();
        assert server.getPromos().size() == valid;
    }

    @Test
    void testFlightFee() throws Exception {
        MySQLDAO d = new MySQLDAO(dbURL, name, password, new Argon2GHashingStrategy());
        DAOAbstraction abstraction = new SQLDao(d);
        Server server = new Server(abstraction);
        Connection connection = DriverManager.getConnection(dbURL, name, password);
        PreparedStatement insertAereo = connection.prepareStatement("INSERT INTO aereo VALUES (?,?,?,?)");
        insertAereo.setInt(1, 136);
        insertAereo.setString(2, "BOEING 747");
        insertAereo.setInt(3, 0);
        insertAereo.setBoolean(4, false);
        PreparedStatement insertVolo = connection.prepareStatement("INSERT INTO VOLO VALUES (13,'MILANO','ROMA',CURRENT_DATE(),136,'MILANO LINATE',150,100,50,'522','3')");
        insertAereo.executeUpdate();
        insertVolo.executeUpdate();
        PreparedStatement insertSeats = connection.prepareStatement("INSERT INTO POSTO VALUES (?,?,?)");
        Random r = new Random();
        int[] tariffe = {150, 100, 50};
        List<String> classes = List.of("FIRST", "BUSINESS", "ECONOMY");
        Map<Integer, String> seatAndClass = new HashMap<>();
        int howManySeats = r.nextInt(150);
        String classe;
        for (int i = 1; i <= howManySeats; i++) {
            insertSeats.setInt(1, i);
            insertSeats.setInt(2, 136);
            insertSeats.setString(3, classe = classes.get(r.nextInt(3)));
            seatAndClass.put(i, classe);
            insertSeats.executeUpdate();
        }
        int subsetSize = r.nextInt(howManySeats-1)+1;
        List<Integer> chosenSeats = new ArrayList<>();
        int expectedPrice = 0, chosenSeat = -1;
        for (int i = 1; i <= subsetSize; i++) {
            chosenSeat = r.nextInt(howManySeats-1)+1;
            expectedPrice += tariffe[classes.indexOf(seatAndClass.get(chosenSeat))];
            chosenSeats.add(chosenSeat);
        }
        assert server.getCurrentFee(13, chosenSeats) == expectedPrice;
    }
}