package server.entities;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public record Promozione(float sconto, Volo volo, boolean everyone, Date start, Date end) implements Serializable {


    public static Promozione fromResultSet(ResultSet set, Volo volo1) throws SQLException {
        return new Promozione(
                set.getFloat(5),
                volo1,
                set.getBoolean(2),
                set.getTimestamp(3),
                set.getTimestamp(4)
        );
    }
}
