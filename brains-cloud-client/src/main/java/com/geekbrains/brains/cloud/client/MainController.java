package com.geekbrains.brains.cloud.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            tfFileName.clear();
        }
    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map((Path p) -> {
                    return p.getFileName().toString();
                }).forEach(o -> {
                    filesList.getItems().add(o);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
