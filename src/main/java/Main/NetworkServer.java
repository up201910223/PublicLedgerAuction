package main.java.Main;

import main.java.KademliaDHT.Node;
import main.java.KademliaDHT.ServerDHT;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class NetworkServer: Represents a Netty-based UDP server node handler.
 */
public class NetworkServer implements Runnable {
    private Channel channel;

    private static final Logger LOGGER = Logger.getLogger(NetworkServer.class.getName());
    private final int listenPort;
    private final Node localNode;

    /**
     * Creates a new NetworkServer instance for the given port and node.
     *
     * @param listenPort Port to bind the server to.
     * @param localNode  Node associated with this server.
     */
    public NetworkServer(int listenPort, Node localNode) {
        this.listenPort = listenPort;
        this.localNode = localNode;
    }

    /**
     * Entry point to start the server thread.
     */
    @Override
    public void run() {
        EventLoopGroup loopGroup = new NioEventLoopGroup();

        try {
            launch(loopGroup);
        } catch (Exception e) {
            throw new RuntimeException("Server failed to start", e);
        } finally {
            loopGroup.shutdownGracefully();
        }
    }

    /**
     * Initializes and binds the Netty UDP server.
     *
     * @param loopGroup Netty event loop group.
     * @throws Exception if binding or execution fails.
     */
    public void launch(EventLoopGroup loopGroup) throws Exception {
        Bootstrap boot = new Bootstrap();
        boot.group(loopGroup)
            .channel(NioDatagramChannel.class)
            .option(ChannelOption.SO_BROADCAST, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .handler(new ChannelInitializer<NioDatagramChannel>() {
                @Override
                protected void initChannel(NioDatagramChannel ch) {
                    ch.pipeline().addLast(new ServerDHT(localNode));
                }
            });

        int retries = 3;
        int attempts = 0;
        int retryDelay = 5;

        while (attempts < retries) {
            try {
                ChannelFuture future = boot.bind(listenPort).sync();
        this.channel = future.channel();
                LOGGER.info("Server active on port " + listenPort);
                future.channel().closeFuture().sync();
                break;
            } catch (Exception e) {
                attempts++;
                LOGGER.warning("Bind failed on port " + listenPort + ". Retrying in " + retryDelay + "s.");
                TimeUnit.SECONDS.sleep(retryDelay);
            }
        }

        if (attempts >= retries) {
            LOGGER.severe("Server failed to start after " + retries + " attempts.");
        }
    }
    /** Expose the bound channel for outbound sends */
    public io.netty.channel.Channel getChannel() {
        return channel;
    }
}
