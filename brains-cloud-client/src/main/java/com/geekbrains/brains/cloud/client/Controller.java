package com.geekbrains.brains.cloud.client;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.*;
import java.util.*;

public class Controller {
    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    final byte SHUTDOWN_CODE = 21;
    final byte TRANSFER_FILE_CODE = 15;
    final byte RECEIVE_FILE_CODE = 16;
    final byte GET_STORAGE_CODE = 17;
    final byte EXIT_CODE = 18;
    final byte DELETE_FILE_CODE = 19;
    final byte RENAME_FILE_CODE = 20;
    final String IP_ADRESS = "localhost";
    final int PORT = 8189;
    Scanner scanner;
    String input;
    SocketChannel socketChannel;
    FileChannel fileChannel;
    String fileName;
    Path file;
    long fileSize;
    byte[] filenameBytes;
    ByteBuffer byteBuffer;
    boolean authorized;

    public Controller() {
        scanner = new Scanner(System.in);
        byteBuffer = ByteBuffer.allocate(1024);
        startClient();
    }

    public void startClient() {
        new Thread(() -> {
            try {
                socketChannel = SocketChannel.open();
                socketChannel.connect(new InetSocketAddress(IP_ADRESS, PORT));
                while (true) {
                    if (!authorized)
                        login();
                    System.out.println("Enter command:");
                    input = scanner.next();
                    if (input.equals("end")) {
                        socketChannel.write(byteBuffer.put(SHUTDOWN_CODE).flip());
//                    socketChannel.close();
                        break;
                    }
                    switch (input) {
                        case "t":
                            transferFile();
                            break;
                        case "r":
                            receiveFile();
                            break;
                        case "s":
                            getStorage();
                            break;
                        case "d":
                            deleteFile();
                            break;
                        case "n":
                            renameFile();
                            break;
                        case "e":
                            byteBuffer.put(EXIT_CODE);
                            byteBuffer.flip();
                            socketChannel.write(byteBuffer);
                            authorized = false;
                            byteBuffer.clear();
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void login() throws IOException {
        while (true) {
            System.out.println("Enter name:");
            input = scanner.next();
            byte[] bytes = input.getBytes();
            byteBuffer.put((byte) bytes.length);
            byteBuffer.put(bytes);
            System.out.println("Enter password:");
            input = scanner.next();
            bytes = input.getBytes();
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
                authorized = true;
                break;
            } else {
                System.out.println("login or password incorrect");
            }
        }
    }

    public void transferFile() throws IOException {
        System.out.println("Enter file name: ");
        input = scanner.next();
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
    }

    private void sendMetaInf() throws IOException {
        filenameBytes = fileName.getBytes();
        System.out.println(byteBuffer);
        byteBuffer.put((byte) TRANSFER_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.putLong(fileSize);
        System.out.println("filesize: " + fileSize);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }

    public void receiveFile() throws IOException {
        System.out.println("Enter file name: ");
        input = tfFileName.getText();
        System.out.println("receiving file: " + input);
        file = Paths.get("brains-cloud-client/" + input);
        Files.createFile(file);
        fileChannel = FileChannel.open(file, StandardOpenOption.WRITE);
        fileName = input;
        sendMetaInfForReceive();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        fileSize = byteBuffer.getLong();
        byteBuffer.clear();
        while (fileChannel.size() != fileSize) {
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
            fileChannel.write(byteBuffer);
            byteBuffer.clear();
        }
        fileChannel.close();
        System.out.println("finished");
    }

    private void sendMetaInfForReceive() throws IOException {
        filenameBytes = input.getBytes();
        byteBuffer.put((byte) RECEIVE_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }

    private void deleteFile() throws IOException {
        System.out.println("Enter file name: ");
        input = scanner.next();
        filenameBytes = input.getBytes();
        byteBuffer.put((byte) DELETE_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }


    private void renameFile() throws IOException {
        System.out.println("Enter filename: ");
        input = scanner.next();
        filenameBytes = input.getBytes();
        byteBuffer.put((byte) RENAME_FILE_CODE);
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        System.out.println("Enter new filename");
        input = scanner.next();
        filenameBytes = input.getBytes();
        byteBuffer.put((byte) filenameBytes.length);
        byteBuffer.put(filenameBytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
    }

    public void getStorage() throws IOException {
        System.out.println("receiving file list from server");
        byteBuffer.put((byte) GET_STORAGE_CODE);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        byte[] b = new byte[socketChannel.read(byteBuffer)];
        byteBuffer.flip();
        for (int i = 0; i < b.length; i++) {
            b[i] = byteBuffer.get();
        }
        Platform.runLater(() -> {
            filesList.getItems().clear();
            filesList.getItems().add(new String(b));
        });
        System.out.print(new String(b));
        byteBuffer.clear();
    }
}
