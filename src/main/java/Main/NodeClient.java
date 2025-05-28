package main.java.Main;

import main.java.KademliaDHT.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class NodeClient: Represents a client node participating in the KademliaDHT network.
 */
public class NodeClient {

    private static final Logger LOGGER = Logger.getLogger(NodeClient.class.getName());
    private final String hostAddress;
    private final int networkPort;

    /**
     * Constructs a NodeClient instance with the specified IP address and port.
     *
     * @param hostAddress The IP address of this node.
     * @param networkPort The port number on which this node communicates.
     */
    NodeClient(String hostAddress, int networkPort) {
        this.hostAddress = hostAddress;
        this.networkPort = networkPort;
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage error: java Main.java <port> [BootstrapNodeIp]");
            System.exit(1);
        }

        NodeClient currentNode = new NodeClient(Utils.getAddress(), Integer.parseInt(args[0]));
        System.out.println(currentNode);

        try {
            Node kademliaNode = new Node(new NodeInfo(currentNode.hostAddress, currentNode.networkPort));
            Kademlia dhtInstance = Kademlia.getInstance();

            if (args.length == 2) {
                String[] bootstrapParts = args[1].split(":");
                String bootstrapIp = bootstrapParts[0];
                int bootstrapPort = Integer.parseInt(bootstrapParts[1]);

                NodeInfo bootstrapInfo = new NodeInfo(bootstrapIp, bootstrapPort);
                kademliaNode.updateRoutingTable(bootstrapInfo);
                dhtInstance.joinNetwork(kademliaNode, bootstrapInfo.getNodeId());
            }

            try {
                NetworkServer server = new NetworkServer(currentNode.networkPort, kademliaNode);
                Thread serverThread = new Thread(server);
                serverThread.start();
                LOGGER.log(Level.FINE, "Kademlia server successfully launched.");
                Kademlia.getInstance().setSharedChannel(server.getChannel());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to start Kademlia server.", e);
            }

            try {
                new Thread(new NodeMainMenu(kademliaNode)).start();
                LOGGER.log(Level.FINE, "Kademlia client interface running.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to start client interface.", e);
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Provided port is not a valid number.", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during node startup", e);
        }
    }

    /**
     * Returns a formatted string representing the current node client.
     *
     * @return String representation of the node.
     */
    @Override
    public String toString() {
        return  "----------------------------------" + '\n' +
                "Node Info" + '\n' +
                "IP Address = " + hostAddress + '\n' +
                "Port = " + networkPort + '\n' +
                "----------------------------------";
    }
}
