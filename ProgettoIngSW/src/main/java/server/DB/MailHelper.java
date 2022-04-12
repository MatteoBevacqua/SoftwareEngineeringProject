package server.DB;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MailHelper {
    private DAOAbstraction database;
    private static String user = "volokosystem@gmail.com", password = "volokosystem1.-", smtpServer = "smtp.gmail.com";

    private ScheduledExecutorService scheduledExecutorService;

    public MailHelper(DAOAbstraction database) {
        this.database = database;
        scheduledExecutorService = new ScheduledThreadPoolExecutor(4);
        scheduledExecutorService.scheduleAtFixedRate(scheduledNotifier, 15, 5, TimeUnit.MINUTES);
        scheduledExecutorService.scheduleAtFixedRate(expiredBookings, 15, 45, TimeUnit.MINUTES);
        scheduledExecutorService.scheduleAtFixedRate(newPromotion, 15, 2, TimeUnit.HOURS);
        scheduledExecutorService.scheduleAtFixedRate(nonLoyalClients, 15, 14, TimeUnit.DAYS);
    }

    public void stopHelper(){
        scheduledExecutorService.shutdown();
    }

    private static Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port",587);
        return Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                    }
                });

    }
    private final Runnable scheduledNotifier = () -> {
        try {
            List<String[]> emails = database.getEmailsToNotify();
            Session session = getSession();
            emails.forEach(mail -> {
                try {
                    MimeMessage message = new MimeMessage(session);
                    message.setSubject("Airline Reservation System");
                    message.setFrom(new InternetAddress(user));
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(mail[0]));
                    message.setText("Your reservation for the flight #? no longer be modified,please check in".replaceAll("\\?", mail[1]));
                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            });

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    };
    private final Runnable  newPromotion = () -> {
        Session session = getSession();
        try {
            AbstractMap.SimpleEntry<Collection<String>, Collection<String>> entry = database.getEmailsForPromotions();
            Collection<String> emails = entry.getKey(), promoTexts = entry.getValue();
            emails.forEach(mail -> {
                promoTexts.forEach(promoText -> {
                    try {
                        MimeMessage message = new MimeMessage(session);
                        message.setSubject("Airline Reservation System - New Promotion");
                        message.setFrom(new InternetAddress(user));
                        message.addRecipient(Message.RecipientType.TO, new InternetAddress(mail));
                        message.setText("Dear user,\ncheck out our new Promotion \n" + promoText);
                        Transport.send(message);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                });
            });

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    };
    private final Runnable nonLoyalClients = () -> {
        try {
            List<String> emails = database.getEmailsNonLoyalCLients();
            Session session = getSession();
            for (String email : emails) {
                MimeMessage message = new MimeMessage(session);
                message.setSubject("Airline Reservation System - Loyalty status lost");
                message.setFrom(new InternetAddress(user));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                message.setText("As of now your loyaly member status has been removed since you haven't booked a flight for more than 2 years.");
                Transport.send(message);
            }
        } catch (SQLException | MessagingException throwables) {
            throwables.printStackTrace();
        }

    };

    private final Runnable expiredBookings = () -> {
        try {
            List<String> emails = database.getEmailForExpiringReservations();
            Session session = getSession();
            emails.forEach(mail -> {
                try {
                    MimeMessage message = new MimeMessage(session);
                    message.setSubject("Airline Reservation System - Expired Booking");
                    message.setFrom(new InternetAddress(user));
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(mail));
                    message.setText("Your reservation expired since you haven't checked-in");
                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            });

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    };

    public void sendEmail(String to, String message) {
        try {
            Session session = getSession();
            MimeMessage msg = new MimeMessage(session);
            msg.setSubject("VoloOK Reservation System");
            msg.setFrom(new InternetAddress(user));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setText(message);
            Transport.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }}
