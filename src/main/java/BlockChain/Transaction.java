package BlockChain;

import Auctions.CryptoUtils;
import Auctions.Wallet;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * Representa uma transação entre duas partes, com suporte a assinatura digital
 * e verificação de integridade e autenticidade.
 */
public class Transaction implements Serializable {

    private PublicKey senderPublicKey;
    private PublicKey receiverPublicKey;
    private double amount;
    private byte[] signature;

    /**
     * Construtor da transação. Assume que o remetente é a carteira atual (singleton).
     *
     * @param receiverPublicKey Chave pública do recetor.
     * @param amount Quantia da transação.
     */
    public Transaction(PublicKey receiverPublicKey, double amount) {
        this.senderPublicKey = Wallet.getInstance().getPublicKey();
        this.receiverPublicKey = receiverPublicKey;
        this.amount = amount;
        this.signature = null;
    }

    /**
     * Assina a transação com a chave privada fornecida.
     *
     * @param privateKey Chave privada do remetente.
     */
    public void signTransaction(PrivateKey privateKey) {
        byte[] data = getDataForSignature();
        this.signature = CryptoUtils.sign(privateKey, data);
    }

    /**
     * Verifica se a assinatura da transação é válida.
     *
     * @return true se for válida, false caso contrário.
     */
    public boolean verifySignature() {
        byte[] data = getDataForSignature();
        return CryptoUtils.verifySignature(senderPublicKey, data, signature);
    }

    /**
     * Obtém os dados a serem usados na assinatura (sender + receiver + amount).
     *
     * @return Array de bytes representando os dados da transação.
     */
    private byte[] getDataForSignature() {
        return (senderPublicKey.toString() + receiverPublicKey.toString() + amount).getBytes();
    }

    /**
     * Representação legível da transação.
     */
    @Override
    public String toString() {
        return "\n\tTransaction Details:\n" +
               "\t\tSender: " + senderPublicKey.toString() + "\n" +
               "\t\tReceiver: " + receiverPublicKey.toString() + "\n" +
               "\t\tAmount: " + amount + "\n" +
               "\t\tSignature: " + (signature != null ? Arrays.toString(signature) : "null") + "\n";
    }

    /**
     * Serialização personalizada: grava as chaves públicas, valor e assinatura.
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(senderPublicKey.getEncoded());
        out.writeObject(receiverPublicKey.getEncoded());
        out.writeDouble(amount);
        out.writeObject(signature);
    }

    /**
     * Desserialização personalizada: reconstrói as chaves públicas a partir dos bytes.
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            byte[] senderKeyBytes = (byte[]) in.readObject();
            byte[] receiverKeyBytes = (byte[]) in.readObject();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.senderPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(senderKeyBytes));
            this.receiverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(receiverKeyBytes));
            this.amount = in.readDouble();
            this.signature = (byte[]) in.readObject();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IOException("Erro ao reconstruir as chaves públicas", e);
        }
    }

    // Getters e Setters
    public PublicKey getSenderPublicKey() {
        return senderPublicKey;
    }

    public PublicKey getReceiverPublicKey() {
        return receiverPublicKey;
    }

    public double getAmount() {
        return amount;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}
