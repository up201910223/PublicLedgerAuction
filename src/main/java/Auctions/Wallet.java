package main.java.Auctions;

import java.security.*;

/**
 * Representa uma carteira digital de utilizador.
 * Implementa padrão singleton e permite assinar dados com a chave privada.
 */
public class Wallet {

    private static Wallet instance;
    private final KeyPair keyPair;

    /**
     * Construtor. Gera um novo par de chaves RSA de 2048 bits.
     */
    public Wallet() {
        this.keyPair = createRSAKeyPair();
    }

    /**
     * Retorna a instância única da Wallet.
     * Garante que só existe uma carteira por utilizador.
     *
     * @return Instância única da Wallet.
     */
    public static synchronized Wallet getInstance() {
        if (instance == null) {
            instance = new Wallet();
        }
        return instance;
    }

    /**
     * Gera um novo par de chaves RSA.
     *
     * @return KeyPair gerado.
     */
    public static KeyPair createRSAKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo RSA não suportado", e);
        }
    }

    /**
     * Assina os dados fornecidos usando a chave privada da carteira.
     *
     * @param data Dados a assinar.
     * @return Assinatura digital dos dados.
     */
    public byte[] sign(byte[] data) {
        return CryptoUtils.sign(getPrivateKey(), data);
    }

    /**
     * Retorna a chave pública associada à carteira.
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /**
     * Retorna a chave privada associada à carteira.
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    /**
     * Retorna o par de chaves completo.
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }
}
