package pannelli;


import server.entities.Volo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class DisplayFlightInfo extends JPanel {
    final Volo volo;
    private JPanel top, b1, b2, b3, b4;

    public DisplayFlightInfo(Volo volo, boolean clickable) {
        super(new GridLayout(5, 0));
        this.volo = volo;
        top = new JPanel();
        b1 = new JPanel();
        b2 = new JPanel();
        b3 = new JPanel();
        b4 = new JPanel();
        JLabel left = new JLabel(), right = new JLabel(), dateLeft = new JLabel(), gate = new JLabel(), sugg = new JLabel(), air = new JLabel();
        air.setText("Departing from " + volo.getAereoporto());
        left.setText(volo.getPartenza() + " ->");
        right.setText(volo.getDestinazione() + " Flight#" + volo.getId());
        dateLeft.setText("Departure at :" + volo.getDataPartenza().toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME).replace("T", " "));
        gate.setText("The flight will depart from gate : " + volo.getGate());
        sugg.setText("Suggested arrival time at the gate is : " + volo.getDataPartenza().toLocalDateTime().minus(Duration.ofHours(2)).format(DateTimeFormatter.ISO_DATE_TIME).replace("T", " "));
        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        b1.add(dateLeft);
        b2.add(gate);
        b3.add(sugg);
        b4.add(air);
        setBorder(BorderFactory.createRaisedSoftBevelBorder());
        if (clickable)
            addMouseListener(new MyMouseAdapter());
        add(top);
        add(b1);
        add(b2);
        add(b3);
        add(b4);

    }

    class MyMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            BookingPanel panel = new BookingPanel(volo);
            panel.init();
            ParteGrafica.INSTANCE.addTab(" Flight#" + volo.getId() + " booking page", panel);
        }
    }
}
