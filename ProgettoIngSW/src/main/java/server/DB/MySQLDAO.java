package server.DB;


import server.DTOs.GeneralitaUtente;
import server.DTOs.SeatDTO;
import server.DTOs.UserData;
import server.entities.Cliente;
import server.entities.Prenotazione;
import server.entities.Promozione;
import server.entities.Volo;
import server.exceptions.FlightAlreadyBookedException;
import server.exceptions.NoSuchFlightException;
import server.exceptions.NoSuchReservationException;
import server.exceptions.UnauthorizedException;
import server.strategy.PasswordHashingStrategy;

import java.sql.Date;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

//platform-specific DAO
public class MySQLDAO extends AbstractDAOImpl {
    private final String dbURL /*= "jdbc:mysql://localhost:3306/AIRLINE_RESERVATION_SYSTEM?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"*/;
    private final static String getSeatOnFlight = "SELECT ID,CLASSE,(SELECT DISTINCT POSTI.ID_PRENOTAZIONE FROM POSTI_PRENOTATI POSTI  WHERE POSTI.POSTO_PRENOTATO=ID AND POSTI.VOLO=?) FROM POSTO WHERE POSTO.AEREO=(SELECT V.AEREO FROM VOLO V WHERE V.ID=?) ORDER BY POSTO.ID";
    private final static String getMySeatsOnFlight = "SELECT ID,CLASSE FROM POSTO,POSTI_PRENOTATI P WHERE P.ID_PRENOTAZIONE=? AND POSTO.ID=P.POSTO_PRENOTATO  ORDER BY POSTO.ID";

    private final static String getBookingsToNotify = "SELECT C.EMAIL,V.ID FROM PRENOTAZIONE AS P,CLIENTE C,PRENOTAZIONE_NOTIFIED PN,VOLO V WHERE PN.CLIENTS_HAVE_BEEN_NOTIFIED=FALSE AND P.ID=PN.ID AND DATEDIFF(V.DATA_PARTENZA,current_date)=3 FOR UPDATE";
    private final static String createUser = "INSERT INTO CLIENTE VALUES (?,?,?,?,?,?,?,?,?)";
    private final static String getAllFlights = "SELECT V.ID FROM VOLO V WHERE V.PARTENZA = ? AND V.DESTINAZIONE = ? AND V.DATA_PARTENZA BETWEEN ? AND ?;";
    private final static String getFlightByPrenotazione = "SELECT * FROM VOLO V,PRENOTAZIONE P WHERE P.VOLO = V.ID ";
    private final static String checkUserCredentialsID = "SELECT C.PASSWORD_HASH FROM CLIENTE C WHERE C.ID=?";
    private final static String getUserBookings = "SELECT P.ID FROM PRENOTAZIONE P,CLIENTE C WHERE C.ID=? AND P.CLIENTE=C.ID AND P.VOLO=?";
    private final static String getUserBookingsALL = "SELECT * FROM PRENOTAZIONE P,CLIENTE C WHERE C.ID=? AND P.CLIENTE=C.ID";
    private final static String creaPrenotazione = "INSERT INTO PRENOTAZIONE VALUES (?,?,?,?,?,?) ;";
    private final static String BOOK_SEATS = "INSERT INTO POSTI_PRENOTATI VALUES (?,?,?,?)";
    private final static String addMilesToUser = "UPDATE CLIENTE C SET C.PUNTI_ACCUMULATI=(SELECT SUM(P.PUNTI_GUADAGNATI) FROM PRENOTAZIONE P WHERE P.CLIENTE=C.ID) WHERE C.ID=?";
    private final static String getFlight = "SELECT V.MIGLIA FROM VOLO V WHERE V.ID=?";
    private final static String getTariffeVoloDaPrenotazione = "SELECT V.TARIFFA_FIRST,V.TARIFFA_BUSINESS,V.TARIFFA_ECONOMY FROM VOLO V,PRENOTAZIONE P  WHERE P.ID=? AND V.ID=P.VOLO";
    private final static Map<String, Integer> classes = Map.of("FIRST", 0, "BUSINESS", 1, "ECONOMY", 2);
    private final String user, password;

