package command;

import client.ErrorHandler;
import pannelli.ParteGrafica;
import pannelli.PaymentPanel;

import javax.swing.*;

public class DeleteReservationCommand implements Command {
    private final int prenID;
    private float penale;

    public DeleteReservationCommand(int id) {
        prenID = id;

    }

    @Override
    public void execute() {
        try {
            PaymentPanel panel;
            try {
                penale = ParteGrafica.INSTANCE.getServerStub().getCancellationFee(prenID);
            } catch (Exception e) {
                ErrorHandler.showErrorMessage("Failed to reach the server");
                return;
            }
            do {
                int option = JOptionPane.showConfirmDialog(null, panel = new PaymentPanel(penale), "Pay the 25% cancellation fee",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (option == JOptionPane.CANCEL_OPTION) return;
            } while (!panel.hasCardHasBeenValidated());
            boolean outcome = ParteGrafica.INSTANCE.getServerStub().deleteReservation(prenID, ParteGrafica.INSTANCE.getEmail());
            if (outcome)
                JOptionPane.showMessageDialog(null, "Reservation deleted successfully!\nYou will get a refund within of the remaining 75% of the sum\n within 3 business days");
            else JOptionPane.showMessageDialog(null, "Something went wrong,please try later");
            if (ParteGrafica.INSTANCE.isUserLoggedIn())
                ConcreteCommandHandler.INSTANCE.handleCommand(new RefreshReservationsCommand());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
