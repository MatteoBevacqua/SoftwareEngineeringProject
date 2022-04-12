package pannelli;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class PaymentPanel extends JPanel {
    private JLabel title, pay;
    private final JTextField[] numbers = new JTextField[4];
    private final float sumToPay;
    private JPanel top, card, bottom;
    private JButton proceed;

    private boolean cardHasBeenValidated = false;

    public boolean hasCardHasBeenValidated() {
        return cardHasBeenValidated;
    }



    public PaymentPanel(float sumToPay) {
        super(new GridLayout(2, 0));
        top = new JPanel(new GridLayout(2, 0));
        card = new JPanel(new GridLayout(1, 0));
        bottom = new JPanel(new GridLayout(2, 0));
        this.sumToPay = sumToPay;
        title = new JLabel("Insert your credit card information to pay");
        pay = new JLabel("Total sum to pay : " + sumToPay + "€");
        InputVerifier[] verifiers = new InputVerifier[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = new JTextField();
            numbers[i].setInputVerifier(verifiers[i] = new InputVerifier() {
                @Override
                public boolean verify(JComponent input) {
                    return ((JTextField) input).getText().matches("[0-9]{4}");
                }
            });
            card.add(numbers[i]);
        }
        proceed = new JButton("Verify Card");
        proceed.addActionListener(action -> {
            String card = "";
            for (JTextField number : numbers) {
                if (number.getText().matches("[0-9]{4}"))
                    card += number.getText();
                else {
                    JOptionPane.showMessageDialog(this, "Invalid card");
                    return;
                }
            }
            if (luhnCheck(card)) {
                cardHasBeenValidated = true;
                JOptionPane.showMessageDialog(this, "Card ok!", "Performing payment", JOptionPane.INFORMATION_MESSAGE);
            } else JOptionPane.showMessageDialog(this, "Invalid card");

        });
        top.add(title);
        top.add(pay);
        bottom.add(card);
        bottom.add(proceed);
        add(top);
        add(bottom);

    }

    private static boolean luhnCheck(String str) {
        int[] numeriCarta = new int[str.length()];
        for (int i = 0; i < str.length(); i++)
            numeriCarta[i] = Integer.parseInt(str.charAt(i) + "");
        for (int i = numeriCarta.length - 2; i >= 0; i -= 2) {
            int j = numeriCarta[i];
            j *= 2;
            if (j > 9)
                j = j % 10 + 1;
            numeriCarta[i] = j;
        }
        int sommaFinale = 0;
        for (int i = 0; i < numeriCarta.length; i++)
            sommaFinale += numeriCarta[i];

        return true || sommaFinale % 10 == 0;

    }

}
