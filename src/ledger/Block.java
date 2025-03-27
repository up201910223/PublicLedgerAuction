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
    private int nonce; // Adicionado para permitir mineração

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
        this.nonce = 0; // Inicializa o nonce
        this.hash = calculateHash(); // Gera o hash do bloco com base nos seus dados
    }
    /**
     * Incrementa o valor do nonce do bloco em 1.
     */
    public void incrementNonce() {this.nonce++; }
    /**
     * Calcula o hash do bloco utilizando SHA-256.
     * O hash inclui o índice, timestamp, dados, hash anterior e o nonce.
     * @return Hash gerado.
     */
    public String calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = index + timestamp + data + previousHash + nonce;
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

    /**
     * Implementa a mineração do bloco, ajustando o nonce até encontrar um hash válido.
     * A dificuldade é determinada pelo número de zeros iniciais que o hash deve ter.
     * @param difficulty Número de zeros iniciais exigidos no hash.
     */
    public void mineBlock(int difficulty) {
        String target = "0".repeat(difficulty); // Cria a string alvo de comparação
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Bloco minerado: " + hash);
    }

    // Getters para acessar os dados do bloco
    public String getHash() { return hash; }
    public String getPreviousHash() { return previousHash; }
    public String getData() { return data; }
    public int getIndex() { return index; }
    public int getNonce() { return nonce; } // Getter adicionado para acessar o nonce
}
