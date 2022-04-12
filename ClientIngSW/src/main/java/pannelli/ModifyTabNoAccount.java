package pannelli;

import client.ErrorHandler;
import server.entities.Prenotazione;
import server.exceptions.UnauthorizedException;
import server.support.MyOptional;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.Optional;

public class ModifyTabNoAccount extends JPanel {

    private JPanel[] top;
    private JTextField[] fields = {
            new JTextField("        Email       "),
            new JTextField("        Numerical ID        "),
    };
    private JPanel topMain = new JPanel(new GridLayout(7, 0));
    private JLabel[] labels = {new JLabel("Insert the email you used to book the flight"), new JLabel("Insert your reservation id")};
    private JButton getRes;
    private JPanel reservation = new JPanel();

    public ModifyTabNoAccount() {
        super(new GridLayout(2, 0));
        for (JTextField field : fields) {
            field.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    field.setText("");
                }
            });
        }
        top = new JPanel[fields.length * 2 + 1];
        for (int i = 0; i < fields.length; i++) {
            top[i] = new JPanel();
            top[i + 1] = new JPanel();
            top[i].add(labels[i]);
            top[i + 1].add(fields[i]);
            topMain.add(top[i]);
            topMain.add(top[i + 1]);
        }
        getRes = new JButton("Find reservation");
        getRes.addActionListener(action -> {
            try {
                reservation.removeAll();
                MyOptional<Prenotazione> p = (ParteGrafica.INSTANCE.getServerStub().getPrenotazioneSingola(Integer.parseInt(
                        fields[1].getText()
                ), fields[0].getText()));
                if(p.isEmpty()) return;
                ParteGrafica.INSTANCE.setEmail(fields[0].getText());
                reservation.add(new DisplayBookingInfo(p.get()));
                revalidate();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            catch (UnauthorizedException e){
                ErrorHandler.showErrorMessage("No reservation found,check the id and try again");
            }
        });
        reservation.setPreferredSize(new Dimension(124, 150));
        top[top.length - 1] = new JPanel();
        top[top.length - 1].add(getRes);
        topMain.add(top[top.length - 1]);
        add(topMain);
        add(reservation);
    }
}
