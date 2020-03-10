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
        file = new File(filename);
        out = new BufferedOutputStream(new FileOutputStream(file, true));
        System.out.println("initializing finished");
        System.out.println("file size: " + fileSize);
        //        Клиент не отправляет файл, ждёт подтверждения готовности к приёму.
        byteBuf.writeByte(3);
        ctx.writeAndFlush(byteBuf);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf buf = (ByteBuf) msg;
        try {
            fileSize -= buf.readableBytes();
            do {
                out.write(buf.readByte());
            } while (buf.readableBytes() > 0);
            buf.release();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fileSize == 0) {
            out.close();
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
