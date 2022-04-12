package command;


import command.Command;
import lombok.AllArgsConstructor;
import pannelli.BookingPanel;
import pannelli.DisplayBookingInfo;
import pannelli.MyReservationPanel;
import pannelli.ParteGrafica;
import server.DTOs.UserData;
import server.DTOs.UserInfo;
import server.entities.Prenotazione;
import server.support.MyOptional;

import java.util.List;
import javax.swing.*;
import javax.swing.text.html.Option;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Optional;

@AllArgsConstructor
public class LoginCommand implements Command {
    private int userID;
    private String password;
    private JPanel outcome;

    @Override
    public void execute() {
        try {
            UserInfo userInfo = new UserInfo(userID, password);
            boolean exitStatus = ParteGrafica.INSTANCE.getServerStub().login(userInfo);
            int userId = userInfo.userId();
            ParteGrafica.INSTANCE.userLoggedIn();
            ParteGrafica.INSTANCE.setUserStatus(userId);
            String textToShow;
            if (exitStatus) {
                textToShow = "Welcome!";
                ParteGrafica.INSTANCE.setAuthInfo(userInfo);
                MyOptional<UserData> userData = ParteGrafica.INSTANCE.getServerStub().displayUserData(ParteGrafica.INSTANCE.getAuthInfo());
                if(userData.isEmpty()) return;
                ParteGrafica.INSTANCE.setUserData(userData.get());
            } else textToShow = "Something went wrong,check your password and try again";
            JOptionPane.showMessageDialog(outcome, textToShow, "Login outcome", JOptionPane.INFORMATION_MESSAGE);
            System.out.println(userId);
            if (exitStatus) {
                JTabbedPane c = (JTabbedPane) outcome.getParent();
                c.remove(outcome);
                List<Prenotazione> prenotazioni = ParteGrafica.INSTANCE.getServerStub().getPrenotazioni(ParteGrafica.INSTANCE.getAuthInfo());
                ParteGrafica.INSTANCE.addTab("My Reservations", new MyReservationPanel(prenotazioni));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
