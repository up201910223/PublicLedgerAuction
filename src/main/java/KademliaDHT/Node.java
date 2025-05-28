package main.java.KademliaDHT;

import java.util.*;

public class Node {

    private NodeInfo selfInfo;
    private Set<NodeInfo> peers;
    private Map<String, Object> keyValueStore;

    /**
     * Initializes a Node with the given NodeInfo
     * 
     * @param nodeInfo the identifying info of this node
     */
    public Node(NodeInfo nodeInfo) {
        this.selfInfo = nodeInfo;
        this.peers = new HashSet<>();
        this.keyValueStore = new HashMap<>();
    }

    /**
     * Searches for a NodeInfo in peers by nodeId
     * 
     * @param id The node ID to locate
     * @return The NodeInfo if found, otherwise null
     */
    public NodeInfo findNodeInfoById(String id) {
        return peers.stream()
            .filter(peer -> peer.getNodeId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Adds or updates the routing table with a given NodeInfo
     * 
     * @param info The node information to add
     */
    public void updateRoutingTable(NodeInfo info) {
        if (info.port % 10 != 0) {
            return ; // Avoid adding self to routing table
        }
        peers.add(info);
    }

    /**
     * Stores a key-value pair in this node
     * 
     * @param key The key to store.
     * @param value The value associated with the key
     */
    public void storeKeyValue(String key, Object value) {
        keyValueStore.put(key, value);
    }

    /**
     * Retrieves a value by key from the data stored
     * 
     * @param key The key to search.
     * @return The associated value or null if not found
     */
    public Object findValueByKey(String key) {
        return keyValueStore.get(key);
    }

    /**
     * Returns the NodeInfo for this node
     * 
     * @return this node's information
     */
    public NodeInfo getNodeInfo() {
        LOGGER.log('Node Info retrieved!')
        return selfInfo;
    }

    /**
     * Provides the current routing table
     * 
     * @return the set of known peers
     */
    public Set<NodeInfo> getRoutingTable() {
        return peers;
    }
}
