package pannelli;


import client.ErrorHandler;
import command.ConcreteCommandHandler;
import command.PlaceOrderCommand;
import server.DTOs.SeatDTO;
import server.entities.Volo;

import javax.swing.*;
import java.awt.*;
import java.util.*;


public class BookingPanel extends JPanel {
    private Volo volo;
    protected JPanel firstInfo, secondInfo, planeLayout;
    private JLabel partenza, posti;
    private JButton[] seats;
    private JScrollPane scrollPane;
    private JPanel seatInfo, outcome;
    protected JButton placeOrder;
    protected Set<SeatDTO> seatsToBook = new HashSet<>();
    protected JLabel subTotal;
    protected int tempPrice = 0;

    public BookingPanel(Volo volo) {
        super(new FlowLayout(FlowLayout.CENTER));
        this.volo = volo;
        subTotal = new JLabel("Subtotal : " + tempPrice + "€");
        subTotal.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        outcome = new JPanel();
        placeOrder = new JButton("Book the selected seats");
        placeOrder.addActionListener(action -> {
            if (seatsToBook.size() > 0)
                ConcreteCommandHandler.INSTANCE.handleCommand(new PlaceOrderCommand(seatsToBook, volo.getId()));
            else ErrorHandler.showErrorMessage("Select some seats first");
        });
        seats = new JButton[volo.getPosti().length];
        firstInfo = new JPanel();
        seatInfo = new JPanel(new GridLayout(2, 0));
        int rows = (int) Math.ceil(volo.getPosti().length / (double) 4);
        planeLayout = new JPanel(new GridLayout(rows, 4, 10, 15));
        scrollPane = new JScrollPane(planeLayout);
        secondInfo = new JPanel();
        partenza = new JLabel("The flight will depart at :");
        posti = new JLabel("Select the seats you want to book");
        add(new DisplayFlightInfo(volo, false));
        JLabel info = new JLabel(), seatsLeft = new JLabel();
        info.setText(volo.getDataPartenza().toString());
        seatsLeft.setText("Seats left on the plane : " + volo.getPosti().length);
        firstInfo.add(partenza);
        firstInfo.add(info);
        secondInfo.add(posti);
        secondInfo.add(seatsLeft);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(500, 270));
        add(firstInfo);
        add(secondInfo);
        add(posti);
        add(scrollPane);
        add(subTotal);
        add(placeOrder);
        add(outcome);

    }

    public final void init() {
        loadSeats();
    }

    protected void loadSeats() {
        final int[] k = {0};
        Arrays.stream(volo.getPosti()).forEach(posto -> {
            JButton button = new JButton("Seat #" + posto.seatId());
            button.setBackground(posto.isBooked() ? Color.RED : Color.GREEN);
            if (!posto.isBooked())
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
            seats[k[0]] = button;
            planeLayout.add(c);
        });
    }

}
