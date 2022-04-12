package server.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Date;

@Getter
@Setter
@AllArgsConstructor
public class GeneralitaUtente implements Serializable {
    private String nome, cognome, email,indirizzo;
    private Date dataNascita;
}
