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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Network {
    protected static final Logger LOGGER = LogManager.getLogger(Network.class);
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
    SocketChannel socketChannel;
    FileChannel fileChannel;
    String fileName;
    Path file;
    long fileSize;
    byte[] tempTextBytes;
    ByteBuffer byteBuffer;
    Alert alert;

    public Network(Controller controller) throws IOException {
        this.controller = controller;
        Path path = Paths.get("client storage/");
        if (!Files.exists(path))
            Files.createDirectory(path);
//        File propertiesFile = new File(this.getClass().getClassLoader().getResource("address.properties").getFile());
        File propertiesFile = new File("address.properties");
        properties = new Properties();
        LOGGER.info("Try to load properties");
        properties.load(new FileReader(propertiesFile));
        LOGGER.info("properties loaded");
        IP_ADDRESS = properties.getProperty("ipAddress");
        PORT = Integer.parseInt(properties.getProperty("port"));
        byteBuffer = ByteBuffer.allocate(1024 * 1024 * 8);
        filesForTransfer = new ArrayList<>();
        alert = new Alert(Alert.AlertType.ERROR);
        startClient();
    }

    public void startClient() {
        LOGGER.info("Staring client");
        Thread thread = new Thread(() -> {
            int count = 0;
            while (true) {
                try {
                    LOGGER.info("Try to connect");
                    controller.infoLabel.setText("Connecting");
                    socketChannel = SocketChannel.open();
                    LOGGER.info("Channel opened");
                    socketChannel.connect(new InetSocketAddress(IP_ADDRESS, PORT));
                    LOGGER.info("Connected to server");
                    controller.infoLabel.setText("Connected");
                    break;
                } catch (IOException e) {
                    controller.infoLabel.setText("Connection refused");
                    e.printStackTrace();
                    LOGGER.error(e.getMessage());
                    try {
                        if (count < 5) {
                            Thread.sleep(1000);
                        } else if (count < 10) {
                            Thread.sleep(5000);
                        } else if (count < 15) {
                            Thread.sleep(20000);
                        } else {
                            Thread.sleep(30000);
                        }
                        count++;
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void login() {
        LOGGER.info("Try to login");
        try {
            byte response = requestServerForAuth(LOGIN_CODE);
            if (response == 1) {
                LOGGER.info("Password accepted");
                System.out.println("Password accepted");
                setAuthorized();
            } else {
                LOGGER.info("Access denied");
                System.out.println("login or password incorrect");
            }
        } catch (IOException e) {
            restartClientAfterException(e);
        }
    }

    public void register() {
        LOGGER.info("Try to register");
        try {
            byte response = requestServerForAuth(REGISTER_CODE);
            if (response == 1) {
                LOGGER.info("User registered");
                System.out.println("User registered");
                setAuthorized();
            } else {
                LOGGER.info("Login busy");
                System.out.println("Login busy");
            }
        } catch (IOException e) {
            restartClientAfterException(e);
        }
    }

    private byte requestServerForAuth(byte commandCode) throws IOException {
        tempTextBytes = controller.loginField.getText().getBytes();
        byteBuffer.put(commandCode);
        byteBuffer.put((byte) tempTextBytes.length);
        byteBuffer.put(tempTextBytes);
        tempTextBytes = controller.passwordFiled.getText().getBytes();
        byteBuffer.put((byte) tempTextBytes.length);
        byteBuffer.put(tempTextBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        LOGGER.info("Waiting for response");
        byteBuffer.clear();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        byte b = byteBuffer.get();
        byteBuffer.clear();
        return b;
    }

    private void setAuthorized() throws IOException {
        controller.isAuthorized = true;
        controller.setAuthorized();
        controller.loginField.clear();
        controller.passwordFiled.clear();
        getStorage();
        controller.infoLabel.setText("");
    }

    private void restartClientAfterException(IOException e) {
        LOGGER.error(e.getMessage());
        alert.setContentText("Server disconnected");
        alert.show();
        e.printStackTrace();
        byteBuffer.clear();
        startClient();
    }

    public void logOut() {
        LOGGER.info("Log out");
        byteBuffer.clear();
        controller.isAuthorized = false;
        controller.setAuthorized();
        byteBuffer.put(EXIT_CODE);
        byteBuffer.flip();
        controller.renameField.setManaged(false);
        controller.renameField.setVisible(false);
        controller.simpleBooleanProperty.set(false);
        controller.renameField.clear();
        try {
            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            restartClientAfterException(e);
        }
        byteBuffer.clear();
    }

    public void transferFile() {
        for (String s :
                filesForTransfer) {
            LOGGER.info("Transferring file: " + s);
            file = Paths.get(s);
            try {
                fileChannel = FileChannel.open(file);
                fileSize = Files.size(file);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                alert.setContentText("File propalo: " + s);
                alert.show();
                e.printStackTrace();
                continue;
            }
            fileName = file.getFileName().toString();
            try {
                sendMetaInf();
                System.out.println("transferring file: " + s);
                System.out.println("start transferring");
                long transferred;
                long position = 0;
                while ((transferred = fileChannel.transferTo(position, fileSize, socketChannel)) != 0) {
                    position += transferred;
                }
                LOGGER.info("File transferred: " + s);
                System.out.println("finished");
                fileChannel.close();
                getStorage();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                logOut();
                e.printStackTrace();
            }
        }
        filesForTransfer.clear();
    }

    public void sendMetaInf() throws IOException {
        LOGGER.info("Sending metadata for transferring");
        tempTextBytes = fileName.getBytes();
        byteBuffer.put(TRANSFER_FILE_CODE);
        byteBuffer.put((byte) tempTextBytes.length);
        byteBuffer.put(tempTextBytes);
        byteBuffer.putLong(fileSize);
        LOGGER.info("file size = " + fileSize);
        System.out.println("file size: " + fileSize);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        LOGGER.info("Metadata send");
    }

    public void receiveFile() {
        fileName = controller.filesList.getSelectionModel().getSelectedItem();
        if (fileName == null)
            return;
        LOGGER.info("Receiving file: " + fileName);
        file = Paths.get("client storage/" + fileName);
        if (Files.exists(file)) {
            LOGGER.info("File already exist");
            controller.infoLabel.setText("File already exists");
            return;
        }
        System.out.println("receiving file: " + fileName);
        try {
            sendMetaInfForReceive();
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
            fileSize = byteBuffer.getLong();
            LOGGER.info("File size = " + fileSize);
            byteBuffer.clear();
            try {
                Files.createFile(file);
                fileChannel = FileChannel.open(file, StandardOpenOption.WRITE);
                LOGGER.info("File created");
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
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
            LOGGER.info("File received: " + fileName);
            System.out.println("finished");
            getStorage();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            logOut();
            e.printStackTrace();
        }
    }

    public void sendMetaInfForReceive() throws IOException {
        LOGGER.info("Sending metadata for receive");
        tempTextBytes = fileName.getBytes();
        byteBuffer.put(RECEIVE_FILE_CODE);
        byteBuffer.put((byte) tempTextBytes.length);
        byteBuffer.put(tempTextBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        LOGGER.info("Metadata send");
    }

    public void deleteFile() {
        fileName = controller.filesList.getSelectionModel().getSelectedItem();
        if (fileName == null) {
            return;
        }
        LOGGER.info("Deleting file: " + fileName);
        tempTextBytes = fileName.getBytes();
        byteBuffer.put(DELETE_FILE_CODE);
        byteBuffer.put((byte) tempTextBytes.length);
        byteBuffer.put(tempTextBytes);
        byteBuffer.flip();
        try {
            LOGGER.info("Sending metadata for delete");
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            getStorage();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            logOut();
            e.printStackTrace();
            byteBuffer.clear();
        }
    }

    public void renameFile() {
        fileName = controller.filesList.getSelectionModel().getSelectedItem();
        if (fileName == null) {
            System.out.println("exit");
            return;
        }
        LOGGER.info("Renaming file: " + fileName);
        controller.simpleBooleanProperty.set(true);
        tempTextBytes = fileName.getBytes();
        byteBuffer.put(RENAME_FILE_CODE);
        byteBuffer.put((byte) tempTextBytes.length);
        byteBuffer.put(tempTextBytes);
        controller.renameField.setVisible(true);
        controller.renameField.setManaged(true);
        controller.infoLabel.setText("Введите новое имя");
    }

    public void rename() {
        fileName = controller.renameField.getText();
        tempTextBytes = fileName.getBytes();
        byteBuffer.put((byte) tempTextBytes.length);
        byteBuffer.put(tempTextBytes);
        byteBuffer.flip();
        try {
            LOGGER.info("Sending metadata for rename");
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            controller.renameField.setVisible(false);
            controller.renameField.setManaged(false);
            controller.infoLabel.setText("");
            controller.renameField.clear();
            getStorage();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            logOut();
            e.printStackTrace();
            byteBuffer.clear();
        }
    }

    public void getStorage() throws IOException {
        LOGGER.info("receiving file list from server");
        System.out.println("receiving file list from server");
        byteBuffer.put(GET_STORAGE_CODE);
        byteBuffer.flip();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.info("Sending request for file list");
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
            LOGGER.info("Sending request for exit");
            socketChannel.write(byteBuffer.put(SHUTDOWN_CODE).flip());
            socketChannel.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