    public MySQLDAO(String dbURL, String user, String password, PasswordHashingStrategy psw) throws SQLException {
        super(psw);
        this.dbURL = dbURL;
        this.user = user;
        this.password = password;
    }

    private Cliente checkIfClienteExistsByMail(String email) throws SQLException {
        Connection connection = getConnection();
        String query = "SELECT * FROM CLIENTE C WHERE C.EMAIL=?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, email);
        ResultSet cliente = preparedStatement.executeQuery();
        if (!cliente.next()) return null;
        return Cliente.fromResultSet(cliente);
    }

    @Override
    public int createBooking(Collection<Integer> seatIds, int volo, GeneralitaUtente utente, int idFedelta, float pricePaid) throws SQLException {
        Connection connection = getConnection();
        int idUtente = idFedelta;
        if (idUtente <= 0) {
            Cliente c;
            if ((c = checkIfClienteExistsByMail(utente.getEmail())) != null) {
                idUtente = c.getId();

            } else {
                c = new Cliente();
                c.setFedelta(false);
                c.setNome(utente.getNome());
                c.setEmail(utente.getEmail());
                c.setIndirizzo(utente.getIndirizzo());
                c.setDataNascita(utente.getDataNascita());
                c.setCognome(utente.getCognome());
                idUtente = createUser(c);
            }
        } else {
            String checkData = "SELECT C.ID FROM CLIENTE C WHERE C.ID=?";
            PreparedStatement preparedStatement = connection.prepareStatement(checkData);
            preparedStatement.setInt(1, idFedelta);
            ResultSet check = preparedStatement.executeQuery();
            if (!check.next()) throw new UnauthorizedException();
        }
        PreparedStatement getF = connection.prepareStatement(getFlight);
        getF.setInt(1, volo);
        ResultSet voloInfo = getF.executeQuery();
        voloInfo.next();
        PreparedStatement check = connection.prepareStatement(getUserBookings);
        check.setInt(1, idUtente);
        check.setInt(2, volo);
        if (check.executeQuery().next()) throw new FlightAlreadyBookedException();
        // crea prenotazione -> aggiorna il posto
        PreparedStatement creaBooking = connection.prepareStatement(creaPrenotazione, Statement.RETURN_GENERATED_KEYS);
        creaBooking.setString(1, null);
        creaBooking.setInt(2, volo);
        creaBooking.setInt(3, idUtente);
        creaBooking.setInt(4, voloInfo.getInt(1));
        creaBooking.setBoolean(5, false);
        creaBooking.setFloat(6, pricePaid);
        creaBooking.executeUpdate();
        ResultSet rs = creaBooking.getGeneratedKeys();
        rs.next();
        int idPrenotazione = rs.getInt(1);
        PreparedStatement updateStatus = connection.prepareStatement(BOOK_SEATS);
        for (Integer seat : seatIds) {
            updateStatus.setInt(1, idPrenotazione);
            updateStatus.setInt(2, seat);
            updateStatus.setInt(3, idUtente);
            updateStatus.setInt(4, volo);
            updateStatus.addBatch();
        }
        updateStatus.executeBatch();
        if (idFedelta > 0) {            //se è fedeltà i punti vengono aggiunti
            PreparedStatement updateUser = connection.prepareStatement(addMilesToUser, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            updateUser.setInt(1, idUtente);
            updateUser.executeUpdate();
        }
        return idPrenotazione;
    }

    public void generateDBSample() throws Exception {
        Connection connection = getConnection();
        final String[] cities = new String[]{"MILANO", "ROMA", "NAPOLI", "AMSTERDAM", "MADRID", "PARIS"};
        final String[] airports = new String[]{"MILANO LINATE", "ROMA FIUMICINO", "NAPOLI", "AMSTERDAM", "MADRID", "PARIS"};
        final String[] aerei = {"BOEING 747", "AIRBUS A320"};
        final String[] classi = {"FIRST", "BUSINESS", "ECONOMY"};
        final String insertAerei = "INSERT INTO AEREO VALUES (?,?,?,?)";
        final String insertPosti = "INSERT INTO POSTO VALUES (?,?,?)";
        Random r = new Random();
        int nAerei = 40;
        PreparedStatement aereiIn = connection.prepareStatement(insertAerei);
        for (int i = 0; i < 40; i++) {
            aereiIn.setString(1, null);
            aereiIn.setString(2, Math.random() < 0.5 ? aerei[0] : aerei[1]);
            aereiIn.setInt(3, new Random().nextInt(10000));
            aereiIn.setBoolean(4, Math.random() < 0.5);
            aereiIn.addBatch();
        }
        aereiIn.executeBatch();
        PreparedStatement postiInsert = connection.prepareStatement(insertPosti);
        for (int i = 1; i <= nAerei; i++) {
            for (int j = 1; j < 100; j++) {
                postiInsert.setInt(1, j);
                postiInsert.setInt(2, i);
                postiInsert.setString(3, classi[r.nextInt(2)]);
                postiInsert.addBatch();
            }
        }
        postiInsert.executeLargeBatch();
        final String inci = "INSERT INTO CITTA VALUES (?,?)";
        PreparedStatement citta = connection.prepareStatement(inci);
        for (String city : cities) {
            citta.setString(1, city);
            citta.setString(2, null);
            citta.executeUpdate();
        }
        final String voli = "INSERT INTO VOLO VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement voliIn = connection.prepareStatement(voli);
        float[] tariffe = {50, 100, 250};
        int nVoli = 0;
        for (String city : cities) {
            for (int i = 0; i < cities.length; i++) {
                if (cities[i].equals(city)) continue;
                voliIn.setString(1, null);
                voliIn.setString(2, city);
                voliIn.setString(3, cities[i]);
                voliIn.setTimestamp(4, new Timestamp(Instant.now().toEpochMilli()));
                voliIn.setString(6, airports[i]);
                voliIn.setInt(5, r.nextInt(nAerei) + 1); //aereo
                //tariffe
                voliIn.setFloat(7, tariffe[2]);
                voliIn.setFloat(8, tariffe[1]);
                voliIn.setFloat(9, tariffe[0]);
                voliIn.setInt(10, new Random().nextInt(515));
                voliIn.setInt(11, new Random().nextInt(15));
                nVoli++;
                voliIn.addBatch();
            }
        }
        voliIn.executeBatch();
        int nPromo = r.nextInt(100);
        PreparedStatement statement = connection.prepareStatement("INSERT INTO PROMOZIONI VALUES (NULL,?,?,?,?,?,?)");
        PreparedStatement insertFlightToo = connection.prepareStatement("INSERT INTO volo_promo VALUES (?,?)");
        Timestamp yesterday = Timestamp.from(Instant.now().minus(Duration.ofDays(1)));
        Timestamp inAWeek = Timestamp.from(Instant.now().plus(Duration.ofDays(7)));
        for (int i = 1; i <= nPromo; i++) {
            statement.setBoolean(1, Math.random() < 0.5);
            statement.setTimestamp(2, yesterday);
            statement.setTimestamp(3, inAWeek);
            statement.setFloat(4, (float) (Math.random() * (0.7 - 0.1) + 0.05));
            statement.setBoolean(5, false);
            statement.setString(6, "Promo");
            statement.addBatch();
            if (Math.random() < 0.5f) {
                insertFlightToo.setInt(1, i);
                insertFlightToo.setInt(2, r.nextInt(nVoli-1)+1);
                insertFlightToo.addBatch();
            }

        }
        statement.executeBatch();
        insertFlightToo.executeBatch();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbURL, user, password);
    }


