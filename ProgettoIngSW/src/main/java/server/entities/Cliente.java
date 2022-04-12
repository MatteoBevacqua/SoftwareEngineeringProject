package server.entities;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Getter
@Setter
public class Cliente implements Cloneable, Serializable {
    private final static Cliente prototype = new Cliente();
    static {
        prototype.dataNascita = new Date();
    }
    private boolean isFedelta;
    private int id;
    private String password,nome,cognome,indirizzo,email;
    private Date dataNascita;

    public static Cliente getPrototype(){
        return prototype;
    }

    public Cliente clone() {
        try {
            Cliente c = (Cliente) super.clone();
            c.dataNascita = (Date) dataNascita.clone();
            return c;
        } catch (CloneNotSupportedException e) {
            throw new Error();
        }
    }

    public static Cliente fromResultSet(ResultSet set) throws SQLException {
        Cliente c = prototype.clone();
        c.setId(set.getInt(1));
        c.setNome(set.getString(2));
        c.setCognome(set.getString(3));
        c.setIndirizzo(set.getString(4));
        c.setFedelta(set.getBoolean(5));
        c.setEmail(set.getString(6));
        return c;
    }


}
