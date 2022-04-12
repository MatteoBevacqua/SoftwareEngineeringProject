package command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum ConcreteCommandHandler implements CommandHandler {
    INSTANCE;
    private final ExecutorService executorService ;

    ConcreteCommandHandler() {
        executorService = Executors.newSingleThreadExecutor();
    }


    //esecuzione asincrona
    @Override
    public void handleCommand(Command command) {
        executorService.execute(command::execute);
    }
}
