package pannelli;


import exceptions.UnreachableServerException;
import lombok.Getter;
import lombok.Setter;
import server.DTOs.UserData;
import server.DTOs.UserInfo;
import server.implementation.*;

import javax.swing.*;
import java.awt.*;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;



public enum ParteGrafica {
    INSTANCE;
    private UserInfo authInfo;
    private int userId = -1;
    private final ServerInterface serverStub;
    @Getter
    private JTabbedPane mainPanel;
    @Getter
    @Setter
    private UserData userData;
    @Getter
    final List<Component> activeTabs = new ArrayList<>();
    @Getter
    @Setter
    private String email;
    private final JFrame mainFrame;
    private boolean userIsLoggedIn = false;


    ParteGrafica() {
        try {
            Registry registry = LocateRegistry.getRegistry();
            serverStub = (ServerInterface) registry.lookup("airlineServer");
        } catch (RemoteException | NotBoundException e) {
            throw new UnreachableServerException();
        }
        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainPanel = new JTabbedPane();
        JButton closeActiveTab = new JButton("Close the current tab");
        closeActiveTab.setPreferredSize(new Dimension(75, 25));
        closeActiveTab.addActionListener(action ->
        {
            if (mainPanel.getSelectedIndex() > 0) {
                mainPanel.removeTabAt(mainPanel.getSelectedIndex());
            } else {
                JOptionPane.showMessageDialog(null, "GoodBye!");
                mainFrame.dispose();
            }
        });
        mainFrame.setTitle("VoloOK");
        mainFrame.setSize(new Dimension(535, 615));
        mainFrame.setResizable(false);
        JPanel flightsPanel = new FlightsPanel();
        JPanel accountPanel = new AccountPanel();
        mainPanel.addTab("Flights", flightsPanel);
        mainPanel.addTab("Login/Register", accountPanel);
        mainPanel.addTab("Modify a reservation", new ModifyTabNoAccount());
        mainPanel.addTab("Browse our promotions", new PromoPanel());
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.add(closeActiveTab, BorderLayout.SOUTH);
        mainFrame.setVisible(true);

    }


    public void addTab(String title, Component component) {
        activeTabs.add(component);
        mainPanel.addTab(title, component);
        mainPanel.setSelectedComponent(component);
    }

    public void removeTab(Component component) {
        activeTabs.remove(component);
        mainPanel.remove(component);
    }

    public void setAuthInfo(UserInfo info) {
        authInfo = info;
        userId = info.userId();
    }

    public UserInfo getAuthInfo() {
        return authInfo;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isUserLoggedIn() {
        return userIsLoggedIn;
    }

    public void setUserStatus(int status) {
        userId = status;
    }

    public void userLoggedIn() {
        userIsLoggedIn = true;
    }

    public ServerInterface getServerStub() {
        return serverStub;
    }


    public static void main(String... args) throws InterruptedException {

    }


}
