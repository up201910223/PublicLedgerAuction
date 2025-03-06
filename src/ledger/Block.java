package ledger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Representa um bloco individual na Blockchain.
 * Cada bloco contém um índice, timestamp, dados, hash e hash do bloco anterior.
 */
public class Block {
    private int index;
    private long timestamp;
    private String data;
    private String previousHash;
    private String hash;

    /**
     * Construtor do bloco.
     * @param index Índice do bloco na cadeia.
     * @param data Informação armazenada no bloco.
     * @param previousHash Hash do bloco anterior.
     */
    public Block(int index, String data, String previousHash) {
        this.index = index;
        this.timestamp = new Date().getTime();
        this.data = data;
        this.previousHash = previousHash;
        this.hash = calculateHash(); // Gera o hash do bloco com base nos seus dados
    }

    /**
     * Calcula o hash do bloco utilizando SHA-256.
     * @return Hash gerado.
     */
    public String calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = index + timestamp + data + previousHash;
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao calcular hash", e);
        }
    }

    // Getters para acessar os dados do bloco
    public String getHash() { return hash; }
    public String getPreviousHash() { return previousHash; }
    public String getData() { return data; }
    public int getIndex() { return index; }
}