    @Override
    public List<Prenotazione> getPrenotazioni(int userId, String password) throws SQLException {
        if (!checkUserInfo(userId, password)) throw new UnauthorizedException();
        List<Prenotazione> prenotazioni = new ArrayList<>();
        Connection connection = getConnection();
        PreparedStatement check = connection.prepareStatement(getUserBookingsALL);
        check.setInt(1, userId);
        ResultSet bookings = check.executeQuery();
        HashSet<Integer> bookingsId = new HashSet<>();
        float[] tariffe = new float[3];
        PreparedStatement tariffeStatement = connection.prepareStatement(getTariffeVoloDaPrenotazione);
        ResultSet tariffeResultSet;
        int currentBooking = -1, currentFlight = -1;
        while (bookings.next()) {
            bookingsId.add(currentBooking = bookings.getInt(1));
            tariffeStatement.setInt(1, currentBooking);
            PreparedStatement getSeats = connection.prepareStatement(getSeatOnFlight);
            PreparedStatement getMySeats = connection.prepareStatement(getMySeatsOnFlight);
            getMySeats.setInt(1, currentBooking);
            ResultSet mySeatsSet = getMySeats.executeQuery();
            tariffeResultSet = tariffeStatement.executeQuery();
            tariffeResultSet.first();
            tariffe = new float[]{tariffeResultSet.getFloat(1), tariffeResultSet.getFloat(1), tariffeResultSet.getFloat(1)};
            HashSet<SeatDTO> seats = new HashSet<>(100), mySeats = new HashSet<>(100);
            while (mySeatsSet.next())
                seats.add(new SeatDTO(mySeatsSet.getInt(1),
                        true
                        , true
                        , tariffe[classes.get(mySeatsSet.getString(2))]));
            getSeats.setInt(1, currentFlight = bookings.getInt(2));
            getSeats.setInt(2, currentFlight);
            ResultSet seatsR = getSeats.executeQuery();
            boolean mine = false;
            while (seatsR.next()) {
                SeatDTO s = (new SeatDTO(seatsR.getInt(1), seatsR.getString(3) != null,
                        mine = seatsR.getString(3) != null && bookingsId.contains(seatsR.getInt(3)), tariffe[classes.get(seatsR.getString(2))]));
                if (mine)
                    mySeats.add(s);
                else seats.add(s);
            }
            PreparedStatement preparedStatement = connection.prepareStatement(getFlightByPrenotazione);
            ResultSet volo = preparedStatement.executeQuery();
            volo.next();
            Volo tempVolo = Volo.fromResultSet(volo);
            boolean canCheckIn = Period.between(LocalDate.now(), tempVolo.getDataPartenza().toLocalDateTime().toLocalDate()).getDays() > 3;
            prenotazioni.add(new Prenotazione(bookings.getInt(1), tempVolo, seats, mySeats, bookings.getInt(4), canCheckIn));
        }
        return prenotazioni;
    }

