package main.java.KademliaDHT;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles outbound DHT messages and incoming responses within a Kademlia-like distributed system.
 */
public class ClientDHT extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = Logger.getLogger(ClientDHT.class.getName());

    private NodeInfo localNode;                  // Node sending the message
    private NodeInfo remoteNode;                 // Node receiving the message
    private List<NodeInfo> neighbors;            // Known neighboring nodes
    private KademliaDHT.Kademlia.MsgType type;           // Type of message being sent
    private String messageKey;                   // Key involved in the message (if applicable)
    private ValueWrapper messageValue;           // Value associated with the key (for STORE, etc.)
    private Timer timeoutTimer;                  // Timer to handle response timeouts
    private byte[] identifier;                   // Unique identifier for this message

    public ClientDHT(NodeInfo localNode, NodeInfo remoteNode, String messageKey,
                     ValueWrapper messageValue, KademliaDHT.Kademlia.MsgType type, List<NodeInfo> neighbors) {
        this.localNode = localNode;
        this.remoteNode = remoteNode;
        this.messageKey = messageKey;
        this.messageValue = messageValue;
        this.type = type;
        this.neighbors = neighbors;
    }

    /**
     * Triggered when the channel becomes active – sends the appropriate message based on type.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws IOException {
        ByteBuf buffer = ctx.alloc().buffer();

        buffer.writeInt(type.ordinal());  // Write message type
        identifier = Utils.generateRandomId();
        buffer.writeInt(identifier.length);
        buffer.writeBytes(identifier);   // Write unique message identifier

        String logMessage;

        // Handle different message types
        switch (type) {
            case FIND_NODE, FIND_VALUE -> {
                appendNodeInfo(buffer);
                if (type == KademliaDHT.Kademlia.MsgType.FIND_VALUE) {
                    writeKey(buffer);
                    logMessage = "Sent key: " + messageKey + " and node info to " + formatAddress(remoteNode);
                } else {
                    logMessage = "Sent node info to " + formatAddress(remoteNode);
                }
            }
            case PING -> {
                writeString(buffer, "PING");
                logMessage = "Pinging " + formatAddress(remoteNode);
            }
            case STORE -> {
                writeKey(buffer);
                writeSerializedValue(buffer, messageValue.getValue());
                logMessage = "STORE request for key: " + messageKey + " with value " + messageValue.getValue() +
                             " to " + formatAddress(remoteNode);
            }
            case NOTIFY -> {
                writeKey(buffer);
                logMessage = "Notifying new block hash: " + messageKey + " to " + formatAddress(remoteNode);
            }
            case LATEST_BLOCK -> {
                writeString(buffer, "LATEST_BLOCK");
                logMessage = "Requesting latest block from " + formatAddress(remoteNode);
            }
            case NEW_AUCTION -> {
                writeKey(buffer);
                logMessage = "Announced new auction with ID " + messageKey + " to " + formatAddress(remoteNode);
            }
            case AUCTION_UPDATE -> {
                writeKey(buffer);
                logMessage = processAuctionUpdate(buffer);
            }
            default -> {
                LOG.warning("Unhandled message type: " + type);
                return;
            }
        }

        // Send the composed message
        Utils.sendPacket(ctx, buffer, new InetSocketAddress(remoteNode.getIpAddr(), remoteNode.getPort()), type, logMessage);

        // Start timeout timer (except for NEW_AUCTION which doesn't expect a reply)
        if (type != KademliaDHT.Kademlia.MsgType.NEW_AUCTION) {
            startTimeout();
        }
    }

    /**
     * Handles responses received on this channel.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object received) throws IOException, ClassNotFoundException {
        if (type != KademliaDHT.Kademlia.MsgType.NEW_AUCTION) {
            cancelTimeout();  // Cancel timeout on valid response
        }

        if (received instanceof DatagramPacket packet) {
            ByteBuf content = packet.content();
            type = KademliaDHT.Kademlia.MsgType.values()[content.readInt()]; // Read message type

            byte[] receivedId = new byte[content.readInt()];
            content.readBytes(receivedId);

            // Validate response identifier
            if (!Arrays.equals(identifier, receivedId)) {
                LOG.warning("Mismatched message ID — possible spoofing.");
            }

            // Delegate based on message type
            switch (type) {
                case FIND_NODE, FIND_VALUE -> handleNodeSearch(ctx, content);
                case PING, STORE, NOTIFY, NEW_AUCTION, AUCTION_UPDATE -> handleAcknowledgment(ctx, content);
                case LATEST_BLOCK -> handleBlockInfo(ctx, content);
                default -> LOG.warning("Unhandled message type on read: " + type);
            }

            content.release(); // Clean up buffer
        } else {
            LOG.warning("Unexpected message received: " + received.getClass().getSimpleName());
        }
    }

    /**
     * Processes search responses (FIND_NODE or FIND_VALUE).
     */
    private void handleNodeSearch(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException, ClassNotFoundException {
        ByteBuf serialized = buffer.readBytes(buffer.readInt());
        Object data = Utils.deserialize(serialized);

        if (type == KademliaDHT.Kademlia.MsgType.FIND_VALUE) {
            messageValue.setValue(data);
            LOG.info("Fetched value: " + data + " from " + ctx.channel().remoteAddress());
        } else if (data instanceof ArrayList<?> list && !list.isEmpty() && list.get(0) instanceof NodeInfo) {
            @SuppressWarnings("unchecked")
            ArrayList<NodeInfo> nodes = (ArrayList<NodeInfo>) list;
            neighbors.addAll(nodes);
            LOG.info("Received neighbor nodes: " + nodes);
        } else {
            LOG.warning("Invalid data type received during FIND_NODE.");
        }
        serialized.release();
    }

    /**
     * Handles acknowledgment messages (PING, STORE, etc.)
     */
    private void handleAcknowledgment(ChannelHandlerContext ctx, ByteBuf buffer) {
        String ackMessage = buffer.readBytes(buffer.readInt()).toString(StandardCharsets.UTF_8);
        LOG.info("Received acknowledgment: " + ackMessage + " from " + ctx.channel().remoteAddress());
    }

    /**
     * Handles response for latest block hash request.
     */
    private void handleBlockInfo(ChannelHandlerContext ctx, ByteBuf buffer) {
        String blockHash = buffer.readBytes(buffer.readInt()).toString(StandardCharsets.UTF_8);
        Kademlia.getInstance().setLatestBlockHash(blockHash);
        LOG.info("Latest block hash received: " + blockHash + " from " + ctx.channel().remoteAddress());
    }

    // Helper method to serialize and send the key string
    private void writeKey(ByteBuf buf) {
        buf.writeInt(messageKey.length());
        buf.writeCharSequence(messageKey, StandardCharsets.UTF_8);
    }

    // Helper to serialize and append local node's information
    private void appendNodeInfo(ByteBuf buf) throws IOException {
        ByteBuf nodeData = Utils.serialize(localNode);
        buf.writeInt(nodeData.readableBytes());
        buf.writeBytes(nodeData);
    }

    // Helper to serialize any object and write it to buffer
    private void writeSerializedValue(ByteBuf buf, Object value) throws IOException {
        ByteBuf serialized = Utils.serialize(value);
        buf.writeInt(serialized.readableBytes());
        buf.writeBytes(serialized);
    }

    // Writes a UTF-8 encoded string into the buffer
    private void writeString(ByteBuf buf, String content) {
        ByteBuf strBuf = Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8));
        buf.writeInt(strBuf.readableBytes());
        buf.writeBytes(strBuf);
    }

    // Processes auction update messages
    private String processAuctionUpdate(ByteBuf buf) throws IOException {
        if (messageValue.getValue() instanceof String updateText) {
            ByteBuf updateBuf = Unpooled.wrappedBuffer(updateText.getBytes(StandardCharsets.UTF_8));
            buf.writeInt(updateBuf.readableBytes());
            buf.writeBytes(updateBuf);
            return "Auction update sent to " + formatAddress(remoteNode);
        } else {
            writeSerializedValue(buf, messageValue.getValue());
            return "Bid value " + messageValue.getValue() + " sent to " + formatAddress(remoteNode);
        }
    }

    // Starts a timeout timer
    private void startTimeout() {
        timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                LOG.warning("Timeout waiting for " + type + " response.");
            }
        }, 5000);
    }

    // Cancels the timeout timer if still active
    private void cancelTimeout() {
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }

    // Formats node's address as IP:Port string
    private String formatAddress(NodeInfo node) {
        return node.getIpAddr() + ":" + node.getPort();
    }

    /**
     * Handles exceptions thrown during network operations.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.log(Level.SEVERE, "Client exception", cause);
        ctx.close();
    }
}
