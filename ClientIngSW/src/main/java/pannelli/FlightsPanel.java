package pannelli;

import command.ConcreteCommandHandler;
import command.SearchCommand;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;


public class FlightsPanel extends JPanel {

    JTextField from, to;
    JButton switchB, search;
    static JLabel info = new JLabel("Insert a departure and a destination city");
    static final int textWidth = 150, textHeight = 20;
    static final int buttonWidth = 150, buttonHeight = 20;
    JPanel searchBar, performSearch, searchResults;
    JScrollPane resultSet;

    public FlightsPanel() {
        super(new FlowLayout(FlowLayout.CENTER));
        UtilDateModel modelLeft = new UtilDateModel(), modelRight = new UtilDateModel();
        LocalDateTime dateTime = LocalDateTime.now();
        modelLeft.setDate(dateTime.getYear(),dateTime.getMonthValue()-1,dateTime.getDayOfMonth());
        dateTime = dateTime.plus(Duration.ofDays(7));
        modelRight.setDate(dateTime.getYear(),dateTime.getMonthValue()-1,dateTime.getDayOfMonth());
        modelLeft.setSelected(true);
        modelRight.setSelected(true);
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl dateLeft = new JDatePanelImpl(modelLeft, p), dateRight = new JDatePanelImpl(modelRight, p);
        searchBar = new JPanel(new GridLayout(1, 0));
        searchBar.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        performSearch = new JPanel(new FlowLayout());
        searchResults = new JPanel(new GridLayout(0, 1));
        resultSet = new JScrollPane(searchResults);
        resultSet.setPreferredSize(new Dimension(450, 250));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        from = new JTextField("ROMA");
        to = new JTextField("MILANO");
        switchB = new JButton("Switch");
        switchB.addActionListener(action -> {
            String f = from.getText();
            from.setText(to.getText());
            to.setText(f);
        });
        search = new JButton("Search");
        search.addActionListener(action -> {
            if (from.getText() == null || to.getText() == null || modelLeft.getValue() == null || modelRight.getValue() == null) {
                JOptionPane.showMessageDialog(this, "Select a departure,a destination and arrival and departure date", "Missing fields", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ConcreteCommandHandler.INSTANCE.handleCommand(new SearchCommand(
                    from.getText(), to.getText(), new Timestamp(modelLeft.getValue().toInstant().toEpochMilli()),  new Timestamp(modelRight.getValue().toInstant().toEpochMilli()), searchResults
            ));
        });
        from.setPreferredSize(new Dimension(textWidth, textHeight));
        to.setPreferredSize(new Dimension(textWidth, textHeight));
        switchB.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        from.setBorder(BasicBorders.getTextFieldBorder());
        to.setBorder(BasicBorders.getTextFieldBorder());
        switchB.setBorder(BasicBorders.getButtonBorder());
        searchBar.add(from);
        searchBar.add(switchB);
        searchBar.add(to);
        performSearch.add(dateLeft);
        performSearch.add(search);
        performSearch.add(dateRight);
        add(info);
        add(searchBar);
        add(performSearch);
        add(resultSet);
    }
}
