package main.java.KademliaDHT;

import main.java.Auctions.Auction;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static main.java.BlockChain.Constants.GENESIS_PREV_HASH;
import static main.java.KademliaDHT.Utils.findClosestNodes;

public class Kademlia {

    private static final Logger LOGGER = Logger.getLogger(Kademlia.class.getName());

    private static final int BUCKET_SIZE = 2; // Max number of nodes in a k-bucket
    private static Kademlia singletonInstance; // Singleton instance of this class

    private StringBuilder currentBlockHash; // Latest known block hash
    private StringBuilder currentAuctionId; // ID of the currently broadcasted auction

    // Message types handled in this implementation
    public enum MsgType {
        PING, FIND_NODE, FIND_VALUE, STORE, NOTIFY, LATEST_BLOCK, NEW_AUCTION, AUCTION_UPDATE
    }

    private Kademlia() {
        this.currentBlockHash = new StringBuilder(GENESIS_PREV_HASH);
        this.currentAuctionId = null;
    }

    // Returns the singleton instance of Kademlia
    public static Kademlia getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new Kademlia();
        }
        return singletonInstance;
    }

    /**
     * Bootstraps a node into the DHT network using a known node ID.
     */
    public void joinNetwork(Node selfNode, String bootstrapNodeId) {
        LOGGER.info("Connecting to bootstrap node...");

        // Perform an initial lookup for the bootstrap node
        List<NodeInfo> initialNearNodes = findNode(selfNode.getNodeInfo(), bootstrapNodeId, selfNode.getRoutingTable());

        // Ask the bootstrap node for the latest block
        connectAndHandle(selfNode.getNodeInfo(), selfNode.findNodeInfoById(bootstrapNodeId), null, null, MsgType.LATEST_BLOCK);

        //ping(selfNode.getNodeInfo(), bootstrapNodeId, selfNode.getRoutingTable()); //Im hoping this fixes the issue with the routing table not updating properly

        // Remove bootstrap node from table temporarily to avoid duplication
        selfNode.getRoutingTable().remove(selfNode.findNodeInfoById(bootstrapNodeId));

        // Expand knowledge by querying nodes found
        for (NodeInfo nearNode : initialNearNodes) {
            selfNode.updateRoutingTable(nearNode);
            List<NodeInfo> extendedNearNodes = findNode(selfNode.getNodeInfo(), nearNode.getNodeId(), selfNode.getRoutingTable());

            // Iteratively expand the node's neighborhood
            while (!extendedNearNodes.isEmpty()) {
                List<NodeInfo> tempNearNodes = new ArrayList<>();
                for (NodeInfo extNearNode : extendedNearNodes) {
                    if (!selfNode.getRoutingTable().contains(extNearNode)) {
                        selfNode.updateRoutingTable(extNearNode);
                        tempNearNodes.addAll(findNode(selfNode.getNodeInfo(), extNearNode.getNodeId(), selfNode.getRoutingTable()));
                    }
                }
                extendedNearNodes = tempNearNodes;
            }
        }
    }

    /**
     * Attempts to find a node by ID within the routing set or via network communication.
     */
    public List<NodeInfo> findNode(NodeInfo selfInfo, String targetId, Set<NodeInfo> routingSet) {
        LOGGER.info("FIND_NODE in progress...");

        // Direct match in routing table
        for (NodeInfo node : routingSet) {
            if (node.getNodeId().equals(targetId)) {
                LOGGER.info("Target node found: " + node);
                return (List<NodeInfo>) connectAndHandle(selfInfo, node, null, null, MsgType.FIND_NODE);
            }
        }

        // Search closest known nodes
        List<NodeInfo> closestNodes = findClosestNodes(routingSet, targetId, BUCKET_SIZE);
        List<NodeInfo> collectedNodes = new ArrayList<>();

        for (NodeInfo closest : closestNodes) {
            collectedNodes.addAll((List<NodeInfo>) connectAndHandle(selfInfo, closest, null, null, MsgType.FIND_NODE));
        }

        // Check if any of the new nodes match
        for (NodeInfo node : collectedNodes) {
            if (node.getNodeId().equals(targetId)) {
                LOGGER.info("Target node found in collected nodes: " + node);
                return (List<NodeInfo>) connectAndHandle(selfInfo, node, null, null, MsgType.FIND_NODE);
            }
        }

        LOGGER.info("Node not located");
        return collectedNodes;
    }

    /**
     * Sends a ping message to a target node.
     */
    public void ping(NodeInfo selfInfo, String targetId, Set<NodeInfo> routingSet) {
        LOGGER.info("PING in progress...");
        for (NodeInfo node : routingSet) {
            if (node.getNodeId().equals(targetId)) {
                LOGGER.info("Node located: " + node);
                connectAndHandle(selfInfo, node, null, null, MsgType.PING);
                return;
            }
        }
        LOGGER.info("Ping failed - node not found");
    }

    /**
     * Attempts to find a value by key, starting with the local store.
     */
    public Object findValue(Node localNode, String key) {
        LOGGER.info("FIND_VALUE in progress...");
        Object valueFound = localNode.findValueByKey(key);
        if (valueFound != null) {
            LOGGER.info("Value found locally: " + valueFound);
            return valueFound;
        }

        // Otherwise, search network
        findNode(localNode.getNodeInfo(), key, localNode.getRoutingTable());

        List<NodeInfo> nearbyNodes = findClosestNodes(localNode.getRoutingTable(), key, BUCKET_SIZE);
        for (NodeInfo nearNode : nearbyNodes) {
            Object value = connectAndHandle(localNode.getNodeInfo(), nearNode, key, new ValueWrapper(null), MsgType.FIND_VALUE);
            LOGGER.info("Value retrieved from network: " + value);
            // TODO: localNode.storeKeyValue(key, value); // For caching
            return value;
        }

        return nearbyNodes;
    }

    /**
     * Stores a key-value pair either locally or on the closest node.
     */
    public void store(Node localNode, String key, ValueWrapper value) {
        LOGGER.info("STORE in progress...");

        findNode(localNode.getNodeInfo(), key, localNode.getRoutingTable());
        List<NodeInfo> closestNodes = findClosestNodes(localNode.getRoutingTable(), key, BUCKET_SIZE);

        NodeInfo nodeToStore = findNodeForKey(localNode.getNodeInfo(), key, closestNodes);
        if (nodeToStore != null) {
            if (nodeToStore.equals(localNode.getNodeInfo())) {
                localNode.storeKeyValue(key, value.getValue());
                if (value.getValue() instanceof Auction) {
                    ((Auction) value.getValue()).setStoredNodeId(localNode.getNodeInfo().getNodeId());
                }
                LOGGER.info("Stored key: " + key + " with value: " + value.getValue());
            } else {
                connectAndHandle(localNode.getNodeInfo(), nodeToStore, key, value, MsgType.STORE);
            }
        } else {
            LOGGER.severe("Failed to locate node for storing key-value");
        }
    }

    /**
     * Determines the best node to store a key based on XOR distance.
     */
    private NodeInfo findNodeForKey(NodeInfo selfInfo, String key, List<NodeInfo> candidates) {
        NodeInfo bestNode = selfInfo;
        int minDistance = Utils.calculateDistance(selfInfo.getNodeId(), key);

        for (NodeInfo candidate : candidates) {
            int dist = Utils.calculateDistance(candidate.getNodeId(), key);
            if (dist < minDistance) {
                minDistance = dist;
                bestNode = candidate;
            }
        }
        return bestNode;
    }

    /**
     * Broadcasts an updated block hash to the network.
     */
    public void notifyNewBlockHash(NodeInfo selfInfo, Set<NodeInfo> routingSet, String newHash) {
        LOGGER.info("Starting block hash notification");
        if (!newHash.contentEquals(this.currentBlockHash)) {
            currentBlockHash = new StringBuilder(newHash);
            LOGGER.info("Block hash updated");
            for (NodeInfo node : routingSet) {
                connectAndHandle(selfInfo, node, newHash, null, MsgType.NOTIFY);
            }
        } else {
            LOGGER.info("Block hash unchanged");
        }
    }

    /**
     * Announces a new auction ID to all peers.
     */
    public void broadcastNewAuction(NodeInfo selfInfo, Set<NodeInfo> routingSet, String auctionId) {
        LOGGER.info("Broadcasting new auction");
        if (this.currentAuctionId == null || !auctionId.contentEquals(this.currentAuctionId)) {
            this.currentAuctionId = new StringBuilder(auctionId);
            for (NodeInfo node : routingSet) {
                connectAndHandle(selfInfo, node, auctionId, null, MsgType.NEW_AUCTION);
            }
        } else {
            LOGGER.info("Auction already broadcasted");
        }
    }

    /**
     * Checks whether a routing table contains a specific node.
     */
    public boolean contains(Set<NodeInfo> routingSet, String nodeId) {
        for (NodeInfo node : routingSet) {
            if (node.getNodeId().equals(nodeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Notifies auction participants of bid updates.
     */
    public void notifyAuctionUpdate(NodeInfo selfInfo, Set<NodeInfo> routingSet, Auction auction) {
        LOGGER.info("Processing auction update notification");

        // Ensure all subscribers are reachable
        for (String subscriberId : auction.getSubscribers()) {
            if (!contains(routingSet, subscriberId) &&
                !subscriberId.equals(selfInfo.getNodeId()) &&
                !subscriberId.equals(auction.getStoredNodeId())) {
                findNode(selfInfo, subscriberId, routingSet);
            }
        }

        String auctionId = auction.getId();
        Double currentBid = auction.getCurrentBid();

        // Notify appropriate participants
        for (NodeInfo node : routingSet) {
            if (node.getNodeId().equals(auction.getStoredNodeId())) {
                if (auction.isOpen()) {
                    connectAndHandle(selfInfo, node, auctionId, new ValueWrapper(currentBid), MsgType.AUCTION_UPDATE);
                }
            }
            if (auction.isSubscriber(node.getNodeId())) {
                if (auction.isOpen() && !node.getNodeId().equals(auction.getStoredNodeId())) {
                    connectAndHandle(selfInfo, node, auctionId, new ValueWrapper(currentBid), MsgType.AUCTION_UPDATE);
                } else if (!auction.isOpen()) {
                    connectAndHandle(selfInfo, node, auctionId, new ValueWrapper("Auction " + auctionId + " closed."), MsgType.AUCTION_UPDATE);
                }
            }
        }
    }

    /**
     * Notifies the auction creator when a new participant subscribes.
     */
    public void notifyNewSubscriber(NodeInfo selfInfo, Set<NodeInfo> routingSet, Auction auction) {
        LOGGER.info("Notifying about new subscriber");

        for (String subscriberId : auction.getSubscribers()) {
            if (!contains(routingSet, subscriberId) &&
                !subscriberId.equals(selfInfo.getNodeId()) &&
                !subscriberId.equals(auction.getStoredNodeId())) {
                findNode(selfInfo, subscriberId, routingSet);
            }
        }

        for (NodeInfo node : routingSet) {
            if (node.getNodeId().equals(auction.getStoredNodeId())) {
                connectAndHandle(selfInfo, node, auction.getId(), new ValueWrapper(selfInfo.getNodeId()), MsgType.AUCTION_UPDATE);
            }
        }
    }

    /**
     * Core communication method to send messages between nodes.
     */
    private Object connectAndHandle(NodeInfo selfInfo, NodeInfo targetInfo, String key, ValueWrapper value, MsgType type) {
        List<NodeInfo> nearNodes = new ArrayList<>();
        EventLoopGroup eventGroup = new NioEventLoopGroup();
        try {
            connectToNode(selfInfo, targetInfo, eventGroup, channel -> {
                ClientDHT handler = new ClientDHT(selfInfo, targetInfo, key, value, type, nearNodes);
                channel.pipeline().addLast(handler);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("Connection interrupted: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Connection error: " + e.getMessage());
        } finally {
            eventGroup.shutdownGracefully().addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.severe("Event loop shutdown error: " + future.cause().getMessage());
                }
            });
        }

        if (type == MsgType.FIND_NODE) return nearNodes;
        else if (type == MsgType.FIND_VALUE) return value.getValue();
        return null;
    }

    /**
     * Establishes a Netty connection and sets up channel pipeline with custom handler.
     */
    private void connectToNode(NodeInfo selfInfo, NodeInfo targetInfo, EventLoopGroup group, MessagePassingQueue.Consumer<Channel> channelSetup) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.AUTO_CLOSE, true)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        channelSetup.accept(ch);
                    }
                });

        bootstrap.localAddress(selfInfo.getPort()); // We can add +1 here if we start getting port issues
        ChannelFuture future = bootstrap.connect(targetInfo.getIpAddr(), targetInfo.getPort()).sync();
        LOGGER.info("Connected to node " + targetInfo.getIpAddr() + ":" + targetInfo.getPort());
        future.channel().closeFuture().await(3, TimeUnit.SECONDS);
    }

    // Getter/setter for current block hash
    public StringBuilder getLatestBlockHash() {
        return currentBlockHash;
    }

    public void setLatestBlockHash(String newHash) {
        this.currentBlockHash = new StringBuilder(newHash);
    }
}
