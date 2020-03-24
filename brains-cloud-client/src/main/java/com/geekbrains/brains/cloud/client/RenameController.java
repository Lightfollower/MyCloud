package com.geekbrains.brains.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RenameController {
    @FXML
    TextField newName;

    @FXML
    VBox globParent;

    public int id;

    public Controller backController;

    public void rename(ActionEvent actionEvent) {
        backController.network.input = newName.getText();
        backController.countDownLatch.countDown();
        System.out.println(newName.getText());
        globParent.getScene().getWindow().hide();
    }
}
