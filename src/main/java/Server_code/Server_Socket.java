package Server_code;

import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class Server_Socket {
    private static final int PORT = 1234;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "BobrikHiHiYeh";

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestType = in.readLine();
            if (requestType == null) return;

            switch (requestType) {
                case "CHECK_USER":
                    String username = in.readLine();
                    if (checkUser(username)) {
                        out.println("OK");
                    } else {
                        out.println("NOT_FOUND");
                    }
                    break;
                case "SEND_MESSAGE":
                    String sender = in.readLine();
                    String receiver = in.readLine();
                    String message = in.readLine();
                    if (saveMessage(sender, receiver, message)) {
                        out.println("MESSAGE_SENT");
                    } else {
                        out.println("MESSAGE_FAILED");
                    }
                    break;
                case "GET_CONVERSATIONS":
                    sendConversations(out);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static boolean checkUser(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) return false;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE username ILIKE ?")) {
            stmt.setString(1, username.trim());
            return stmt.executeQuery().next();
        }
    }

    private static boolean saveMessage(String sender, String receiver, String message) throws SQLException {
        if (sender == null || receiver == null || message == null) return false;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO messages (send_id, recieve_id, content_mes) " +
                             "VALUES ((SELECT id_user FROM users WHERE username ILIKE ?), " +
                             "(SELECT id_user FROM users WHERE username ILIKE ?), ?)")) {
            stmt.setString(1, sender.trim());
            stmt.setString(2, receiver.trim());
            stmt.setString(3, message);
            return stmt.executeUpdate() > 0;
        }
    }

    private static void sendConversations(PrintWriter out) throws SQLException {
        Map<String, List<String>> conversations = new TreeMap<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT u1.username as sender, u2.username as receiver, m.content_mes " +
                             "FROM messages m JOIN users u1 ON m.send_id = u1.id_user " +
                             "JOIN users u2 ON m.recieve_id = u2.id_user ORDER BY m.sent_time")) {

            while (rs.next()) {
                String user1 = rs.getString("sender").trim();
                String user2 = rs.getString("receiver").trim();

                String chatKey;
                if (user1.compareTo(user2) < 0) {
                    chatKey = user1 + " <-> " + user2;
                } else {
                    chatKey = user2 + " <-> " + user1;
                }

                String msg = user1 + ": " + rs.getString("content_mes");
                conversations.computeIfAbsent(chatKey, k -> new ArrayList<>()).add(msg);
            }
        }

        out.println("CONVERSATIONS_OK");
        out.println(conversations.size());
        for (Map.Entry<String, List<String>> entry : conversations.entrySet()) {
            out.println(entry.getKey());
            out.println(entry.getValue().size());
            entry.getValue().forEach(out::println);
        }
    }
}