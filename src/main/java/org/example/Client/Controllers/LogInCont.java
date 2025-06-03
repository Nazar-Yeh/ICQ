package org.example.Client.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LogInCont {
    private static final int PORT = 1234;
    private static final String DEFAULT_IP = "localhost";

    @FXML private TextField IP_id;
    @FXML private Button Next_Id;
    @FXML private TextField User_name_id;

    @FXML
    void initialize() {
        IP_id.setText(DEFAULT_IP);
        Next_Id.setOnAction(event -> handleLogin());
        User_name_id.setOnAction(event -> handleLogin());
    }

    private void handleLogin() {
        String ip = IP_id.getText().trim();
        String username = User_name_id.getText().trim();

        if (username.isEmpty()) {
            showAlert("Помилка", "Введіть ім'я користувача!");
            return;
        }

        try (Socket socket = new Socket(ip, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("CHECK_USER");
            out.println(username);

            String response = in.readLine();

            if ("OK".equals(response)) {
                loadMainView(username);
            } else if ("NOT_FOUND".equals(response)) {
                showAlert("Помилка", "Користувача не знайдено!");
            } else {
                showAlert("Помилка", "Невідома відповідь від сервера: " + response);
            }

        } catch (IOException e) {
            showAlert("Помилка підключення", "Не вдалося підключитись до сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMainView(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Interface.fxml"));
            Parent root = loader.load();

            MainChatCont mainController = loader.getController();
            mainController.setUsername(username);

            Stage stage = (Stage) Next_Id.getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("YehTelegram - " + username);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            showAlert("Помилка", "Не вдалося завантажити головний інтерфейс.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}