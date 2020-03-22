package com.geekbrains.brains.cloud.client;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Controller implements Initializable {
    final byte SHUTDOWN_CODE = 21;
    final byte TRANSFER_FILE_CODE = 15;
    final byte RECEIVE_FILE_CODE = 16;
    final byte GET_STORAGE_CODE = 17;
    final byte EXIT_CODE = 18;
    final byte DELETE_FILE_CODE = 19;
    final byte RENAME_FILE_CODE = 20;
    final byte LOGIN_CODE = 21;
    final byte REGISTER_CODE = 22;
    final String IP_ADDRESS;
    final int PORT;

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

    Properties properties;
    List<String> filesForTransfer;
    String input;
    SocketChannel socketChannel;
    FileChannel fileChannel;
    String fileName;
    Path file;
    long fileSize;
    byte[] filenameBytes;
    ByteBuffer byteBuffer;
    boolean isAuthorized;
    CountDownLatch countDownLatch;

    public Controller() throws IOException {
        File propertiesFile = new File("F:\\cloud\\MyCloud\\brains-cloud-client\\src\\main\\resources\\address.properties");
        properties = new Properties();
        properties.load(new FileReader(propertiesFile));
        IP_ADDRESS = properties.getProperty("ipAddress");
        PORT = Integer.parseInt(properties.getProperty("port"));
        byteBuffer = ByteBuffer.allocate(1024);
        filesForTransfer = new ArrayList<>();
        startClient();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeDragAndDropLabel();
    }

    public void startClient() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(IP_ADDRESS, PORT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void login() throws IOException {
        byte[] bytes = loginField.getText().getBytes();
        byteBuffer.put(LOGIN_CODE);
        byteBuffer.put((byte) bytes.length);
        byteBuffer.put(bytes);
        bytes = passwordFiled.getText().getBytes();
        byteBuffer.put((byte) bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        byte b = byteBuffer.get();
        byteBuffer.clear();
        if (b == 1) {
            System.out.println("Password accepted");
            isAuthorized = true;
            setAuthorized();
            loginField.clear();
            passwordFiled.clear();
            getStorage();
        } else {
            System.out.println("login or password incorrect");
        }
    }

    public void register() throws IOException {
        byte[] bytes = loginField.getText().getBytes();
        byteBuffer.put(REGISTER_CODE);
        byteBuffer.put((byte) bytes.length);
        byteBuffer.put(bytes);
        bytes = passwordFiled.getText().getBytes();
        byteBuffer.put((byte) bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        byte b = byteBuffer.get();
        byteBuffer.clear();
        if (b == 1) {
            System.out.println("User registered");
            isAuthorized = true;
            setAuthorized();
            loginField.clear();
            passwordFiled.clear();
            getStorage();
        } else {
            System.out.println("Login busy");
        }
    }

    public void unlogin() throws IOException {
        isAuthorized = false;
        setAuthorized();
        byteBuffer.put(EXIT_CODE);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }

    public void transferFile() throws IOException {
        for (String s :
                filesForTransfer) {
            input = s;
            file = Paths.get(input);
            fileChannel = FileChannel.open(file);
            fileSize = Files.size(file);
            fileName = file.getFileName().toString();
            sendMetaInf();
            System.out.println("transferring file: " + input);
            System.out.println("start transferring");
            byteBuffer.clear();
            int n = fileChannel.read(byteBuffer);
            do {
                byteBuffer.flip();
                socketChannel.write(byteBuffer);
                byteBuffer.clear();
                n = fileChannel.read(byteBuffer);
            }
            while (n > -1);
            byteBuffer.clear();
            System.out.println("finished");
            fileChannel.close();
            getStorage();
        }
        filesForTransfer.clear();
    }

    private void sendMetaInf() throws IOException {
        filenameBytes = fileName.getBytes();
        byteBuffer.put(TRANSFER_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.putLong(fileSize);
        System.out.println("filesize: " + fileSize);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }

    public void receiveFile() throws IOException {
        input = filesList.getSelectionModel().getSelectedItem();
        if (input == null) {
            return;
        }
        System.out.println("receiving file: " + input);
        file = Paths.get("brains-cloud-client/" + input);
        fileName = input;
        sendMetaInfForReceive();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        fileSize = byteBuffer.getLong();
        byteBuffer.clear();
        Files.createFile(file);
        fileChannel = FileChannel.open(file, StandardOpenOption.WRITE);
        while (fileChannel.size() != fileSize) {
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
            fileChannel.write(byteBuffer);
            byteBuffer.clear();
        }
        fileChannel.close();
        System.out.println("finished");
        getStorage();
    }

    private void sendMetaInfForReceive() throws IOException {
        filenameBytes = input.getBytes();
        byteBuffer.put(RECEIVE_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }

    public void deleteFile() throws IOException {
        input = filesList.getSelectionModel().getSelectedItem();
        if (input == null) {
            return;
        }
        filenameBytes = input.getBytes();
        byteBuffer.put(DELETE_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        getStorage();
    }


    public void renameFile() throws IOException, InterruptedException {
        countDownLatch = new CountDownLatch(1);
        input = filesList.getSelectionModel().getSelectedItem();
        if (input == null) {
            return;
        }
        filenameBytes = input.getBytes();
        byteBuffer.put(RENAME_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Rename.fxml"));
            Parent root = loader.load();
            RenameController lc = loader.getController();
            lc.id = 100;
            lc.backController = this;

            stage.setTitle("Enter new name");
            stage.setScene(new Scene(root, 400, 200));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
        countDownLatch.await();
        filenameBytes = input.getBytes();
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        getStorage();
    }

    public void getStorage() throws IOException {
        System.out.println("receiving file list from server");
        byteBuffer.put(GET_STORAGE_CODE);
        byteBuffer.flip();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        byte[] b = new byte[socketChannel.read(byteBuffer)];
        byteBuffer.flip();
        for (int i = 0; i < b.length; i++) {
            b[i] = byteBuffer.get();
        }
        String[] strings = new String(b).split(System.lineSeparator());
        Platform.runLater(() -> {
            filesList.getItems().clear();
            filesList.getItems().addAll(strings);
        });
        System.out.print(new String(b));
        byteBuffer.clear();
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
        socketChannel.write(byteBuffer.put(SHUTDOWN_CODE).flip());
        socketChannel.close();
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
                        filesForTransfer.add(o.getAbsolutePath());
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
}
