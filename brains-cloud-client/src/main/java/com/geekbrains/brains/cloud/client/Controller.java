package com.geekbrains.brains.cloud.client;


import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Controller implements Initializable {
    Network network;
    @FXML
    ListView<String> filesList;

    @FXML
    HBox authPanel;

    @FXML
    HBox receivePanel;

    @FXML
    HBox transferPanel;

    @FXML
    HBox exitPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordFiled;

    @FXML
    Label filesDragAndDrop;

    @FXML
    Label infoLabel;

    @FXML
    TextField renameField;

    @FXML
    Button receiveFileBtn;
    @FXML
    Button deleteFileBtn;
    @FXML
    Button renameFileBtn;
    SimpleBooleanProperty simpleBooleanProperty = new SimpleBooleanProperty(false);

    boolean isAuthorized;
    CountDownLatch countDownLatch;

    public Controller() throws IOException {
        network = new Network(this);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        deleteFileBtn.disableProperty().bind(simpleBooleanProperty);
        receiveFileBtn.disableProperty().bind(simpleBooleanProperty);
        renameFileBtn.disableProperty().bind(simpleBooleanProperty);
        initializeDragAndDropLabel();
    }


    public void login() throws IOException {
        network.login();
    }

    public void register() throws IOException {
        network.register();
    }

    public void unlogin() throws IOException {
        network.unlogin();
    }

    public void transferFile() throws IOException {
        network.transferFile();
    }

    public void receiveFile() throws IOException {
        network.receiveFile();
    }

    public void deleteFile() throws IOException {
        network.deleteFile();
    }

    public void renameFile() throws IOException, InterruptedException {
        simpleBooleanProperty.set(true);
        network.renameFile();
    }

    public void rename() throws IOException {
        network.rename();
        simpleBooleanProperty.set(false);
    }

    public void setAuthorized() {
        if (!isAuthorized) {
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            receivePanel.setVisible(false);
            receivePanel.setManaged(false);
            transferPanel.setVisible(false);
            transferPanel.setManaged(false);
            exitPanel.setVisible(false);
            exitPanel.setManaged(false);
        } else {
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            receivePanel.setVisible(true);
            receivePanel.setManaged(true);
            transferPanel.setVisible(true);
            transferPanel.setManaged(true);
            exitPanel.setVisible(true);
            exitPanel.setManaged(true);
        }
    }

    public void shutdown() throws IOException {
        network.shutdown();
    }

    private void initializeDragAndDropLabel() {
        filesDragAndDrop.setOnDragOver(event -> {
            if (event.getGestureSource() != filesDragAndDrop && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        filesDragAndDrop.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                filesDragAndDrop.setText("");
                for (File o : db.getFiles()) {
                    network.filesForTransfer.add(o.getAbsolutePath());
                }
                try {
                    transferFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                filesDragAndDrop.setText("Drop files here!");
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    public void refresh(String[] strings) {
        Platform.runLater(() -> {
            filesList.getItems().clear();
            filesList.getItems().addAll(strings);
        });
    }
}
