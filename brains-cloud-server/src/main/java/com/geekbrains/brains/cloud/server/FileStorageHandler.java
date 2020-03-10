package com.geekbrains.brains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.nio.charset.Charset;

public class FileStorageHandler extends ChannelInboundHandlerAdapter {
    final int TRANSFER_FILE_CODE = 15;
    final int RECEIVE_FILE_CODE = 16;
    final int GET_STORAGE_CODE = 17;
    final int EXIT_CODE = 18;
    final int DELETE_FILE_CODE = 19;
    final int RENAME_FILE_CODE = 20;

    String userName;
    byte command;
    byte fileNameLength;
    String filename;
    String newFileName;
    long fileSize;
    ByteBuf buf;
    FileSender fileSender;
    File folder;
    File[] files;

    public FileStorageHandler(FileSender fileSender, String username) {
        this.fileSender = fileSender;
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
                FileWriterHandler fileWriterHandler = new FileWriterHandler(fileSender, filename, fileSize, userName);
                ctx.pipeline().addLast(fileWriterHandler);
                fileWriterHandler.channelActive(ctx);
                ctx.pipeline().remove(this);
                break;
            case RECEIVE_FILE_CODE:
                receiveDataForSendFile();
                fileSender.sendFile(ctx, filename);
                break;
            case GET_STORAGE_CODE:
                sendStorageToClient(ctx);
                break;
            case DELETE_FILE_CODE:
                receiveDataForSendFile();
                fileSender.deleteFile(filename);
                break;
            case RENAME_FILE_CODE:
                receiveDataForRenameFile();
                fileSender.renameFile(filename, newFileName);
                break;
            case EXIT_CODE:
                System.out.println("exit");
                AuthorizationHandler authorizationHandler = new AuthorizationHandler(fileSender);
                ctx.pipeline().addFirst(authorizationHandler);
                authorizationHandler.channelActive(ctx);
                ctx.pipeline().remove(this);
                break;
        }
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
        for (File f :
                files) {
            if (f.isFile())
                stringBuilder.append(f.getName() + "\n");
        }
        ByteBuf buf = ctx.alloc().buffer(2048);
        buf.writeBytes(stringBuilder.toString().getBytes());
        ctx.writeAndFlush(buf);
        System.out.println(stringBuilder.toString());
    }
}