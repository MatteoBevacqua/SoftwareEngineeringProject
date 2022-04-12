package client;

import java.util.Date;

public class Utils {

    public static java.sql.Timestamp dateToTimestamp(Date d){
        return new java.sql.Timestamp(d.toInstant().toEpochMilli());
    }


}