    @Override
    public UserData displayUserData(int userId, String password) throws SQLException {
        if (!checkUserInfo(userId, password)) throw new UnauthorizedException();
        Connection connection = getConnection();
        String getUser = "SELECT C.ID,C.NOME,C.COGNOME,C.EMAIL,C.PUNTI_ACCUMULATI,C.FEDELTA,C.DATA_NASCITA FROM CLIENTE C WHERE C.ID=?";
        PreparedStatement getUserD = connection.prepareStatement(getUser);
        getUserD.setInt(1, userId);
        ResultSet data = getUserD.executeQuery();
        data.next();
        return new UserData(
                data.getInt(1),
                data.getString(2),
                data.getString(3),
                data.getString(4),
                data.getInt(5),
                data.getBoolean(6),
                data.getString(7)
        );

    }

    @Override
    public boolean checkIn(int idPrenotazione, String email) throws SQLException, UnauthorizedException {
        Connection connection = getConnection();
        String checkIfAllowed = "SELECT V.DATA_PARTENZA FROM PRENOTAZIONE P,VOLO V WHERE V.ID=P.VOLO AND P.ID=?";
        String checkIn = "UPDATE  PRENOTAZIONE P SET P.CHECK_IN_EFFETTUATO=TRUE WHERE P.ID=?";
        PreparedStatement checkIfAllowedQ = connection.prepareStatement(checkIfAllowed);
        checkIfAllowedQ.setInt(1, idPrenotazione);
        PreparedStatement proceed = connection.prepareStatement(checkIn);
        ResultSet pren = checkIfAllowedQ.executeQuery();
        proceed.setInt(1, idPrenotazione);
        if (!pren.next()) return false;
        Date date = pren.getDate(1);
        //Check in non permesso prima di 3 giorni
        if (Period.between(LocalDate.now(), date.toLocalDate()).getDays() > 3) return false;
        proceed.executeUpdate();
        return true;

    }

