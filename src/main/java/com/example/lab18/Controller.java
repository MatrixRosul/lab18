package com.example.lab18;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {

    @FXML
    private TextArea textArea;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @FXML
    private void handleOpenFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Відкрити текстовий файл");
        File file = fileChooser.showOpenDialog(textArea.getScene().getWindow());
        if (file != null) {
            try (Scanner scanner = new Scanner(file)) {
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine()).append("\n");
                }
                textArea.setText(content.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSaveFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти текстовий файл");
        File file = fileChooser.showSaveDialog(textArea.getScene().getWindow());
        if (file != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Параметри збереження");
            dialog.setHeaderText("Введіть кількість копій та місце збереження");
            dialog.setContentText("Кількість копій:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(input -> {
                try {
                    int numberOfCopies = Integer.parseInt(input);
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Виберіть місце збереження копій");
                    File backupLocation = directoryChooser.showDialog(textArea.getScene().getWindow());
                    if (backupLocation != null) {
                        Task<Void> saveTask = new Task<>() {
                            @Override
                            protected Void call() throws Exception {
                                saveFile(file); // Зберігаємо оригінальний файл
                                for (int i = 0; i < numberOfCopies; i++) {
                                    createBackup(file, backupLocation, i); // Створюємо резервні копії
                                }
                                return null;
                            }
                        };
                        saveTask.setOnSucceeded(e -> showSuccessMessage()); // Показуємо повідомлення про успішне збереження
                        saveTask.setOnFailed(e -> {
                            saveTask.getException().printStackTrace();
                            showErrorDialog("Помилка", "Не вдалося зберегти файл та створити резервні копії.");
                        });
                        executor.submit(saveTask);
                    }
                } catch (NumberFormatException e) {
                    showErrorDialog("Помилка", "Некоректна кількість копій. Будь ласка, введіть дійсне число.");
                }
            });
        }
    }

    private void showSuccessMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успіх");
        alert.setHeaderText(null);
        alert.setContentText("Файл успішно збережено з резервними копіями.");
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void createBackup(File originalFile, File backupLocation, int order) {
        File backupDirectory = new File(backupLocation, "резервні копії"); // Користувач вибрав папку для зберігання копій, тому додаємо "резервні копії" до цієї папки
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs(); // Створюємо директорію, якщо вона не існує
        }

        File backupFile = new File(backupDirectory, originalFile.getName() + "_" + order + "резервна копія");
        try {
            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFile(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(textArea.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
