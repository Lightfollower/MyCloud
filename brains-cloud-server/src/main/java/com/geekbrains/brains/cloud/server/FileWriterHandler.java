package com.geekbrains.brains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;

public class FileWriterHandler extends ChannelInboundHandlerAdapter {
    FileManager fileManager;
    File file;
    String filename;
    String userName;
    long fileSize;
    BufferedOutputStream out;

    public FileWriterHandler(FileManager fileManager, String filename, long fileSize, String username) {
        this.fileManager = fileManager;
        this.filename = filename;
        this.fileSize = fileSize;
        this.userName = username;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("File writer activated");
        file = new File(filename);
        out = new BufferedOutputStream(new FileOutputStream(file));
        System.out.println("initializing finished");
        System.out.println("file size: " + fileSize);
        if (fileSize == 0) {
            out.close();
            System.out.println("fileReceived");
            FileStorageHandler fileStorageHandler = new FileStorageHandler(fileManager, userName);
            ctx.pipeline().addLast(fileStorageHandler);
            fileStorageHandler.channelActive(ctx);
            ctx.pipeline().remove(this);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        fileSize -= buf.readableBytes();
        do {
            out.write(buf.readByte());
        } while (buf.readableBytes() > 0);
        buf.release();
        if (fileSize == 0) {
            out.close();
            System.out.println("fileReceived");
            FileStorageHandler fileStorageHandler = new FileStorageHandler(fileManager, userName);
            ctx.pipeline().addLast(fileStorageHandler);
            fileStorageHandler.channelActive(ctx);
            ctx.pipeline().remove(this);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
