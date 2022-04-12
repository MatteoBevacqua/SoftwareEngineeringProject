package server.DB;

import server.strategy.PasswordHashingStrategy;

public abstract class AbstractDAOImpl implements DAOImplementor {
    protected final PasswordHashingStrategy pswHandler;

    public AbstractDAOImpl(PasswordHashingStrategy pw) {
        this.pswHandler = pw;
    }

}
