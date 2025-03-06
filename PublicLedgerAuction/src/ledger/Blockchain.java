package ledger;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma Blockchain, que é uma lista encadeada de blocos.
 */
public class Blockchain {
    private List<Block> chain;

    /**
     * Construtor da Blockchain. Inicia com o bloco génesis.
     */
    public Blockchain() {
        chain = new ArrayList<>();
        chain.add(createGenesisBlock());
    }

    /**
     * Cria o bloco génesis (o primeiro bloco da cadeia).
     * @return O bloco génesis.
     */
    private Block createGenesisBlock() {
        return new Block(0, "Genesis Block", "0");
    }

    /**
     * Obtém o último bloco da cadeia.
     * @return Último bloco.
     */
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    /**
     * Adiciona um novo bloco à blockchain.
     * @param data Dados a armazenar no novo bloco.
     */
    public void addBlock(String data) {
        Block previousBlock = getLatestBlock();
        Block newBlock = new Block(previousBlock.getIndex() + 1, data, previousBlock.getHash());
        chain.add(newBlock);
    }

    /**
     * Exibe todos os blocos da blockchain.
     */
    public void printBlockchain() {
        for (Block block : chain) {
            System.out.println("Index: " + block.getIndex());
            System.out.println("Timestamp: " + block.getData());
            System.out.println("Previous Hash: " + block.getPreviousHash());
            System.out.println("Hash: " + block.getHash());
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
