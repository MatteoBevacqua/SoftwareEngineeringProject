package pannelli;


import command.ConcreteCommandHandler;
import command.ModifyTabCommand;
import server.DTOs.SeatDTO;
import server.entities.Prenotazione;
import server.entities.Volo;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


public class ChangeReservationPanel extends BookingPanel {
    private Prenotazione p;
    private HashSet<SeatDTO> mySeats;

    public ChangeReservationPanel(Volo volo) {
        super(volo);
    }

    public ChangeReservationPanel(Prenotazione p) {
        this(p.volo());
        this.p = p;
        remove(subTotal);
        remove(placeOrder);
        placeOrder = new JButton("Apply changes");
        placeOrder.addActionListener(action -> {
            ConcreteCommandHandler.INSTANCE.handleCommand(new ModifyTabCommand(p, tempPrice, seatsToBook));
        });
        add(placeOrder);

    }

    protected void loadSeats() {
        p.bookedSeats().addAll(p.mySeats());
        List<SeatDTO> seats = new ArrayList<>(p.bookedSeats());
        seats.sort(SeatDTO::compareTo);
        seats.forEach(posto -> {
            System.out.println(posto);
            JButton button = new JButton("Seat #" + posto.seatId());
            button.setBackground(posto.isBooked() ? (posto.bookedByMe() ? Color.YELLOW : Color.RED) : Color.GREEN);
            if (posto.bookedByMe()) seatsToBook.add(posto);
            if (!posto.isBooked() || posto.bookedByMe())
                button.addActionListener(action -> {
                    if (seatsToBook.contains(posto)) {
                        seatsToBook.remove(posto);
                        tempPrice -= posto.prezzo();
                        button.setBackground(Color.GREEN);
                    } else {
                        seatsToBook.add(posto);
                        tempPrice += posto.prezzo();
                        button.setBackground(Color.YELLOW);
                    }
                    subTotal.setText("Subtotal : " + tempPrice + "€");
                });
            else button.setEnabled(false);
            JPanel c = new JPanel(new GridLayout(2, 0));
            c.add(button);
            c.add(new JLabel("Price : " + posto.prezzo() + "€"));
            planeLayout.add(c);
        });

    }
}
