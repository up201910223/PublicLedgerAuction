package Kademlia;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NodeInfo implements Serializable, Comparable<NodeInfo> {
    private static final Logger logger = Logger.getLogger(NodeInfo.class.getName());

    private final String nodeId;
    private final String ipAddr;
    private final int port;

    /**
     * Constructs a NodeInfo object with the specified IP address and port
     * Generates a unique nodeId based on these parameters
     *
     * @param ipAddr The IP address of the node
     * @param port   The port number on which the node listens
     */
    public NodeInfo(String ipAddr, int port) {
        this.ipAddr = ipAddr;
        this.port = port;
        this.nodeId = generateNodeId(ipAddr, port);
    }

    /**
     * Generates a unique node ID by hashing the IP address, port, and a random factor
     * Uses SHA-1 and returns the first 40 hex characters (160 bits)
     *
     * @param ipAddress The IP address of the node
     * @param port      The port number
     * @return A unique node ID as a hex string, or null if an error occurs
     */
    public static String generateNodeId(String ipAddress, int port) {
        String input = ipAddress + ":" + port + ":" + Math.random();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes());
            return Utils.getHexString(hash).substring(0, 40);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "SHA-1 algorithm not found, failed to generate node ID", e);
            return null;
        }
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
               "nodeId='" + nodeId + '\'' +
               ", ipAddr='" + ipAddr + '\'' +
               ", port=" + port +
               '}';
    }

    @Override
    public int compareTo(NodeInfo other) {
        return this.nodeId.compareTo(other.nodeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NodeInfo)) return false;
        NodeInfo other = (NodeInfo) obj;
        return Objects.equals(nodeId, other.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    // Custom serialization methods to explicitly control serialization
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(nodeId);
        out.writeObject(ipAddr);
        out.writeInt(port);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        nodeId = (String) in.readObject();
        ipAddr = (String) in.readObject();
        port = in.readInt();
    }

    // Getters

    public String getNodeId() {
        return nodeId;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public int getPort() {
        return port;
    }
}
