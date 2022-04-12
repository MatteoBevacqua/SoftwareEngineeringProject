package command;

import pannelli.ParteGrafica;


import lombok.AllArgsConstructor;
import server.DTOs.UserInfo;
import server.entities.Cliente;

import javax.swing.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

@AllArgsConstructor
public class RegisterCommand implements Command {
    private final String  name, surname, email, password, dataNascita,address;
    private JPanel outcome;

    @Override
    public void execute() {
        try {
            outcome.removeAll();
            Cliente cliente = Cliente.getPrototype().clone();
            cliente.setNome(name);
            cliente.setCognome(surname);
            cliente.setPassword(password);
            cliente.setEmail(email);
            cliente.setIndirizzo(address);
            cliente.setFedelta(true);
            DateFormat sourceFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date date = sourceFormat.parse(dataNascita);
            cliente.setDataNascita(date);
            int generatedID = ParteGrafica.INSTANCE. getServerStub().createUser(cliente);
            if (generatedID >= 0) {
                outcome.add(new JTextField("Login to access your new account!"));
                ParteGrafica.INSTANCE. setAuthInfo(new UserInfo(generatedID, password));
                JOptionPane.showMessageDialog(null,"Your user id is: " + generatedID+ "\nUse it with your password to login from now on!");
            } else outcome.add(new JTextField("Something went wrong,retry in a bit"));
            outcome.revalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