    @Override
    public List<String> getEmailForExpiringReservations() throws SQLException {
        Connection connection = getConnection();
        String getMails = "SELECT C.EMAIL FROM PRENOTAZIONE P,CLIENTE C,VOLO V WHERE P.VOLO=V.ID AND C.ID=P.CLIENTE AND P.CHECK_IN_EFFETTUATO=FALSE AND TIMESTAMPDIFF(hour, V.DATA_PARTENZA, now()) = 24";
        PreparedStatement checkIfAllowedQ = connection.prepareStatement(getMails);
        ResultSet set = checkIfAllowedQ.executeQuery();
        List<String> emails = new ArrayList<>();
        while (set.next())
            emails.add(set.getString(1));
        return emails;
    }

    //restuisce
    @Override
    public AbstractMap.SimpleEntry<Collection<String>, Collection<String>> getEmailsForPromotions() throws SQLException {
        Connection connection = getConnection();
        String getPromo = "SELECT P.SUMMARY FROM PROMOZIONI P  WHERE P.EMAIL_SENT=FALSE FOR UPDATE;";
        String updatePromo = "UPDATE PROMOZIONI P SET P.EMAIL_SENT=TRUE";
        PreparedStatement updatePromoQuery = connection.prepareStatement(updatePromo);
        PreparedStatement promoQuery = connection.prepareStatement(getPromo);
        String getMails = "SELECT C.EMAIL FROM CLIENTE C WHERE C.FEDELTA=TRUE";
        PreparedStatement checkIfAllowedQ = connection.prepareStatement(getMails);
        ResultSet promoSet = promoQuery.executeQuery();
        ResultSet mailSet = checkIfAllowedQ.executeQuery();
        List<String> emails = new ArrayList<>(), promoTexts = new ArrayList<>();
        while (promoSet.next())
            promoTexts.add(promoSet.getString(1));
        while (mailSet.next())
            emails.add(mailSet.getString(1));
        return new AbstractMap.SimpleEntry<>(emails, promoTexts);
    }

    @Override
    public boolean deleteReservation(int idPrenotazione, String email) throws SQLException, UnauthorizedException {
        Connection connection = getConnection();
        PreparedStatement check = connection.prepareStatement("SELECT * FROM prenotazione P WHERE P.ID =? ");
        check.setInt(1,idPrenotazione);
        if (!check.executeQuery().next()) throw new NoSuchReservationException();
        String updateUser =
                "UPDATE CLIENTE C,PRENOTAZIONE P2 SET C.PUNTI_ACCUMULATI=(C.PUNTI_ACCUMULATI-(SELECT P.PUNTI_GUADAGNATI FROM PRENOTAZIONE P WHERE P.ID=? )) WHERE C.ID=P2.VOLO AND P2.ID=? AND C.EMAIL=?;";
        String deletePren = "DELETE FROM PRENOTAZIONE P WHERE P.ID =?;";
        String updateSeats = "DELETE FROM POSTI_PRENOTATI P WHERE  P.ID_PRENOTAZIONE=? ";
        PreparedStatement updateUserQ = connection.prepareStatement(updateUser);
        updateUserQ.setInt(1, idPrenotazione);
        updateUserQ.setInt(2, idPrenotazione);
        updateUserQ.setString(3, email);
        PreparedStatement delete = connection.prepareStatement(deletePren);
        delete.setInt(1, idPrenotazione);
        PreparedStatement updateSeatsQ = connection.prepareStatement(updateSeats);
        updateSeatsQ.setInt(1, idPrenotazione);
        updateSeatsQ.executeUpdate();
        updateUserQ.executeUpdate();
        delete.executeUpdate();
        return true;
    }

    @Override
    public List<String> getEmailsNonLoyalCLients() throws SQLException {
        List<String> emails = new ArrayList<>();
        Connection connection = getConnection();
        String getClients = "SELECT C.EMAIL,C.ID FROM CLIENTE C,PRENOTAZIONE P  WHERE C.ID = P.CLIENTE AND DATEDIFF(CURRENT_DATE(),(SELECT MAX(V1.DATA_PARTENZA) FROM VOLO V1 WHERE V1.ID =P.VOLO AND P.CLIENTE =C.ID))>(2*365)";
        Statement em = connection.createStatement();
        ResultSet emm = em.executeQuery(getClients);
        String updateStatus = "UPDATE CLIENTE C SET C.FEDELTA=FALSE WHERE C.ID=?";
        PreparedStatement update = connection.prepareStatement(updateStatus);
        while (emm.next()) {
            emails.add(emm.getString(1));
            update.setInt(1, emm.getInt(2));
            update.addBatch();
        }
        update.executeBatch();
        return emails;
    }


