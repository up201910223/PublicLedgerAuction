package main.java.KademliaDHT;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

/** 
 * Utility class providing helper methods for the Kademlia protocol implementation
 */
public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /**
     * Retrieves the public IP address of the local machine using an external web service
     *
     * @return The public IP address as a String
     * @throws RuntimeException if an error occurs during IP retrieval
     */
    public static String getAddress() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL("https://checkip.amazonaws.com/").openStream()))) {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get public IP address", e);
        }
    }

    /**
     * Converts a byte array into its hexadecimal string representation
     *
     * @param hash The byte array to convert
     * @return Hexadecimal string representation
     */
    public static String getHexString(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Generates a random 160-bit (20 byte) node ID
     *
     * @return A byte array representing the random ID
     */
    public static byte[] generateRandomId() {
        SecureRandom random = new SecureRandom();
        byte[] id = new byte[20]; // 160 bits
        random.nextBytes(id);
        return id;
    }

    /**
     * Serializes a Java object into a Netty ByteBuf
     *
     * @param obj The object to serialize
     * @return ByteBuf containing the serialized object data
     * @throws IOException if serialization fails
     */
    public static ByteBuf serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
            byte[] bytes = baos.toByteArray();
            return Unpooled.wrappedBuffer(bytes);
        }
    }

    /**
     * Deserializes an object from a Netty ByteBuf
     *
     * @param byteBuf ByteBuf containing the serialized object
     * @return The deserialized object
     * @throws IOException            If an I/O error occurs
     * @throws ClassNotFoundException If the class of the serialized object is not found
     */
    public static Object deserialize(ByteBuf byteBuf) throws IOException, ClassNotFoundException {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }

    /**
     * Finds the K closest nodes to the requested node ID in the routing table
     *
     * @param myRoutingTable The set of known nodes
     * @param requestedNodeId The target node ID to find neighbors for
     * @param K The maximum number of closest nodes to return
     * @return List of closest NodeInfo objects
     */
    public static List<NodeInfo> findClosestNodes(Set<NodeInfo> myRoutingTable, String requestedNodeId, final int K) {
        Map<NodeInfo, Integer> distanceMap = new HashMap<>();

        for (NodeInfo nodeInfo : myRoutingTable) {
            if (!nodeInfo.getNodeId().equals(requestedNodeId)) {
                int distance = calculateDistance(requestedNodeId, nodeInfo.getNodeId());
                distanceMap.put(nodeInfo, distance);
            }
        }

        List<Map.Entry<NodeInfo, Integer>> sortedEntries = new ArrayList<>(distanceMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue());

        int count = Math.min(K, sortedEntries.size());
        List<NodeInfo> nearNodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            nearNodes.add(sortedEntries.get(i).getKey());
        }

        return nearNodes;
    }

    /**
     * Calculates the XOR-based distance between two node IDs, returned as the bit count of the XOR result
     *
     * @param nodeId1 First node ID as a hexadecimal string
     * @param nodeId2 Second node ID as a hexadecimal string
     * @return The number of differing bits (distance)
     */
    public static int calculateDistance(String nodeId1, String nodeId2) {
        BigInteger id1 = new BigInteger(nodeId1, 16);
        BigInteger id2 = new BigInteger(nodeId2, 16);
        BigInteger distance = id1.xor(id2);
        return distance.bitCount();
    }

    /**
     * Sends a DatagramPacket containing the given ByteBuf to the specified address and logs success or failure
     *
     * @param ctx          The Netty channel context
     * @param msg          ByteBuf containing the message to send
     * @param sender       InetSocketAddress to send the packet to
     * @param messageType  The type of message being sent
     * @param success      Log message upon successful sending
     */
    public static void sendPacket(ChannelHandlerContext ctx, ByteBuf msg, InetSocketAddress sender, Kademlia.MsgType messageType, String success) {
        ctx.writeAndFlush(new DatagramPacket(msg, sender)).addListener(future -> {
            if (!future.isSuccess()) {
                ChannelFuture closeFuture = ctx.channel().close();
                closeFuture.addListener(close -> {
                    if (close.isSuccess()) {
                        System.err.println(messageType + " failed, channel closed successfully");
                    } else {
                        System.err.println(messageType + " failed, channel close failed: " + close.cause());
                    }
                });
            } else {
                logger.info(success);
            }
        });
    }
}
