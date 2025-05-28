package main.java.KademliaDHT;

import main.java.Auctions.Auction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import main.java.KademliaDHT.Kademlia.MsgType;

/**
 * UDP Server handler for incoming Kademlia messages
 * Responsible for deserializing packets, dispatching message handlers
 * and sending responses/acknowledgments
 */
public class ServerDHT extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(ServerDHT.class.getName());

    private final Node myNode;
    private static final int K = 2;  // Number of closest nodes to return in FIND_NODE

    public ServerDHT(Node node) {
        this.myNode = node;
    }

    /**
     * Called when a packet is received
     * Decodes packet, identifies message type, and delegates to specific handlers
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, ClassNotFoundException {
        System.out.println("Server received a packet!");
        if (!(msg instanceof DatagramPacket packet)) {
            logger.warning("Received unknown message type: " + msg.getClass().getName());
            return;
        }

        ByteBuf buffer = packet.content();

        // Read message type enum ordinal
        Kademlia.MsgType messageType = Kademlia.MsgType.values()[buffer.readInt()];
        logger.info("Received " + messageType + " packet from: " + packet.sender());

        // Read random request ID (used to match requests and responses)
        int randomIdLength = buffer.readInt();
        byte[] randomId = new byte[randomIdLength];
        buffer.readBytes(randomId);

        InetSocketAddress sender = packet.sender();

        // Dispatch by message type
        switch (messageType) {
            case FIND_NODE, FIND_VALUE -> handleFindNodeOrValue(ctx, buffer, messageType, randomId, sender);
            case PING -> handlePing(ctx, buffer, messageType, randomId, sender);
            case STORE -> handleStore(ctx, buffer, messageType, randomId, sender);
            case NOTIFY -> handleNotify(ctx, buffer, messageType, randomId, sender);
            case LATEST_BLOCK -> handleLatestBlock(ctx, buffer, messageType, randomId, sender);
            case NEW_AUCTION -> handleNewAuction(ctx, buffer, messageType, randomId, sender);
            case AUCTION_UPDATE -> handleAuctionUpdate(ctx, buffer, messageType, randomId, sender);
            default -> logger.warning("Received unknown message type: " + messageType);
        }

        buffer.release();
    }

    /**
     * Handles FIND_NODE and FIND_VALUE messages
     * For FIND_VALUE, attempts to return stored value; otherwise returns closest nodes
     */
    private void handleFindNodeOrValue(ChannelHandlerContext ctx, ByteBuf buffer, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) throws IOException, ClassNotFoundException {
        int nodeInfoLength = buffer.readInt();
        ByteBuf nodeInfoBytes = buffer.readBytes(nodeInfoLength);
        NodeInfo nodeInfo = (NodeInfo) Utils.deserialize(nodeInfoBytes);

        if (messageType == Kademlia.MsgType.FIND_VALUE) {
            if (handleFindValue(ctx, buffer, nodeInfo, messageType, randomId, sender)) {
                nodeInfoBytes.release();
                return;  // Value found and response sent
            }
            // Value not found, fallback to FIND_NODE logic
            messageType = Kademlia.MsgType.FIND_NODE;
        }

        logger.info("Received node info: " + nodeInfo);
        myNode.updateRoutingTable(nodeInfo);
        //myNode.updateRoutingTable(new NodeInfo(sender.getAddress().getHostAddress(), sender.getPort()));

        List<NodeInfo> closestNodes = Utils.findClosestNodes(myNode.getRoutingTable(), nodeInfo.getNodeId(), K);
        closestNodes.add(myNode.getNodeInfo()); // Include self node info in response

        sendSerializedResponse(ctx, messageType, randomId, sender, closestNodes,
                "Sent closest nodes info to " + sender);

        nodeInfoBytes.release();
    }

    /**
     * Handles FIND_VALUE message specifically by trying to locate the value
     * Returns true if value found and response sent, false otherwise
     */
    private boolean handleFindValue(ChannelHandlerContext ctx, ByteBuf buffer, NodeInfo requesterNode,
                                    Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) throws IOException {
        int keyLength = buffer.readInt();
        String key = buffer.readCharSequence(keyLength, StandardCharsets.UTF_8).toString();

        logger.info("Received FIND_VALUE request for key: " + key + " from node: " + requesterNode);

        Object value = myNode.findValueByKey(key);
        if (value != null) {
            sendSerializedResponse(ctx, messageType, randomId, sender, value,
                    "Responded with stored value for key: " + key);
            return true;
        }
        return false;
    }

    /**
     * Handles STORE messages to store a key-value pair in the local node
     */
    private void handleStore(ChannelHandlerContext ctx, ByteBuf buffer, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) throws IOException, ClassNotFoundException {
        int keyLength = buffer.readInt();
        String key = buffer.readCharSequence(keyLength, StandardCharsets.UTF_8).toString();

        int valueLength = buffer.readInt();
        ByteBuf valueBytes = buffer.readBytes(valueLength);
        Object value = Utils.deserialize(valueBytes);

        logger.info("STORE request: key=" + key + ", value=" + value);

        // If value is an Auction, set the stored node ID before storing
        if (value instanceof Auction auction) {
            auction.setStoredNodeId(myNode.getNodeInfo().getNodeId());
        }

        myNode.storeKeyValue(key, value);
        logger.info("Stored key-value pair: " + key);

        sendAck(ctx, messageType, randomId, sender);
    }

    /**
     * Handles PING messages by responding with an acknowledgment
     */
    private void handlePing(ChannelHandlerContext ctx, ByteBuf buffer, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) {
        int pingMsgLength = buffer.readInt();
        ByteBuf pingMsgBytes = buffer.readBytes(pingMsgLength);
        String pingMsg = pingMsgBytes.toString(StandardCharsets.UTF_8);

        logger.info("Received PING: " + pingMsg + " from " + sender);
        myNode.updateRoutingTable(new NodeInfo(sender.getAddress().getHostAddress(), sender.getPort()));

        sendAck(ctx, messageType, randomId, sender);

        pingMsgBytes.release();
    }

    /**
     * Handles NOTIFY messages which announce a new block hash
     */
    private void handleNotify(ChannelHandlerContext ctx, ByteBuf buffer, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) {
        int blockHashLength = buffer.readInt();
        String blockHash = buffer.readCharSequence(blockHashLength, StandardCharsets.UTF_8).toString();

        logger.info("Received NOTIFY for new block hash: " + blockHash + " from " + sender);

        sendAck(ctx, messageType, randomId, sender);

        Kademlia.getInstance().notifyNewBlockHash(myNode.getNodeInfo(), myNode.getRoutingTable(), blockHash);
        //myNode.updateRoutingTable(new NodeInfo(sender.getAddress().getHostAddress(), sender.getPort()));

    }

    /**
     * Handles LATEST_BLOCK requests by returning the latest block hash
     */
    private void handleLatestBlock(ChannelHandlerContext ctx, ByteBuf buffer, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) {
        int requestLength = buffer.readInt();
        String request = buffer.readCharSequence(requestLength, StandardCharsets.UTF_8).toString();

        logger.info("Received LATEST_BLOCK request: " + request + " from " + sender);

        ByteBuf response = ctx.alloc().buffer();

        response.writeInt(messageType.ordinal());
        response.writeInt(randomId.length);
        response.writeBytes(randomId);

        String latestBlockHash = Kademlia.getInstance().getLatestBlockHash().toString();
        byte[] blockHashBytes = latestBlockHash.getBytes(StandardCharsets.UTF_8);

        response.writeInt(blockHashBytes.length);
        response.writeBytes(blockHashBytes);

        //myNode.updateRoutingTable(new NodeInfo(sender.getAddress().getHostAddress(), sender.getPort()));

        Utils.sendPacket(ctx, response, sender, messageType,
                "Sent latest block hash to " + sender);
    
    }

    /**
     * Handles NEW_AUCTION messages announcing a new auction
     */
    private void handleNewAuction(ChannelHandlerContext ctx, ByteBuf buffer, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) throws IOException, ClassNotFoundException {
        int auctionIdLength = buffer.readInt();
        String auctionId = buffer.readCharSequence(auctionIdLength, StandardCharsets.UTF_8).toString();

        logger.info("New auction created with ID " + auctionId + ", notified by " + sender);

        sendAck(ctx, messageType, randomId, sender);

        Kademlia.getInstance().broadcastNewAuction(myNode.getNodeInfo(), myNode.getRoutingTable(), auctionId);
    }

    /**
     * Handles AUCTION_UPDATE messages which update bids or subscribers on an auction
     */
    private void handleAuctionUpdate(ChannelHandlerContext ctx, ByteBuf buffer, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) {
        try {
            int auctionIdLength = buffer.readInt();
            String auctionId = buffer.readCharSequence(auctionIdLength, StandardCharsets.UTF_8).toString();

            int updateLength = buffer.readInt();
            ByteBuf updateBytes = buffer.readBytes(updateLength);

            Object stored = myNode.findValueByKey(auctionId);

            if (stored instanceof Auction auction) {
                //myNode.updateRoutingTable(new NodeInfo(sender.getAddress().getHostAddress(), sender.getPort()));
                try {
                    Double bid = (Double) Utils.deserialize(updateBytes.copy());
                    auction.setCurrentBid(bid);
                    // TODO: update current bidder info if needed
                } catch (Exception e) {
                    // Treat updateBytes as subscriber info string if not a bid
                    String subscriber = updateBytes.toString(StandardCharsets.UTF_8);
                    auction.addSubscriber(subscriber);
                }
                myNode.storeKeyValue(auctionId, auction);
            }
            else {
                logger.warning("Invalid value type for auction ID: " + auctionId);
            }

            try {
                Double bid = (Double) Utils.deserialize(updateBytes.copy());
                logger.info("New bid " + bid + " on auction " + auctionId + " from " + sender);
            } catch (Exception e) {
                String closeMsg = updateBytes.toString(StandardCharsets.UTF_8);
                logger.info(closeMsg + " notified by " + sender);
            }

            sendAck(ctx, messageType, randomId, sender);

            updateBytes.release();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to handle AUCTION_UPDATE", ex);
        }
    }

    /**
     * Sends a serialized response message back to the sender
     */
    private void sendSerializedResponse(ChannelHandlerContext ctx, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender,
                                        Object msg, String successLog) throws IOException {
        ByteBuf response = ctx.alloc().buffer();

        response.writeInt(messageType.ordinal());

        response.writeInt(randomId.length);
        response.writeBytes(randomId);

        ByteBuf serializedMsg = Utils.serialize(msg);
        response.writeInt(serializedMsg.readableBytes());
        response.writeBytes(serializedMsg);

        Utils.sendPacket(ctx, response, sender, messageType, successLog);
    }

    /**
     * Sends a simple acknowledgment message
     */
    private void sendAck(ChannelHandlerContext ctx, Kademlia.MsgType messageType, byte[] randomId, InetSocketAddress sender) {
        ByteBuf ackMsg = ctx.alloc().buffer();

        ackMsg.writeInt(messageType.ordinal());

        ackMsg.writeInt(randomId.length);
        ackMsg.writeBytes(randomId);

        String ack = messageType + "ACK";
        ByteBuf ackBuf = Unpooled.wrappedBuffer(ack.getBytes(StandardCharsets.UTF_8));
        ackMsg.writeInt(ackBuf.readableBytes());
        ackMsg.writeBytes(ackBuf);

        Utils.sendPacket(ctx, ackMsg, sender, messageType, "Sent " + messageType + "ACK to " + sender);
    }
}
