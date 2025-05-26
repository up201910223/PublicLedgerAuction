package main.java.Auctions;

import java.io.*;
import java.security.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Auction implements Serializable {
    // Logger para registar eventos importantes
    private static final Logger logger = Logger.getLogger(Auction.class.getName());

    // Atributos principais do leilão
    private String auctionId;
    private PublicKey sellerPublicKey;
    private String item;
    private double startingPrice;
    private long endTime;
    private double currentBid;
    private PublicKey currentBidder;
    private boolean isOpen;
    private List<String> subscribers;
    private String endTimeString;
    private Timer timer;
    private StringBuilder storedNodeId;

    /**
     * Construtor principal para criar um novo leilão.
     *
     * @param sellerPublicKey Chave pública do vendedor
     * @param item Nome do item em leilão
     * @param startingPrice Preço inicial do leilão
     * @param endTimeString Tempo de fim do leilão no formato "yyyy-MM-dd HH:mm:ss"
     */
    public Auction(PublicKey sellerPublicKey, String item, double startingPrice, String endTimeString) {
        this.auctionId = generateAuctionId(sellerPublicKey, item, startingPrice, endTimeString);
        this.sellerPublicKey = sellerPublicKey;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.isOpen = true;
        this.subscribers = new ArrayList<>();
        this.endTimeString = endTimeString;
        this.storedNodeId = new StringBuilder();
        try {
            // Conversão da string de data para LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime time = LocalDateTime.parse(endTimeString, formatter);
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/London"));
            this.endTime = ChronoUnit.MILLIS.between(now, time);
            if (this.endTime <= 0) {
                throw new IllegalArgumentException();
            }
            this.startAuctionTimer();
        } catch (DateTimeParseException e) {
            logger.warning("Invalid end time format. Please use yyyy-MM-dd HH:mm:ss");
        } catch (IllegalArgumentException e) {
            logger.warning("End time cannot be in the past");
        }
    }

    /**
     * Permite a um utilizador fazer uma oferta num leilão aberto.
     * Verifica a assinatura digital, se o leilão está aberto e se a oferta é válida.
     */
    public boolean placeBid(PublicKey bidderPublicKey, double bidAmount, byte[] signature) {
        byte[] data = (bidderPublicKey.toString() + bidAmount).getBytes();

        if (!CryptoUtils.verifySignature(bidderPublicKey, data, signature)) {
            logger.warning("Invalid bid signature.");
            return false;
        } else if (!this.isOpen()) {
            logger.warning("Bid rejected. Auction is closed.");
            return false;
        } else if (bidAmount <= this.currentBid) {
            logger.warning("Bid amount must be greater than current bid.");
            return false;
        }

        this.currentBid = bidAmount;
        this.currentBidder = bidderPublicKey;

        return true;
    }

    /**
     * Inicia um temporizador para encerrar o leilão quando o tempo expirar.
     */
    private void startAuctionTimer() {
        logger.info("Starting Auction " + auctionId);

        this.timer = new Timer();
        long timeout = this.endTime;
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeAuction();
            }
        }, timeout);
    }

    /**
     * Cancela o temporizador do leilão, se estiver ativo.
     */
    private void cancelAuctionTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Verifica se o leilão ainda está aberto.
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Fecha o leilão e regista o vencedor (se houver).
     */
    public void closeAuction() {
        isOpen = false;
        logger.info("Auction closed. Winner: " + currentBidder + ", Winning bid: " + currentBid);
        cancelAuctionTimer();
        // TODO: notificar subscritores através do Kademlia
    }

    /**
     * Gera um ID único para o leilão com base nas informações fornecidas.
     */
    public static String generateAuctionId(PublicKey sellerPublicKey, String item, double startingPrice, String endTime) {
        String input = sellerPublicKey + ":" + item + ":" + startingPrice + ":" + endTime + ":" + Math.random();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes());
            return CryptoUtils.getHexString(hash).substring(0, 40);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error generating auction ID", e);
            return null;
        }
    }

    /**
     * Adiciona um subscritor à lista (se ainda não estiver presente).
     */
    public void addSubscriber(String nodeId) {
        if (!isSubscriber(nodeId)) {
            subscribers.add(nodeId);
        } else {
            logger.info("You are already subscribed to this auction.");
        }
    }

    /**
     * Verifica se um determinado node está subscrito ao leilão.
     */
    public boolean isSubscriber(String nodeId) {
        return subscribers.contains(nodeId);
    }

    /**
     * Converte os dados do leilão para formato legível.
     */
    @Override
    public String toString() {
        return "Auction ID: " + auctionId + "\n" +
                "Seller Public Key: " + sellerPublicKey + "\n" +
                "Item: " + item + "\n" +
                "Starting Price: " + startingPrice + "\n" +
                "End Time: " + endTimeString + "\n" +
                "Current Bid: " + currentBid + "\n" +
                "Current Bidder: " + currentBidder + "\n" +
                "Is Open: " + isOpen + "\n" +
                "Subscribers: " + subscribers + "\n" +
                "Stored in node " + storedNodeId + "\n";
    }

    /**
     * Serialização personalizada do objeto Auction.
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(auctionId);
        out.writeObject(sellerPublicKey);
        out.writeObject(item);
        out.writeDouble(startingPrice);
        out.writeLong(endTime);
        out.writeDouble(currentBid);
        out.writeObject(currentBidder);
        out.writeBoolean(isOpen);
        out.writeObject(subscribers);
        out.writeObject(storedNodeId);
    }

    /**
     * Desserialização personalizada do objeto Auction.
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        auctionId = (String) in.readObject();
        sellerPublicKey = (PublicKey) in.readObject();
        item = (String) in.readObject();
        startingPrice = in.readDouble();
        endTime = in.readLong();
        currentBid = in.readDouble();
        currentBidder = (PublicKey) in.readObject();
        isOpen = in.readBoolean();
        subscribers = (List<String>) in.readObject();
        storedNodeId = (StringBuilder) in.readObject();
    }

    // Métodos auxiliares para obter ou definir campos específicos do leilão

    public String getId() {
        return this.auctionId;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public PublicKey getCurrentBidder() {
        return currentBidder;
    }

    public PublicKey getSellerPublicKey() {
        return sellerPublicKey;
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    public String getStoredNodeId() {
        return storedNodeId.toString();
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public void setCurrentBidder(PublicKey currentBidder) {
        this.currentBidder = currentBidder;
    }

    public void setStoredNodeId(String storedNodeId) {
        this.storedNodeId = new StringBuilder(storedNodeId);
    }
}
