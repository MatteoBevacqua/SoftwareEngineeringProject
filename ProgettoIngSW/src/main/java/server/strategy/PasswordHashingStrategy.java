package server.strategy;

public interface PasswordHashingStrategy {
    String hashPassword(String password);
    boolean checkHashedPassword(String hash,String password);
}
