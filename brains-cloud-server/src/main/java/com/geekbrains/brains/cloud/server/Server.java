package com.geekbrains.brains.cloud.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


public class Server {
    final int PORT;
    Properties properties;
    public Server() throws IOException {
        File propertiesFile = new File("F:\\cloud\\MyCloud\\brains-cloud-server\\src\\main\\resources\\port.properties");
        properties = new Properties();
        properties.load(new FileReader(propertiesFile));
        PORT = Integer.parseInt(properties.getProperty("port"));
        DBService.setProperties(properties);
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            DBService.connect();
                            FileSender fileSender = new FileSender(ch);
                            ch.pipeline().addLast( new AuthorizationHandler(fileSender));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(PORT).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            DBService.disconnect();
        }
    }
}
