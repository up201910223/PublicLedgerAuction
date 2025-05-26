package main.java.BlockChain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;

/**
 * Representa um bloco na blockchain, contendo uma lista de transações e metadados
 * como índice, hash, timestamp, nonce e hash do bloco anterior.
 */
public class Block implements Serializable {
    private int index;
    private String previousHash;
    private List<Transaction> transactions;
    private long timestamp;
    private String hash;
    private int nonce;

    /**
     * Construtor do bloco.
     * @param index Índice do bloco na cadeia.
     * @param previousHash Hash do bloco anterior.
     * @param transactions Lista de transações a incluir no bloco.
     */
    public Block(int index, String previousHash, List<Transaction> transactions) {
        this.index = index;
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.timestamp = new Date().getTime();
        this.nonce = 0;
        this.hash = calculateHash(); // Calcula o hash inicial do bloco
    }

    /**
     * Calcula o hash do bloco com base no conteúdo atual.
     * Usa SHA-256 sobre os campos: previousHash, timestamp, nonce, e as transações.
     * @return O hash resultante como uma string hexadecimal.
     */
    public String calculateHash() {
        String input = previousHash + timestamp + nonce + transactions.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return BlockchainUtils.bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular o hash do bloco", e);
        }
    }

    /**
     * Incrementa o nonce (tentativas de mineração).
     */
    public void incrementNonce() {
        this.nonce++;
    }

    /**
     * Implementa a mineração do bloco.
     * O bloco só é considerado "minerado" quando o seu hash começa com N zeros.
     * @param difficulty Número de zeros exigidos no início do hash.
     */
    public void mineBlock(int difficulty) {
        String target = "0".repeat(difficulty);
        while (!hash.substring(0, difficulty).equals(target)) {
            incrementNonce();
            hash = calculateHash();
        }
        System.out.println("Bloco minerado com sucesso: " + hash);
    }

    /**
     * Serialização customizada - grava os campos do bloco num stream.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(index);
        out.writeObject(previousHash);
        out.writeObject(transactions);
        out.writeLong(timestamp);
        out.writeObject(hash);
        out.writeInt(nonce);
    }

    /**
     * Desserialização customizada - lê os campos do bloco de um stream.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        index = in.readInt();
        previousHash = (String) in.readObject();
        transactions = (List<Transaction>) in.readObject();
        timestamp = in.readLong();
        hash = (String) in.readObject();
        nonce = in.readInt();
    }

    // Getters

    public int getIndex() {
        return index;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getHash() {
        return hash;
    }

    public int getNonce() {
        return nonce;
    }

    /**
     * Representação textual do bloco, útil para debug e logs.
     */
    @Override
    public String toString() {
        return "Bloco #" + index + "\n" +
                "Hash anterior: " + previousHash + "\n" +
                "Timestamp: " + BlockchainUtils.formatTimestamp(timestamp) + "\n" +
                "Hash atual: " + hash + "\n" +
                "Nonce: " + nonce + "\n" +
                "Transações: " + transactions + "\n";
    }

    /**
     * Verifica se dois blocos são iguais com base nos seus campos principais.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || getClass() != obj.getClass()) return false;

        Block other = (Block) obj;
        return index == other.index &&
                timestamp == other.timestamp &&
                nonce == other.nonce &&
                hash.equals(other.hash) &&
                previousHash.equals(other.previousHash) &&
                transactions.equals(other.transactions);
    }
}
