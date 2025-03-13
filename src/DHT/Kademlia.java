package DHT;
import java.net.*;
import java.io.*;
import java.util.*;

class Node {
    private final int id;
    private final int port;
    private DatagramSocket socket;
    private final Map<Integer, InetSocketAddress> routingTable = new HashMap<>();


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
        if (message.startsWith("PING")){
            sendMessage("PING RECEIVED", senderAddress, senderPort);
        }
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

    public void addNode(int nodeId, InetSocketAddress address){
        routingTable.put(nodeId, address);
    }
}
