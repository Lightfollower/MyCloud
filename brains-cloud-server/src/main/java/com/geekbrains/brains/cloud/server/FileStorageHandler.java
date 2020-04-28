package com.geekbrains.brains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.Charset;

public class FileStorageHandler extends ChannelInboundHandlerAdapter {
    protected static final Logger LOGGER = LogManager.getLogger(FileStorageHandler.class);
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
        LOGGER.info("Handler here " + userName);
        sendStorageToClient(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        buf = (ByteBuf) msg;
        LOGGER.info("command: " + (command = buf.readByte()));

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
                sendStorageToClient(ctx);
                break;
            case RENAME_FILE_CODE:
                receiveDataForRenameFile();
                fileManager.renameFile(filename, newFileName);
                sendStorageToClient(ctx);
                break;
            case EXIT_CODE:
                LOGGER.info("exit");
                AuthorizationHandler authorizationHandler = new AuthorizationHandler(fileManager);
                ctx.pipeline().addFirst(authorizationHandler);
                authorizationHandler.channelActive(ctx);
                ctx.pipeline().remove(this);
                break;
            case SHUTDOWN_CODE:
                LOGGER.info("shutdown");
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
        LOGGER.info("filename length = " + (fileNameLength = buf.readByte()));
        LOGGER.info("filename: " + (filename = "server storage/" + userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
        LOGGER.info("file size: " + (fileSize = buf.readLong()));
    }

    private void receiveDataForSendFile() {
        LOGGER.info("filename length = " + (fileNameLength = buf.readByte()));
        LOGGER.info("filename: " + (filename = "server storage/" + userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
    }

    private void receiveDataForRenameFile() {
        LOGGER.info("filename length = " + (fileNameLength = buf.readByte()));
        LOGGER.info("filename: " + (filename = "server storage/" + userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
        LOGGER.info("filename length = " + (fileNameLength = buf.readByte()));
        LOGGER.info("new filename: " + (newFileName = "server storage/" + userName + "/" + buf.readCharSequence(fileNameLength, Charset.defaultCharset()).toString()));
    }

    private void sendStorageToClient(ChannelHandlerContext ctx) {
        folder = new File("server storage/" + userName);
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