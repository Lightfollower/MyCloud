package com.geekbrains.brains.cloud.client;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        deleteFileBtn.disableProperty().bind(simpleBooleanProperty);
        receiveFileBtn.disableProperty().bind(simpleBooleanProperty);
        renameFileBtn.disableProperty().bind(simpleBooleanProperty);
        initializeDragAndDropLabel();
        try {
            network = new Network(this);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("File properties propalo");
            System.exit(1);
        }
    }


    public void login() {
        network.login();
    }

    public void register() {
        network.register();
    }

    public void unlogin() {
        network.unlogin();
    }

    public void transferFile() {
        network.transferFile();
    }

    public void receiveFile() {
        network.receiveFile();
    }

    public void deleteFile() {
        network.deleteFile();
    }

    public void renameFile() {
        network.renameFile();
    }

    public void rename() {
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

    public void shutdown() {
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
                transferFile();
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
