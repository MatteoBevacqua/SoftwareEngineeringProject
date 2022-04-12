package server.DTOs;


import java.io.Serializable;

public record UserInfo(int userId, String password) implements Serializable {
}