package com.geekbrains.brains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.nio.charset.Charset;

public class FileStorageHandler extends ChannelInboundHandlerAdapter {
    final byte SHUTDOWN_CODE = 21;
    final byte TRANSFER_FILE_CODE = 15;
    final byte RECEIVE_FILE_CODE = 16;
    final byte GET_STORAGE_CODE = 17;
    final byte EXIT_CODE = 18;
    final byte DELETE_FILE_CODE = 19;
    final byte RENAME_FILE_CODE = 20;

    String userName;
    byte command;
    byte fileNameLength;
    String filename;
    String newFileName;
    long fileSize;
    ByteBuf buf;
    FileManager fileManager;
    File folder;
    File[] files;

    public FileStorageHandler(FileManager fileManager, String username) {
        this.fileManager = fileManager;
        this.userName = username;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler here " + userName);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        buf = (ByteBuf) msg;
        System.out.println("command: " + (command = buf.readByte()));

        switch (command) {
            case TRANSFER_FILE_CODE:
                receiveDataForWriteFile();
                FileWriterHandler fileWriterHandler = new FileWriterHandler(fileManager, filename, fileSize, userName);
                ctx.pipeline().addLast(fileWriterHandler);
                fileWriterHandler.channelActive(ctx);
                ctx.pipeline().remove(this);
                break;
            case RECEIVE_FILE_CODE:
                receiveDataForSendFile();
                fileManager.sendFile(ctx, filename);
                break;
            case GET_STORAGE_CODE:
                sendStorageToClient(ctx);
                break;
            case DELETE_FILE_CODE:
                receiveDataForSendFile();
                fileManager.deleteFile(filename);
                break;
            case RENAME_FILE_CODE:
                receiveDataForRenameFile();
                fileManager.renameFile(filename, newFileName);
                break;
            case EXIT_CODE:
                System.out.println("exit");
                AuthorizationHandler authorizationHandler = new AuthorizationHandler(fileManager);
                ctx.pipeline().addFirst(authorizationHandler);
                authorizationHandler.channelActive(ctx);
                ctx.pipeline().remove(this);
                break;
            case SHUTDOWN_CODE:
                System.out.println("shutdown");
                ctx.close();
                break;
        }
        buf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void receiveDataForWriteFile() {
        System.out.println("filename length = " + (fileNameLength = buf.readByte()));
        System.out.println("filename: " + (filename = userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
        System.out.println("file size: " + (fileSize = buf.readLong()));
    }

    private void receiveDataForSendFile() {
        System.out.println("filename length = " + (fileNameLength = buf.readByte()));
        System.out.println("filename: " + (filename = userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
    }

    private void receiveDataForRenameFile() {
        System.out.println("filename length = " + (fileNameLength = buf.readByte()));
        System.out.println("filename: " + (filename = userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
        System.out.println("filename length = " + (fileNameLength = buf.readByte()));
        System.out.println("new filename: " + (newFileName = userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
    }

    private void sendStorageToClient(ChannelHandlerContext ctx) {
        folder = new File("/cloud/myCloud/" + userName);
        files = folder.listFiles();
        StringBuilder stringBuilder = new StringBuilder();
        ByteBuf buf = ctx.alloc().buffer(2048);
        if(files.length == 0) {
            buf.writeBytes("empty\n".getBytes());
            ctx.writeAndFlush(buf);
            return;
        }
        for (File f :
                files) {
            System.out.println(f.getName());
            if (f.isFile())
                stringBuilder.append(f.getName() + System.lineSeparator());
        }
        buf.writeBytes(stringBuilder.toString().getBytes());
        ctx.writeAndFlush(buf);
    }
}