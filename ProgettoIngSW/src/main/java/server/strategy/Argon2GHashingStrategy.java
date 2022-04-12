package server.strategy;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class Argon2GHashingStrategy implements PasswordHashingStrategy {
    //thread safe
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 16, 32);

    @Override
    public String hashPassword(String password) {
        String hash = argon2.hash(4, 1024, 8, password);
        if (!argon2.verify(hash, password)) throw new RuntimeException("Fatal computation error");
        return hash;
    }

    @Override
    public boolean checkHashedPassword(String hash, String password) {
        return argon2.verify(hash, password);
    }
}
