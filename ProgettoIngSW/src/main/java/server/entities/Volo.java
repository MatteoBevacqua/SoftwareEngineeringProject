package server.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import server.DTOs.SeatDTO;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
public class Volo implements Serializable, Cloneable {




    private int id, gate, length;
    private Timestamp dataPartenza;
    private SeatDTO[] posti = new SeatDTO[0];
    private String partenza, destinazione,aereo,aereoporto;

    private Volo() {

    }

    public static Volo fromResultSet(ResultSet set) throws SQLException {
        Volo v = new Volo();
        v.setId(set.getInt(1));
        v.setPartenza(set.getString(2));
        v.setDestinazione(set.getString(3));
        v.setDataPartenza(set.getTimestamp(4));
        v.setAereo(set.getString(5));
        v.setAereoporto(set.getString(6));
        v.setLength(set.getInt(8));
        v.setGate(set.getInt(9));
        return v;
    }

}
