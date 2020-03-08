package com.geekbrains.brains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;

public class FileWriterHandler extends ChannelInboundHandlerAdapter {
    FileSender fileSender;
    File file;
    String filename;
    String userName;
    long fileSize;
    BufferedOutputStream out;

    public FileWriterHandler(FileSender fileSender, String filename, long fileSize, String username) {
        this.fileSender = fileSender;
        this.filename = filename;
        this.fileSize = fileSize;
        this.userName = username;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("File writer activated");
        ByteBuf byteBuf = ctx.alloc().buffer(1024);
//        Клиент не отправляет файл, ждёт подтверждения готовности к приёму.
        byteBuf.writeByte(3);
        ctx.writeAndFlush(byteBuf);
        file = new File(filename);
        System.out.println("initializing finished");
        System.out.println("file size: " + fileSize);
        out = new BufferedOutputStream(new FileOutputStream(file, true));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf buf = (ByteBuf) msg;
        try {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
            }
            buf.release();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (file.length() == fileSize) {
            System.out.println("fileReceived");
            FileStorageHandler fileStorageHandler = new FileStorageHandler(fileSender, userName);
            ctx.pipeline().addLast(fileStorageHandler);
            try {
                fileStorageHandler.channelActive(ctx);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ctx.pipeline().remove(this);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
