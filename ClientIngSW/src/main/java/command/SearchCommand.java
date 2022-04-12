package command;

import exceptions.UnreachableServerException;
import pannelli.DisplayFlightInfo;
import pannelli.ParteGrafica;
import client.Utils;

import lombok.AllArgsConstructor;
import server.entities.Volo;

import javax.swing.*;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.List;

@AllArgsConstructor
public class SearchCommand implements Command {
    private String from, to;
    private Timestamp fromD, toD;
    private JPanel displayResults;

    @Override
    public void execute() {
        try {
            List<Volo> voli = ParteGrafica.INSTANCE. getServerStub().getVoli(Utils.dateToTimestamp(fromD), Utils.dateToTimestamp(toD), from, to);
            displayResults.removeAll();
            if (voli != null)
                voli.forEach(volo -> {
                    displayResults.add(new DisplayFlightInfo(volo, true));
                });
            else displayResults.add(new JLabel("No matches with the selected criteria"));
            displayResults.revalidate();
            displayResults.repaint();
        } catch (RemoteException e) {
            throw new UnreachableServerException();
        }

    }

}
