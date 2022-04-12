package pannelli;

import command.ConcreteCommandHandler;
import command.LoginCommand;
import command.RegisterCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AccountPanel extends JPanel {
    private final JPanel result;
    private final MyField userName;
    private MyField name;
    private MyField surname;
    private MyField email;
    private final MyField dataNascita;
    private final MyField address;
    private final JPasswordField passwordField;
    private final JPasswordField registPass;
    private final JPasswordField reenterPsw;

    public AccountPanel() {
        BoxLayout boxLayout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(boxLayout);
        userName = new MyField("Username");
        dataNascita = new MyField("Date of birth : DD/MM/YYYY");
        address = new MyField("Insert your billing address");
        dataNascita.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                return ((MyField) input).getText().matches("[0-9]{2}/[0-9]{2}/[0-9]{4}");
            }
        });
        JButton performLogin = new JButton("Login");
        JButton performRegistration = new JButton("Register");
        GridLayout first = new GridLayout(6, 0, 15, 10),
                second = new GridLayout(7, 0, 15, 10);
        JPanel loginPanel = new JPanel(first);
        JPanel registerPanel = new JPanel(second);
        result = new JPanel();
        passwordField = new JPasswordField();
        registPass = new JPasswordField();
        reenterPsw = new JPasswordField();
        registPass.setText("Password");
        reenterPsw.setText("Password");
        userName.setPreferredSize(new Dimension(270, 25));
        passwordField.setPreferredSize(new Dimension(270, 25));
        JLabel insertPassword = new JLabel("Insert your password");
        JLabel insertUsername = new JLabel("Insert your user ID");
        performLogin.addActionListener(action -> {
            ConcreteCommandHandler.INSTANCE.handleCommand(new LoginCommand(
                    Integer.parseInt(userName.getText()), new String(passwordField.getPassword()), this
            ));
        });
        performRegistration.addActionListener(action -> {
            String firstPass = new String(registPass.getPassword()), secondPass = new String(reenterPsw.getPassword());
            if (!firstPass.equals(secondPass)) {
                JOptionPane.showMessageDialog(this, "The passwords don't match!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            ConcreteCommandHandler.INSTANCE.handleCommand(new RegisterCommand(
                    name.getText(), surname.getText(), email.getText(), new String(registPass.getPassword()), dataNascita.getText(),address.getText(), result
            ));
        });
        JPanel logi = new JPanel();
        JLabel login = new JLabel("Login if you already have an account");
        logi.add(login);
        loginPanel.add(logi);
        loginPanel.add(insertUsername);
        loginPanel.add(userName);
        loginPanel.add(insertPassword);
        loginPanel.add(passwordField);
        loginPanel.add(performLogin);
        loginPanel.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        registerPanel.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        name = new MyField("First name");
        surname = new MyField("Second Name");
        email = new MyField("Email");
        registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.PAGE_AXIS));
        name.setPreferredSize(new Dimension(270, 25));
        surname.setPreferredSize(new Dimension(270, 25));
        email.setPreferredSize(new Dimension(270, 25));
        reenterPsw.setPreferredSize(new Dimension(270, 25));
        registerPanel.add(new JLabel("Register to join our loyalty program!"));
        JPanel c1 = new JPanel(), c2 = new JPanel();
        c1.add(performRegistration);
        registerPanel.add(name);
        registerPanel.add(surname);
        registerPanel.add(email);
        registerPanel.add(dataNascita);
        registerPanel.add(address);
        registerPanel.add(registPass);
        registerPanel.add(reenterPsw);
        registerPanel.add(c1);
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(150, 25));
        add(loginPanel);
        add(spacer);
        add(registerPanel);
        add(result);
    }

    static class MyField extends JTextField {
        public MyField(String label) {
            super(label);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setText("");
                }
            });
        }
    }
}
