package main.java.BlockChain;

import main.java.Auctions.Auction;
import main.java.Auctions.Wallet;
import main.java.BlockChain.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.security.*;
import java.util.logging.Logger;

/**
 * Representa uma Blockchain, que é uma lista encadeada de blocos.
 */
public class Blockchain {
    public List<Block> chain;
    public final int difficulty; // Nível de dificuldade da mineração
    public List<Transaction> pendingTransactions; // Transações à espera de serem incluídas em blocos
    public final Wallet wallet; // Carteira associada à Blockchain

    private static final Logger logger = Logger.getLogger(Blockchain.class.getName());

    // Singleton — garante que só existe uma instância da Blockchain
    private static Blockchain instance;
    /**
     * Construtor da Blockchain. Inicia com o bloco génesis com dificuldade default.
     */
    private Blockchain() {
        this.chain = new ArrayList<>();
        this.wallet = Wallet.getInstance();
        this.pendingTransactions = new ArrayList<>();
        this.difficulty = Constants.DIFFICULTY; // Importa o nível de dificuldade da classe Constants
        Block genesisBlock = createGenesisBlock();
        chain.add(createGenesisBlock());
    }
    /**
     * Obtém singleton ds instãnica da classe Blockchain.
     *
     * @return Singleton ds instãnica da classe Blockchain.
     */
    public static Blockchain getInstance() {
        if (instance == null) {
            instance = new Blockchain();
        }
        return instance;
    }

    /**
     * Cria o bloco génesis (o primeiro bloco da cadeia).
     * @return O bloco génesis.
     */
    private Block createGenesisBlock() {
        List<Transaction> genesisTransactions = new ArrayList<>();

        KeyPair receiver = Wallet.generateKeyPair(); // Gera um destino fictício
        Transaction tx = new Transaction(receiver.getPublic(), 0); // Transação simbólica com valor 0
        tx.signTransaction(wallet.getPrivateKey()); // Assinada com a chave privada da carteira
        genesisTransactions.add(tx);

        Block genesis = new Block(0, Constants.GENESIS_PREV_HASH, genesisTransactions);
        genesis.mineBlock(difficulty);
        return genesis;
    }

    /**
     * Obtém o último bloco da cadeia.
     * @return Último bloco.
     */
    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    /**
     * Adiciona um novo bloco à blockchain.
     */
    public void addBlock(Block block) {
        this.chain.add(block);
    }
    /**
     * Adição de transações pendentes com verificação de assinatura. 
     */
    public boolean addTransaction(Transaction tx) {
        if (tx == null || !tx.verifySignature()) {
            logger.warning("Transação inválida ou nula. Rejeitada.");
            return false;
        }
        pendingTransactions.add(tx);
        return true;
    }
    /*
     *  Método toString() para impressão da blockchain
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Blockchain:\n");
        for (Block block : chain) {
            sb.append(block).append("\n");
        }
        return sb.toString();
    }

    /**
     * Exibe todos os blocos da blockchain.
     */
    public void printBlockchain() {
        for (Block block : chain) {
            System.out.println("Index: " + block.getIndex());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("Previous Hash: " + block.getPreviousHash());
            System.out.println("Hash: " + block.getHash());
            System.out.println("Nonce: " + block.getNonce());
            System.out.println("----------------------");
        }
    }

    /**
     * Obtém a blockchain completa.
     * @return Lista de blocos.
     */
    public List<Block> getChain() {
        return chain;
    }
}
