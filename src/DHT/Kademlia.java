package DHT;
import java.net.*;
import java.io.*;
import java.util.*;

class Node {
    private final int id;
    private final int port;
    private DatagramSocket socket;
    private final Map<String,String> dataStore = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, InetSocketAddress> routingTable = Collections.synchronizedMap(new TreeMap<>());


    public Node(int id, int port) throws SocketException{
        this.id = id;
        this.port = port;
        this.socket = new DatagramSocket(port);
    }

    public void thread_start(){
        new Thread(this::listen).start();
    }

    private void listen(){
        byte[] buffer = new byte[1024];
        while(true){
            try{
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(),0,packet.getLength());
                processMessage(message,packet.getAddress(),packet.getPort());
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void processMessage(String message, InetAddress senderAddress, int senderPort){
        System.out.println("Message: " + message + " from " + senderAddress + ":" + senderPort);
        String[] parts = message.split(" ",2);
        String command = parts[0];
        String content = parts.length > 1 ? parts[1] : "";

        switch(command) {
            case "PING":
                sendMessage("PING", senderAddress, senderPort);
                break;
            case "STORE":
                String[] keyValue = content.split(" ",2);
                if (keyValue.length == 2){
                    dataStore.put(keyValue[0],keyValue[1]);
                    sendMessage("STORED " + keyValue[0], senderAddress, senderPort);
                }
                break;
            case "FIND_NODE":
                int targetId = Integer.parseInt(content);
                InetSocketAddress closestNode = findClosestNode(targetId);
                if(closestNode != null){
                    sendMessage("NODE " + closestNode.getAddress().getHostAddress() + ":" + closestNode.getPort(),senderAddress,senderPort);
                }
                else{
                    sendMessage("NODE_NOT_FOUND", senderAddress, senderPort);
                }
                break;
            case "FIND_VALUE":
                if(dataStore.containsKey(content)){
                    sendMessage("VALUE " + content + " " + dataStore.get(content), senderAddress, senderPort);
                }
                else{
                    sendMessage("VALUE_NOT_FOUND " + content, senderAddress, senderPort);
                }
                break;
            case "JOIN":
                String[] joinParts = content.split(" ",2);
                int newNodeId = Integer.parseInt(joinParts[0]);
                routingTable.put(xorDistance(id,newNodeId), new InetSocketAddress(senderAddress,senderPort));
                sendMessage("JOINED AS " + newNodeId, senderAddress, senderPort);
                break;
            case "LEAVE":
                int leavingNodeId = Integer.parseInt(content);
                routingTable.remove(xorDistance(id,leavingNodeId));
                break;
        }
    }

    private InetSocketAddress findClosestNode(int targetId){
        return routingTable.isEmpty() ? null: routingTable.firstEntry().getValue();
    }

    private int xorDistance(int node1,int node2){
        return node1 ^ node2;
    }

    private void sendMessage(String message, InetAddress address, int port){
        try{
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data,data.length,address,port);
            socket.send(packet);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
