package main.java.Main;

import main.java.Auctions.Auction;
import main.java.Auctions.CryptoUtils;
import main.java.Auctions.Wallet;
import main.java.BlockChain.Block;
import main.java.BlockChain.Blockchain;
import main.java.BlockChain.Miner;
import main.java.BlockChain.Transaction;
import main.java.BlockChain.Constants;
import main.java.KademliaDHT.*;

import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Class NodeMainMenu: Interface handler for interacting with a node in the KademliaDHT network.
 */
public class NodeMainMenu implements Runnable {

    private final Scanner inputScanner;
    private final Kademlia dht;
    public Blockchain chain;
    private final Wallet wallet;
    private final Node thisNode;

    /**
     * Constructs a NodeMainMenu linked to the given node.
     *
     * @param thisNode The current node instance.
     */
    public NodeMainMenu(Node thisNode) {
        this.inputScanner = new Scanner(System.in);
        this.dht = Kademlia.getInstance();
        this.chain = Blockchain.getInstance();
        this.wallet = Wallet.getInstance();
        this.thisNode = thisNode;
    }

    public String displayMenu() {
        return """
               ----------------------------------
                0 - Show Routing Table
                1 - Find Node
                2 - Store Value
                3 - Find Value
                4 - Ping Node
                5 - Mine Block
                6 - Create Auction
                7 - Place Bid
                8 - Subscribe to Auction
                99 - Exit
               ----------------------------------""";
    }

    @Override
    public void run() {
        System.out.println(displayMenu());
        while (true) {
            String command = inputScanner.nextLine();

            switch (command) {
                case "menu" -> System.out.println(displayMenu());

                case "0" -> {
                    for (NodeInfo info : thisNode.getRoutingTable()) {
                        System.out.println(info);
                    }
                }

                case "1" -> {
                    System.out.println("Enter Node ID:");
                    String nodeId = inputScanner.nextLine();
                    dht.findNode(thisNode.getNodeInfo(), nodeId, thisNode.getRoutingTable());
                }

                case "2" -> {
                    System.out.println("Enter Key:");
                    String key = inputScanner.nextLine();
                    Block newBlock = new Block(Blockchain.getLastBlock().index+1, Blockchain.getPreviousHash(), Blockchain.getTransactions());
                    newBlock.mineBlock(Constants.DIFFICULTY);
                    newBlock.toString();
                    chain.addBlock(newBlock);
                    dht.store(thisNode, key, new ValueWrapper(newBlock));
                }

                case "3" -> {
                    System.out.println("Enter Key:");
                    String findKey = inputScanner.nextLine();
                    dht.findValue(thisNode, findKey);
                }

                case "4" -> {
                    System.out.println("Enter Node ID:");
                    String pingId = inputScanner.nextLine();
                    dht.ping(thisNode.getNodeInfo(), pingId, thisNode.getRoutingTable());
                }

                case "5" -> {
                    System.out.println("Mining block...");
                    Block minedBlock = new Block(Blockchain.getLastBlock().index+1,Blockchain.getPreviousHash(),Blockchain.getTransactions());
                    minedBlock.mineBlock(Constants.DIFFICULTY);
                    minedBlock.toString();
                    dht.store(thisNode, minedBlock.getHash(), new ValueWrapper(minedBlock));
                    dht.notifyNewBlockHash(thisNode.getNodeInfo(), thisNode.getRoutingTable(), minedBlock.getHash());
                }

                case "6" -> {
                    System.out.println("Item to Auction:");
                    String itemName = inputScanner.nextLine();

                    System.out.println("Starting Price:");
                    double startPrice = Double.parseDouble(inputScanner.nextLine());

                    System.out.println("End Time (yyyy-MM-dd HH:mm:ss):");
                    String expiry = inputScanner.nextLine();

                    Auction auction = new Auction(wallet.getPublicKey(), itemName, startPrice, expiry);
                    auction.addSubscriber(thisNode.getNodeInfo().getNodeId());

                    dht.store(thisNode, auction.getId(), new ValueWrapper(auction));
                    dht.broadcastNewAuction(thisNode.getNodeInfo(), thisNode.getRoutingTable(), auction.getId());
                }

                case "7" -> {
                    System.out.println("Enter Auction ID:");
                    String auctionId = inputScanner.nextLine();
                    Auction existingAuction = (Auction) dht.findValue(thisNode, auctionId);

                    if (existingAuction != null) {
                        System.out.println("Enter Bid Amount:");
                        double bid = Double.parseDouble(inputScanner.nextLine());

                        PublicKey pubKey = wallet.getPublicKey();
                        PrivateKey privKey = wallet.getPrivateKey();

                        byte[] signature = CryptoUtils.sign(privKey, (pubKey.toString() + bid).getBytes());

                        if (existingAuction.placeBid(pubKey, bid, signature)) {
                            Transaction tx = new Transaction(existingAuction.getSellerPublicKey(), bid);
                            tx.signTransaction(privKey);

                            if (chain.addTransaction(tx)) {
                                dht.notifyAuctionUpdate(thisNode.getNodeInfo(), thisNode.getRoutingTable(), existingAuction);
                            }
                        }
                    } else {
                        System.out.println("Auction not found.");
                    }
                }

                case "8" -> {
                    System.out.println("Enter Auction ID:");
                    String subId = inputScanner.nextLine();
                    Object result = dht.findValue(thisNode, subId);
                    Auction auctionToJoin = null;

                    if (result instanceof Auction) {
                        auctionToJoin = (Auction) result;
                    } else if (result instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Auction) {
                        auctionToJoin = (Auction) list.get(0);
                    }

                    if (auctionToJoin != null) {
                        auctionToJoin.addSubscriber(thisNode.getNodeInfo().getNodeId());
                        dht.notifyNewSubscriber(thisNode.getNodeInfo(), thisNode.getRoutingTable(), auctionToJoin);
                    } else {
                        System.out.println("Auction not found.");
                    }
                }

                case "99" -> {
                    System.out.println("Exiting node client...");
                    System.exit(0);
                }

                default -> System.out.println("Invalid input. Please try again.");
            }
        }
    }

    public Block generateBlock() {
        Miner miner = new Miner();
        List<Transaction> txList = new ArrayList<>();

        KeyPair keyPair = Wallet.generateKeyPair();
        Transaction genesisTx = new Transaction(keyPair.getPublic(), 0);
        genesisTx.signTransaction(wallet.getPrivateKey());

        txList.add(genesisTx);
        Block block = new Block(1, chain.getLastBlock().getHash(), txList);

        miner.mine(block);
        return block;
    }
}
