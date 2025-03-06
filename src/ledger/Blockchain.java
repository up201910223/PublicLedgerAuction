package ledger;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma Blockchain, que é uma lista encadeada de blocos.
 */
public class Blockchain {
    private List<Block> chain;
    private int difficulty; // Nível de dificuldade da mineração

    /**
     * Construtor da Blockchain. Inicia com o bloco génesis e define a dificuldade.
     */
    public Blockchain(int difficulty) {
        this.chain = new ArrayList<>();
        this.difficulty = difficulty;
        chain.add(createGenesisBlock());
    }

    /**
     * Cria o bloco génesis (o primeiro bloco da cadeia).
     * @return O bloco génesis.
     */
    private Block createGenesisBlock() {
        Block genesis = new Block(0, "Genesis Block", "0");
        genesis.mineBlock(difficulty); // Minera o bloco génesis
        return genesis;
    }

    /**
     * Obtém o último bloco da cadeia.
     * @return Último bloco.
     */
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    /**
     * Adiciona um novo bloco à blockchain e realiza a mineração.
     * @param data Dados a armazenar no novo bloco.
     */
    public void addBlock(String data) {
        Block previousBlock = getLatestBlock();
        Block newBlock = new Block(previousBlock.getIndex() + 1, data, previousBlock.getHash());
        newBlock.mineBlock(difficulty); // Mineração do bloco antes de adicioná-lo
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
