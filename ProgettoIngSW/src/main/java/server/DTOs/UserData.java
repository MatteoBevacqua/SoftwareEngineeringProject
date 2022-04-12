package server.DTOs;

import java.io.Serializable;

public record UserData(int id,String nome,String cognome,String email,int miles,boolean fedelta,String dataNascita)  implements Serializable {
}