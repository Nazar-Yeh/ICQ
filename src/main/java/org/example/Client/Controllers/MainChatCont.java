package org.example.Client.Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.*;
import java.net.Socket;
import java.sql.*;

public class MainChatCont {
    @FXML private ListView<String> id_chat, id_contacts;
    @FXML private TextField id_message, id_search;
    @FXML private Button id_send;
    @FXML private Label id_username;

    private final ObservableList<String> allContacts = FXCollections.observableArrayList();
    private final FilteredList<String> filteredContacts = new FilteredList<>(allContacts);
    private String currentUser, selectedContact;
    private final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private final String DB_USER = "postgres";
    private final String DB_PASS = "BobrikHiHiYeh";

    public void setUsername(String username) {
        currentUser = username;
        id_username.setText(username);
    }

    @FXML
    void initialize() {
        id_contacts.setItems(filteredContacts);
        loadContacts();

        id_search.textProperty().addListener((obs, old, text) ->
                filteredContacts.setPredicate(contact ->
                        text.isEmpty() || contact.toLowerCase().contains(text.toLowerCase())
                ));

        id_contacts.getSelectionModel().selectedItemProperty().addListener((obs, old, contact) -> {
            selectedContact = contact;
            if (contact != null) loadChat();
        });

        id_send.setOnAction(e -> sendMessage());
        id_message.setOnAction(e -> sendMessage());
        startChatPolling();
    }

    private void loadContacts() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username FROM users WHERE username != '" + currentUser + "'")) {
            allContacts.clear();
            while (rs.next()) allContacts.add(rs.getString("username"));
        } catch (SQLException e) {
            System.err.println("DB error: " + e.getMessage());
        }
    }

    private void sendMessage() {
        if (selectedContact == null || id_message.getText().isEmpty()) return;

        id_chat.getItems().add("You: " + id_message.getText());
        sendToServer(id_message.getText());
        id_message.clear();
    }

    private void sendToServer(String msg) {
        try (Socket socket = new Socket("localhost", 1234);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println("SEND_MESSAGE\n" + currentUser + "\n" + selectedContact + "\n" + msg);
            if (!"MESSAGE_SENT".equals(in.readLine()))
                System.err.println("Send failed");
        } catch (IOException e) {
            System.err.println("Send error: " + e.getMessage());
        }
    }

    private void loadChat() {
        if (selectedContact == null) return;

        String query = "SELECT u1.username as sender, m.content_mes " +
                "FROM messages m " +
                "JOIN users u1 ON m.send_id = u1.id_user " +
                "JOIN users u2 ON m.recieve_id = u2.id_user " +
                "WHERE (u1.username=? AND u2.username=?) OR (u1.username=? AND u2.username=?) " +
                "ORDER BY m.sent_time";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentUser);
            stmt.setString(2, selectedContact);
            stmt.setString(3, selectedContact);
            stmt.setString(4, currentUser);

            ObservableList<String> newMessages = FXCollections.observableArrayList();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                newMessages.add(rs.getString("sender") + ": " + rs.getString("content_mes"));
            }

            Platform.runLater(() -> {
                id_chat.getItems().clear();
                id_chat.getItems().addAll(newMessages);
            });
        } catch (SQLException e) {
            System.err.println("Load chat error: " + e.getMessage());
        }
    }
    private void startChatPolling() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (selectedContact != null) {
                    loadChat();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}