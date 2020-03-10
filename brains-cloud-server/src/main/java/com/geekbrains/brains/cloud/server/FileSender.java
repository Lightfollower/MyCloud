package com.geekbrains.brains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.io.*;

public class FileSender {
    Channel channel;
    File file;
    long fileSize;
    FileInputStream fileInputStream;
    BufferedInputStream bufferedInputStream;

    public FileSender(Channel channel) {
        this.channel = channel;
    }

    public void sendFile(ChannelHandlerContext ctx, String filename) throws Exception {
        file = new File(filename);
        fileInputStream = new FileInputStream(file);
        bufferedInputStream = new BufferedInputStream(fileInputStream);
        fileSize = file.length();
        ByteBuf buf = ctx.alloc().buffer(1024);
        buf.writeLong(fileSize);
        ctx.writeAndFlush(buf);

        FileRegion region = new DefaultFileRegion(fileInputStream.getChannel(), 0, file.length());
        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        transferOperationFuture.addListener(
                (ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        System.out.println("Файл успешно передан");
                        fileInputStream.close();
                    }
                });
    }

    public void deleteFile(String filename) {
        file = new File(filename);
        file.delete();
    }

    public void renameFile(String filename, String newFileName) {
        file = new File(filename);
        file.renameTo(new File(newFileName));
    }
}
