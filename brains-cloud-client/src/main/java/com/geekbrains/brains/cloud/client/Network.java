package com.geekbrains.brains.cloud.client;

import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Network {
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

    Controller controller;

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
    Alert alert;

    public Network(Controller controller) throws IOException {
        this.controller = controller;
        File propertiesFile = new File("F:\\cloud\\MyCloud\\brains-cloud-client\\src\\main\\resources\\address.properties");
        properties = new Properties();
        properties.load(new FileReader(propertiesFile));
        IP_ADDRESS = properties.getProperty("ipAddress");
        PORT = Integer.parseInt(properties.getProperty("port"));
        byteBuffer = ByteBuffer.allocate(1024 * 1024 * 8);
        filesForTransfer = new ArrayList<>();
        alert = new Alert(Alert.AlertType.ERROR);
        startClient();
    }

    public void startClient() {
        new Thread(() -> {
            while (true) {
                try {
                    controller.infoLabel.setText("Connecting");
                    socketChannel = SocketChannel.open();
                    socketChannel.connect(new InetSocketAddress(IP_ADDRESS, PORT));
                    controller.infoLabel.setText("Connected");
                    break;
                } catch (IOException e) {
                    controller.infoLabel.setText("Connection refused");
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void login() {
        filenameBytes = controller.loginField.getText().getBytes();
        byteBuffer.put(LOGIN_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        filenameBytes = controller.passwordFiled.getText().getBytes();
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
            byte b = byteBuffer.get();
            byteBuffer.clear();
            if (b == 1) {
                System.out.println("Password accepted");
                controller.isAuthorized = true;
                controller.setAuthorized();
                controller.loginField.clear();
                controller.passwordFiled.clear();
                getStorage();
                controller.infoLabel.setText("");
            } else {
                System.out.println("login or password incorrect");
            }
        } catch (IOException e) {
            alert.setContentText("Server disconnected");
            alert.show();
            e.printStackTrace();
            byteBuffer.clear();
            startClient();
        }
    }


    public void register() {
        filenameBytes = controller.loginField.getText().getBytes();
        byteBuffer.put(REGISTER_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        filenameBytes = controller.passwordFiled.getText().getBytes();
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
            byte b = byteBuffer.get();
            byteBuffer.clear();
            if (b == 1) {
                System.out.println("User registered");
                controller.isAuthorized = true;
                controller.setAuthorized();
                controller.loginField.clear();
                controller.passwordFiled.clear();
                getStorage();
            } else {
                System.out.println("Login busy");
            }
        } catch (IOException e) {
            alert.setContentText("Server disconnected");
            alert.show();
            e.printStackTrace();
            byteBuffer.clear();
            startClient();
        }
    }

    public void unlogin() {
        controller.isAuthorized = false;
        controller.setAuthorized();
        byteBuffer.put(EXIT_CODE);
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            alert.setContentText("Server disconnected");
            alert.show();
            e.printStackTrace();
            startClient();
        }
        byteBuffer.clear();
    }

    public void transferFile() {
        for (String s :
                filesForTransfer) {
            file = Paths.get(s);
            try {
                fileChannel = FileChannel.open(file);
                fileSize = Files.size(file);
            } catch (IOException e) {
                alert.setContentText("File propalo: " + s);
                alert.show();
                e.printStackTrace();
                break;
            }
            fileName = file.getFileName().toString();
            try {
                sendMetaInf();
                System.out.println("transferring file: " + s);
                System.out.println("start transferring");
//                byteBuffer.clear();
                long transferred;
                long position = 0;
                while ((transferred = fileChannel.transferTo(position, fileSize, socketChannel)) != 0) {
                    position += transferred;
                }
                System.out.println("finished");
                fileChannel.close();
                getStorage();
            } catch (IOException e) {
                showAlertAndUnlogin();
                e.printStackTrace();
            }
        }
        filesForTransfer.clear();
    }

    public void sendMetaInf() throws IOException {
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

    public void receiveFile() {
        fileName = controller.filesList.getSelectionModel().getSelectedItem();
        if (fileName == null)
            return;
        file = Paths.get("brains-cloud-client/" + fileName);
        if (Files.exists(file)) {
            controller.infoLabel.setText("File already exists");
            return;
        }
        System.out.println("receiving file: " + fileName);
//        fileName = input;
        try {
            sendMetaInfForReceive();
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
            fileSize = byteBuffer.getLong();
//            byteBuffer.clear();
            try {
                Files.createFile(file);
                fileChannel = FileChannel.open(file, StandardOpenOption.WRITE);
            } catch (IOException e) {
                alert.setContentText("File propalo: " + fileName);
                e.printStackTrace();
                return;
            }
            long position = 0;
            long received = 0;
            long bytesLeft = fileSize;
            while (bytesLeft > received) {
                received = fileChannel.transferFrom(socketChannel, position, fileSize);
                position += received;
                bytesLeft -= received;
            }
            fileChannel.transferFrom(socketChannel, position, bytesLeft);
            fileChannel.close();
            System.out.println("finished");
            getStorage();
        } catch (IOException e) {
            showAlertAndUnlogin();
            e.printStackTrace();
        }
    }

    public void sendMetaInfForReceive() throws IOException {
        filenameBytes = input.getBytes();
        byteBuffer.put(RECEIVE_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }

    public void deleteFile() {
        input = controller.filesList.getSelectionModel().getSelectedItem();
        if (input == null) {
            return;
        }
        filenameBytes = input.getBytes();
        byteBuffer.put(DELETE_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            getStorage();
        } catch (IOException e) {
            showAlertAndUnlogin();
            e.printStackTrace();
            byteBuffer.clear();
        }
    }

    public void renameFile() {
        fileName = controller.filesList.getSelectionModel().getSelectedItem();
        if (input == null) {
            return;
        }
        filenameBytes = fileName.getBytes();
        byteBuffer.put(RENAME_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        controller.renameField.setVisible(true);
        controller.renameField.setManaged(true);
        controller.infoLabel.setText("Введите новое имя");
    }

    public void rename() {
        fileName = controller.renameField.getText();
        filenameBytes = input.getBytes();
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            controller.renameField.setVisible(false);
            controller.renameField.setManaged(false);
            controller.infoLabel.setText("");
            getStorage();
        } catch (IOException e) {
            showAlertAndUnlogin();
            e.printStackTrace();
            byteBuffer.clear();
        }
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
        controller.refresh(strings);

        System.out.print(new String(b));
        byteBuffer.clear();
    }

    public void shutdown() {
        try {
            socketChannel.write(byteBuffer.put(SHUTDOWN_CODE).flip());
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlertAndUnlogin() {
        alert.setContentText("Server disconnected");
        alert.show();
        unlogin();
        startClient();
    }
}