    public float getCurrentFee(int idVolo, Collection<Integer> seatIds) throws SQLException {
        Connection connection = getConnection();
        float[] tariffe = getTariffe(idVolo);
        float total = 0F;
        ResultSet set;
        PreparedStatement getTotalFee = connection.prepareStatement("SELECT P.CLASSE FROM POSTO P,VOLO V WHERE V.ID=? AND P.AEREO=V.AEREO AND P.ID=?");
        for (Integer seatId : seatIds) {
            getTotalFee.setInt(1, idVolo);
            getTotalFee.setInt(2, seatId);
            set = getTotalFee.executeQuery();
            if (!set.next()) throw new RuntimeException("No results");
            total += tariffe[classes.get(set.getString(1))];
        }
        return total;
    }

    private int getVoloId(int idPrenotazione) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement getVoloId = connection.prepareStatement("SELECT V.ID FROM VOLO V,PRENOTAZIONE P WHERE P.ID=? AND P.VOLO=V.ID");
        getVoloId.setInt(1, idPrenotazione);
        ResultSet set = getVoloId.executeQuery();
        return set.next() ? set.getInt(1) : -1;
    }


    public float getCancellationFee(int idPrenotazione) throws SQLException {
        Connection connection = getConnection();
        int flightId = getVoloId(idPrenotazione);
        PreparedStatement getSeats = connection.prepareStatement("SELECT P.POSTO_PRENOTATO FROM posti_prenotati P WHERE P.ID_PRENOTAZIONE=?");
        getSeats.setInt(1, idPrenotazione);
        ResultSet seats = getSeats.executeQuery();
        List<Integer> seatIds = new ArrayList<>();
        while (seats.next())
            seatIds.add(seats.getInt(1));
        PreparedStatement getOldPrice = connection.prepareStatement("SELECT P.PRICE_PAID FROM PRENOTAZIONE P WHERE P.ID=?");
        getOldPrice.setInt(1, idPrenotazione);
        ResultSet oldPricePaid = getOldPrice.executeQuery();
        if (!oldPricePaid.next()) throw new RuntimeException("No results");
        float oldPrice = oldPricePaid.getFloat(1);
        float currentFee = getCurrentFee(flightId, seatIds);
        return Math.abs(currentFee - oldPrice) + (Math.abs(currentFee) * 25 / 100);
    }


    @Override
    public List<Promozione> getActivePromos() throws SQLException {
        Connection connection = getConnection();
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp inAWeek = Timestamp.from(Instant.now().plus(Duration.ofDays(7)));
        ArrayList<Promozione> arrayList = new ArrayList<>();
        final String promos = "SELECT * FROM PROMOZIONI P WHERE P.INIZIO <= ? AND P.FINE <= ?";
        PreparedStatement getPromos = connection.prepareStatement(promos);
        getPromos.setTimestamp(1, now);
        getPromos.setTimestamp(2, inAWeek);
        ResultSet promotions = getPromos.executeQuery();
        final String associatedToFlight = "SELECT CODICE_VOLO FROM VOLO_PROMO WHERE CODICE_PROMO = ? ";
        PreparedStatement isAssociatedToFlight = connection.prepareStatement(associatedToFlight);
        int currId, eventualFlight;
        Volo volo;
        while (promotions.next()) {
            volo = null;
            currId = promotions.getInt(1);
            isAssociatedToFlight.setInt(1, currId);
            ResultSet temp = isAssociatedToFlight.executeQuery();
            if (temp.next()) {
                eventualFlight = temp.getInt(1);
                volo = getVoloById(eventualFlight);
            }
            arrayList.add(Promozione.fromResultSet(promotions, volo));
        }
        return arrayList;
    }

    private float[] getTariffe(int voloId) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement getFee = connection.prepareStatement("SELECT V.TARIFFA_FIRST,V.TARIFFA_BUSINESS,V.TARIFFA_ECONOMY  FROM VOLO V where V.ID=?");
        getFee.setInt(1, voloId);
        ResultSet tariffs = getFee.executeQuery();
        float[] tariffsArr = new float[3];
        if (!tariffs.next()) throw new RuntimeException("No such flight");
        tariffsArr[0] = tariffs.getFloat(1);
        tariffsArr[1] = tariffs.getFloat(2);
        tariffsArr[2] = tariffs.getFloat(3);
        return tariffsArr;
    }

    private Volo getVoloById(int id) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement voloInfo = connection.prepareStatement("SELECT * FROM VOLO V WHERE V.ID=?");
        voloInfo.setInt(1, id);
        ResultSet volo = voloInfo.executeQuery();
        if (!volo.next()) throw new NoSuchFlightException();
        Volo v = Volo.fromResultSet(volo);
        PreparedStatement seats = connection.prepareStatement(getSeatOnFlight);
        seats.setInt(1, id);
        seats.setInt(2, id);
        float[] currentFlightTariffs = getTariffe(id);
        ResultSet postiVolo = seats.executeQuery();
        postiVolo.last();
        SeatDTO[] posti = new SeatDTO[Math.max(postiVolo.getRow(), 0)];
        int k = 0;
        postiVolo.beforeFirst();
        while (postiVolo.next())
            posti[k++] = new SeatDTO(postiVolo.getInt(1), postiVolo.getString(3) != null, false, currentFlightTariffs[classes.get(postiVolo.getString(2))]);
        v.setPosti(posti);
        return v;
    }

    @Override
    public Prenotazione getPrenotazioneSingola(int prenotazioneID, String email) throws SQLException {
        Connection connection = getConnection();
        String getPr = "SELECT * FROM PRENOTAZIONE P,CLIENTE C WHERE P.ID=? AND P.CLIENTE = C.ID AND C.EMAIL=?";
        PreparedStatement getPrenotazione = connection.prepareStatement(getPr);
        getPrenotazione.setInt(1, prenotazioneID);
        getPrenotazione.setString(2, email);
        ResultSet set = getPrenotazione.executeQuery();
        if (!set.next()) throw new UnauthorizedException();
        PreparedStatement getSeats = connection.prepareStatement(getSeatOnFlight);
        int voloId = set.getInt(2);
        getSeats.setInt(1, voloId);
        getSeats.setInt(2, voloId);
        ResultSet seatsR = getSeats.executeQuery();
        List<SeatDTO> seats = new ArrayList<>(), mySeats = new ArrayList<>();
        boolean mine = false;
        float[] tariffe = getTariffe(voloId);
        while (seatsR.next()) {
            SeatDTO s = (new SeatDTO(seatsR.getInt(1), seatsR.getString(3) != null,
                    mine = seatsR.getString(3) != null && (seatsR.getInt(3) == prenotazioneID), tariffe[classes.get(seatsR.getString(2))]));
            if (mine)
                mySeats.add(s);
            else seats.add(s);
        }
        PreparedStatement preparedStatement = connection.prepareStatement(getFlightByPrenotazione);
        ResultSet volo = preparedStatement.executeQuery();
        volo.next();
        Volo tempVolo = Volo.fromResultSet(volo);
        boolean canCheckIn = Period.between(LocalDate.now(), tempVolo.getDataPartenza().toLocalDateTime().toLocalDate()).getDays() > 3;
        return new Prenotazione(set.getInt(1), tempVolo, seats, mySeats, set.getInt(4), canCheckIn);


    }

    public boolean checkUserInfo(int userId, String password) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement check = connection.prepareStatement(checkUserCredentialsID);
        check.setInt(1, userId);
        ResultSet set = check.executeQuery();
        return set.next() && set.getString(1) != null && pswHandler.checkHashedPassword(set.getString(1), password);
    }

    public boolean modifyReservation(int idPrenotazione, String email, Collection<Integer> seatIds) throws SQLException {
        Connection connection = getConnection();
        final String getPrenotazione;
        getPrenotazione = "SELECT P.ID,P.CLIENTE,P.VOLO  FROM PRENOTAZIONE P,CLIENTE C WHERE P.ID=? AND P.CLIENTE=C.ID AND C.EMAIL=?";
        PreparedStatement getPrenQ = connection.prepareStatement(getPrenotazione);
        getPrenQ.setInt(1, idPrenotazione);
        getPrenQ.setString(2, email);
        ResultSet prenotazioneEsiste = getPrenQ.executeQuery();
        if (!prenotazioneEsiste.next()) return false;
        int idCliente = prenotazioneEsiste.getInt(2);
        int idVolo = prenotazioneEsiste.getInt(3);
        PreparedStatement deleteOldSeats = connection.prepareStatement("DELETE FROM POSTI_PRENOTATI P WHERE P.ID_PRENOTAZIONE=?");
        deleteOldSeats.setInt(1, idPrenotazione);
        deleteOldSeats.executeUpdate();
        PreparedStatement insertNewSeats = connection.prepareStatement(BOOK_SEATS);
        deleteOldSeats.setInt(1, idPrenotazione);
        deleteOldSeats.executeUpdate();
        for (Integer seatId : seatIds) {
            insertNewSeats.setInt(1, idPrenotazione);
            insertNewSeats.setInt(2, seatId);
            insertNewSeats.setInt(3, idCliente);
            insertNewSeats.setInt(4, idVolo);
            insertNewSeats.addBatch();
        }
        insertNewSeats.executeBatch();
        return true;
    }


    @Override
    public List<Volo> getFlights(Timestamp fromD, Timestamp toD, String from, String to) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement allFlights = connection.prepareStatement(getAllFlights);
        allFlights.setString(1, from);
        allFlights.setString(2, to);
        allFlights.setTimestamp(3, fromD);
        allFlights.setTimestamp(4, toD);
        List<Volo> voli = new ArrayList<>();
        PreparedStatement seats = connection.prepareStatement(getSeatOnFlight);
        ResultSet voliRisultato = allFlights.executeQuery();
        while (voliRisultato.next())
            voli.add(getVoloById(voliRisultato.getInt(1)));
        return voli;
    }

    @Override
    public int createUser(Cliente DTO) throws SQLException {
        Connection connection = getConnection();
        Cliente c;
        //info del cliente già nel database
        //ciò succede quando ha già prenotato un volo senza registrarsi
        //pertanto viene solamente aggiunto il campo password nella tabella rendendolo
        // un utente registrato
        if ((c = checkIfClienteExistsByMail(DTO.getEmail())) != null && DTO.getPassword() != null) {
            String query = "UPDATE CLIENTE C SET C.PASSWORD_HASH=? WHERE C.ID=?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, pswHandler.hashPassword(DTO.getPassword()));
            statement.setInt(2, c.getId());
            statement.executeUpdate();
            return c.getId();
        }
        PreparedStatement create = connection.prepareStatement(createUser, Statement.RETURN_GENERATED_KEYS);
        create.setString(1, null);
        create.setString(2, DTO.getNome());
        create.setString(3, DTO.getCognome());
        create.setString(4, DTO.getIndirizzo());
        create.setBoolean(5, true);
        create.setString(6, DTO.getEmail());
        create.setInt(7, 0);
        create.setDate(9, new java.sql.Date(DTO.getDataNascita().getTime()));
        String hash = null;
        if (DTO.getPassword() != null)
            hash = pswHandler.hashPassword(DTO.getPassword());
        create.setString(8, hash);
        create.executeUpdate();
        ResultSet idSet = create.getGeneratedKeys();
        idSet.next();
        return idSet.getInt(1);
    }


    public List<String[]> getEmailsToNotify() throws SQLException {
        Connection connection = getConnection();
        PreparedStatement getUser = connection.prepareStatement(getBookingsToNotify);
        ResultSet set = getUser.executeQuery();
        List<String[]> emailsToNotify = new ArrayList<>();
        while (set.next()) {
            emailsToNotify.add(new String
                    []{set.getString(1), set.getString(2)});
        }
        return emailsToNotify;
    }
}
