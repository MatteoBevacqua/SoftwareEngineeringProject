package pannelli;

import command.ConcreteCommandHandler;
import command.DeleteReservationCommand;
import server.DTOs.SeatDTO;
import server.entities.Prenotazione;

import javax.swing.*;
import java.awt.*;

public class DisplayBookingInfo extends JPanel {
    private final Prenotazione prenotazione;
    private JPanel infoPanel;
    private JLabel prenID, seats, mySeats, pointsGained, date;
    private JButton modifyRes, checkIn, delete;

    public DisplayBookingInfo(Prenotazione prenotazione) {
        this.prenotazione = prenotazione;
        prenID = new JLabel("Booking #" + prenotazione.id());
        seats = new JLabel("N° of seats reserved : " + prenotazione.mySeats().size());
        mySeats = new JLabel("My seats : " + prenotazione.mySeats().stream().map(SeatDTO::seatId).toList());
        pointsGained = new JLabel("Points gained : " + prenotazione.miglia());
        modifyRes = new JButton("Modify Reservation");
        delete = new JButton("Delete Reservation");
        date = new JLabel("Departure date : " + prenotazione.volo().getDataPartenza().toLocalDateTime().toString());
        delete.addActionListener(action -> {
            ConcreteCommandHandler.INSTANCE.handleCommand(new DeleteReservationCommand(prenotazione.id()));
        });
        modifyRes.addActionListener(action -> {
            ChangeReservationPanel panel = new ChangeReservationPanel(prenotazione);
            panel.init();
            ParteGrafica.INSTANCE. addTab("Modify booking", panel);
        });
        infoPanel = new JPanel(new GridLayout(7, 0));
        checkIn = new JButton("Proceed with check - in");
        checkIn.setEnabled(prenotazione.canCheckIn());
        if (!checkIn.isEnabled())
            checkIn.setToolTipText("You will be able to check in 3 days before the flight departs");
        infoPanel.add(prenID);
        infoPanel.add(date);
        infoPanel.add(seats);
        infoPanel.add(mySeats);
        infoPanel.add(checkIn);
        infoPanel.add(modifyRes);
        infoPanel.add(delete);
        infoPanel.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        add(infoPanel);

    }
}
