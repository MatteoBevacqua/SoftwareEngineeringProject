package pannelli;

import exceptions.UnreachableServerException;
import server.entities.Promozione;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.List;

public class PromoPanel extends JPanel {

    private static class DisplayPromoInfo extends JPanel {
        private final Promozione p;
        private final JPanel top, bottom;

        public DisplayPromoInfo(Promozione p, boolean relativeToFlight) {
            super(new GridLayout(2, 0));
            top = new JPanel(new GridLayout(0, 1));
            bottom = new JPanel(new GridLayout(relativeToFlight ? 2 : 1, 0));
            this.p = p;
            top.add(new JLabel("Valid from : " + p.start().toString() + " until " + p.end().toString()));
            JCheckBox checkBox = new JCheckBox(" Loyalty only");
            checkBox.setSelected(!p.everyone());
            checkBox.setEnabled(false);
            bottom.add(checkBox);
            bottom.add(new JLabel("Discount percentage : " + new DecimalFormat("##.#").format(p.sconto() * 100) + "%"));
            if(relativeToFlight)
                bottom.add(new JLabel(" Promotion valid only for the following flight "));
            add(top);
            add(bottom);
            setBorder(BorderFactory.createRaisedSoftBevelBorder());
            repaint();
        }
    }

    private JScrollPane mainPanel;
    private JPanel bottom;

    public PromoPanel() {
        JButton button = new JButton("Load the latest promotions");
        var ref = new Object() {
            List<Promozione> promozioni;
        };
        button.addActionListener(l -> {
            try {
                ref.promozioni = ParteGrafica.INSTANCE.getServerStub().getPromos();
                int rows = 0;
                for (Promozione promozione : ref.promozioni)
                    rows += promozione.volo() != null ? 2 : 1;
                bottom = new JPanel(new GridLayout(rows, 0));
                mainPanel = new JScrollPane(bottom);
                mainPanel.setPreferredSize(new Dimension(485, 450));
                mainPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                ref.promozioni.forEach(p -> {
                    bottom.add(new DisplayPromoInfo(p,p.volo()!=null));
                    if (p.volo() != null) {
                        bottom.add(new DisplayFlightInfo(p.volo(), true));
                    }
                });
                add(mainPanel);
                revalidate();
            } catch (RemoteException e) {
                System.out.println(e);
            }
        });
        button.setLocation(240, 50);
        add(button);
        setVisible(true);


    }

}
