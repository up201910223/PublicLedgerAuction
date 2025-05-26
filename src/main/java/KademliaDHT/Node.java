package main.java.KademliaDHT;

import java.util.*;

public class Node {

    private NodeInfo selfInfo;
    private Set<NodeInfo> peers;
    private Map<String, Object> dataStore;

    /**
     * Initializes a Node with the given NodeInfo
     * 
     * @param nodeInfo the identifying info of this node
     */
    public Node(NodeInfo nodeInfo) {
        this.selfInfo = nodeInfo;
        this.peers = new HashSet<>();
        this.dataStore = new HashMap<>();
    }

    /**
     * Searches for a NodeInfo in peers by nodeId
     * 
     * @param id The node ID to locate
     * @return The NodeInfo if found, otherwise null
     */
    public NodeInfo getNodeInfoById(String id) {
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
        peers.add(info);
    }

    /**
     * Stores a key-value pair in this node
     * 
     * @param key The key to store.
     * @param value The value associated with the key
     */
    public void putData(String key, Object value) {
        dataStore.put(key, value);
    }

    /**
     * Retrieves a value by key from the data stored
     * 
     * @param key The key to search.
     * @return The associated value or null if not found
     */
    public Object getData(String key) {
        return dataStore.get(key);
    }

    /**
     * Returns the NodeInfo for this node
     * 
     * @return this node's information
     */
    public NodeInfo getNodeInfo() {
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
