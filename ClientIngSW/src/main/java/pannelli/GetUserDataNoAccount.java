package pannelli;

import server.DTOs.GeneralitaUtente;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class GetUserDataNoAccount extends JPanel {
    private JTextField name = new JTextField("Insert your name here"),
            secondName = new JTextField("Insert your second name"),
            address = new JTextField("Insert your billing address"),
            date = new JTextField("Insert your date of birth - DD/MM/YYYY"),
            email = new JTextField("Insert your email");

    private JPanel north, south;
    private JLabel noL = new JLabel("Fill these fields if you don't have an account"), yesL = new JLabel("Fill this field only if you have an account");
    private JTextField loyaltyIdNumber = new JTextField("Insert your loyalty id number");
    final JTextField[] fields;

    public GetUserDataNoAccount(boolean loyaltyEnabled) {
        super(new GridLayout(2, 0));
        final int[] k = {0};
        north = new JPanel(new GridLayout(7, 0));
        south = new JPanel(new GridLayout(3, 0));
        north.add(noL);
        south.add(yesL);
        fields = new JTextField[]{name, secondName, email, date, address};
        for (int i = 0; i < fields.length; i++) {
            JTextComponent c = fields[i];
            fields[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    c.setText("");
                }
            });
            north.add(fields[i]);
        }
        loyaltyIdNumber.setEnabled(loyaltyEnabled);
        if (loyaltyEnabled) {
            south.add(loyaltyIdNumber);
            loyaltyIdNumber.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    loyaltyIdNumber.setText("");
                }
            });
        }
        add(north);
        add(south);
    }

    public GeneralitaUtente getUserData() throws ParseException {
        String[] fieldsContent = new String[fields.length];
        for (int i = 0; i < fields.length; i++)
            fieldsContent[i] = fields[i].getText();
        DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date d = null;
        d = sourceFormat.parse(fieldsContent[3]);
        return new GeneralitaUtente(name.getText(), secondName.getText(), email.getText(), address.getText(), new java.sql.Date(d.getTime()));
    }

    public int loyalty() {
        return loyaltyIdNumber.getText().length() != 0 && loyaltyIdNumber.getText().matches("[0-9]+") ? Integer.parseInt(loyaltyIdNumber.getText()) : -1;
    }
}
