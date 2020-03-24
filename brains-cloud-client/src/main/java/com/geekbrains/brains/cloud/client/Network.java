package com.geekbrains.brains.cloud.client;

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

    public Network(Controller controller) throws IOException {
        this.controller = controller;
        File propertiesFile = new File("F:\\cloud\\MyCloud\\brains-cloud-client\\src\\main\\resources\\address.properties");
        properties = new Properties();
        properties.load(new FileReader(propertiesFile));
        IP_ADDRESS = properties.getProperty("ipAddress");
        PORT = Integer.parseInt(properties.getProperty("port"));
        byteBuffer = ByteBuffer.allocate(1024 * 1024 * 8);
        filesForTransfer = new ArrayList<>();
        startClient();
    }

    public void startClient() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(IP_ADDRESS, PORT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void login() throws IOException {
        byte[] bytes = controller.loginField.getText().getBytes();
        byteBuffer.put(LOGIN_CODE);
        byteBuffer.put((byte) bytes.length);
        byteBuffer.put(bytes);
        bytes = controller.passwordFiled.getText().getBytes();
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
            controller.isAuthorized = true;
            controller.setAuthorized();
            controller.loginField.clear();
            controller.passwordFiled.clear();
            getStorage();
        } else {
            System.out.println("login or password incorrect");
        }
    }

    public void register() throws IOException {
        byte[] bytes = controller.loginField.getText().getBytes();
        byteBuffer.put(REGISTER_CODE);
        byteBuffer.put((byte) bytes.length);
        byteBuffer.put(bytes);
        bytes = controller.passwordFiled.getText().getBytes();
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
            controller.isAuthorized = true;
            controller.setAuthorized();
            controller.loginField.clear();
            controller.passwordFiled.clear();
            getStorage();
        } else {
            System.out.println("Login busy");
        }
    }

    public void unlogin() throws IOException {
        controller.isAuthorized = false;
        controller.setAuthorized();
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
            long transferred;
            long position = 0;
            while ((transferred = fileChannel.transferTo(position, fileSize, socketChannel)) != 0)
            {
                position += transferred;
            }
            System.out.println("finished");
            fileChannel.close();
            getStorage();
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

    public void receiveFile() throws IOException {
        input = controller.filesList.getSelectionModel().getSelectedItem();
        if (input == null)
            return;
        file = Paths.get("brains-cloud-client/" + input);
        if (Files.exists(file)) {
            controller.infoLabel.setText("File already exists");
            return;
        }
        System.out.println("receiving file: " + input);
        fileName = input;
        sendMetaInfForReceive();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        fileSize = byteBuffer.getLong();
        byteBuffer.clear();

        Files.createFile(file);
        fileChannel = FileChannel.open(file, StandardOpenOption.WRITE);
        long position = 0;
        long received = 0;
        long bytesLeft = fileSize;
        while (bytesLeft > received){
            received = fileChannel.transferFrom(socketChannel, position, fileSize);
            position += received;
            bytesLeft -= received;
        }
        fileChannel.transferFrom(socketChannel, position, bytesLeft);
        fileChannel.close();
        System.out.println("finished");
        getStorage();
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

    public void deleteFile() throws IOException {
        input = controller.filesList.getSelectionModel().getSelectedItem();
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

    public void renameFile() {
        input = controller.filesList.getSelectionModel().getSelectedItem();
        if (input == null) {
            return;
        }
        filenameBytes = input.getBytes();
        byteBuffer.put(RENAME_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        controller.renameField.setVisible(true);
        controller.renameField.setManaged(true);
        controller.infoLabel.setText("Введите новое имя");
    }

    public void rename() throws IOException {
        input = controller.renameField.getText();
        filenameBytes = input.getBytes();
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        controller.renameField.setVisible(false);
        controller.renameField.setManaged(false);
        controller.infoLabel.setText("");
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
        controller.refresh(strings);

        System.out.print(new String(b));
        byteBuffer.clear();
    }

    public void shutdown() throws IOException {
        socketChannel.write(byteBuffer.put(SHUTDOWN_CODE).flip());
        socketChannel.close();
    }
}
