package pannelli;

import server.DTOs.UserData;
import server.entities.Prenotazione;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MyReservationPanel extends JPanel {
    private List<Prenotazione> prenotazioniUtente;
    private JPanel top, bottomFixed;
    private JScrollPane bottom;
    private final JLabel user, name, secondName, userId, miles, fedelta, email, bookings;
    private UserData dataToDisplay;

    public MyReservationPanel(List<Prenotazione> prenotazioniUtente) {

        super(new GridLayout(2, 0));
        top = new JPanel(new GridLayout(9, 0));
        dataToDisplay = ParteGrafica.INSTANCE.getUserData();
        bottomFixed = new JPanel(new GridLayout(prenotazioniUtente.size() + 1, 0));
        bottom = new JScrollPane(bottomFixed);
        bottom.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.prenotazioniUtente = prenotazioniUtente;
        if (prenotazioniUtente.size() == 0) {
            bottomFixed.add(new JLabel("Nothing to show"));
        } else
            prenotazioniUtente.forEach(pren -> {
                bottomFixed.add(new DisplayBookingInfo(pren));
            });
        JLabel[] labels = {
                user = new JLabel("Account details"),
                name = new JLabel("First Name : " + dataToDisplay.nome()),
                secondName = new JLabel("Second Name : " + dataToDisplay.cognome()),
                email = new JLabel("Email : " + dataToDisplay.email()),
                userId = new JLabel("UserId : " + dataToDisplay.id()),
                miles = new JLabel("Cumulative points : " + dataToDisplay.miles()),
                fedelta = new JLabel("Loyalty Program : "), bookings = new JLabel("Your bookings ")};
        Font f = labels[0].getFont();
        for (JLabel label : labels) {
            label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
            label.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        }
        JPanel[] topPanels = new JPanel[labels.length];
        for (int i = 0; i < topPanels.length; i++)
            topPanels[i] = new JPanel();
        int k = 0;
        for (JLabel label : labels)
            topPanels[k++].add(label);
        for (JPanel topPanel : topPanels)
            top.add(topPanel);
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(dataToDisplay.fedelta());
        checkBox.setEnabled(false);
        topPanels[topPanels.length - 2].add(checkBox);
        add(top);
        add(bottom);

    }
}
