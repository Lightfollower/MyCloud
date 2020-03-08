package com.geekbrains.brains.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

public class AuthorizationHandler extends ChannelInboundHandlerAdapter {
    ByteBuf buf;
    String login;
    String password;
    Boolean authorized;
    FileSender fileSender;

    public AuthorizationHandler(FileSender fileSender) {
        this.fileSender = fileSender;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        buf = (ByteBuf) msg;
        byte b = buf.readByte();
        System.out.println("login: " + (login = buf.readCharSequence(b, Charset.defaultCharset()).toString()));
        b = buf.readByte();
        System.out.println("password: " + (password = buf.readCharSequence(b, Charset.defaultCharset()).toString()));
        authorized = DBService.verify(login, password);
        buf.clear();
        buf.writeByte(authorized ? (byte) 1 : (byte) 2);
        ctx.writeAndFlush(buf);
        buf.clear();
        if (authorized) {
            FileStorageHandler fileStorageHandler = new FileStorageHandler(fileSender, login);
            fileStorageHandler.channelActive(ctx);
            ctx.pipeline().addFirst(fileStorageHandler);
            ctx.pipeline().remove(this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
