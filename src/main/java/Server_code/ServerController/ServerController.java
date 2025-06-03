package Server_code.ServerController;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import java.io.*;
import java.net.Socket;

public class ServerController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private ListView<String> id_chat_server;

    @FXML
    private ListView<String> id_serer_mes;

    private Map<String, List<String>> conversations = new HashMap<>();
    private ObservableList<String> chatList = FXCollections.observableArrayList();
    private ObservableList<String> messageList = FXCollections.observableArrayList();
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    @FXML
    void initialize() {
        assert id_chat_server != null : "fx:id=\"id_chat_server\" was not injected: check your FXML file 'ServerInterface.fxml'.";
        assert id_serer_mes != null : "fx:id=\"id_serer_mes\" was not injected: check your FXML file 'ServerInterface.fxml'.";

        id_chat_server.setItems(chatList);
        id_serer_mes.setItems(messageList);

        id_chat_server.setOnMouseClicked(event -> {
            String selectedChat = id_chat_server.getSelectionModel().getSelectedItem();
            messageList.clear();
            if (selectedChat != null) {
                messageList.addAll(conversations.get(selectedChat));
            }
        });

        fetchConversationsFromServer();
        startServerPolling();
    }

    private void fetchConversationsFromServer() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_CONVERSATIONS");
            String response = in.readLine();
            if (!"CONVERSATIONS_OK".equals(response)) {
                System.err.println("Error fetching conversations: " + response);
                return;
            }

            Map<String, List<String>> newConversations = new HashMap<>();
            List<String> newChatList = new ArrayList<>();
            int conversationCount = Integer.parseInt(in.readLine());
            for (int i = 0; i < conversationCount; i++) {
                String participants = in.readLine();
                int messageCount = Integer.parseInt(in.readLine());
                List<String> messages = new ArrayList<>();
                for (int j = 0; j < messageCount; j++) {
                    messages.add(in.readLine());
                }
                newConversations.put(participants, messages);
                newChatList.add(participants);
            }

            Platform.runLater(() -> {
                String selectedChat = id_chat_server.getSelectionModel().getSelectedItem();

                conversations.clear();
                conversations.putAll(newConversations);
                chatList.clear();
                chatList.addAll(newChatList);

                if (selectedChat != null && chatList.contains(selectedChat)) {
                    id_chat_server.getSelectionModel().select(selectedChat);
                    messageList.clear();
                    messageList.addAll(conversations.get(selectedChat));
                } else if (!chatList.isEmpty()) {
                    id_chat_server.getSelectionModel().select(0);
                }
            });
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    private void startServerPolling() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                fetchConversationsFromServer();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}