package main.java.BlockChain;

/**
 * Classe Miner: Representa um minerador na blockchain.
 * Responsável por realizar a prova de trabalho (PoW) e acumular recompensas.
 */
public class Miner {

    /** Recompensa acumulada pelo minerador. */
    private double reward;

    /**
     * Mines the block with the given difficulty.
     *
     * @param b The most recent block in the chain.
     */
    private boolean PoW(Block b) {
        String target = new String(new char[Constants.DIFFICULTY]).replace('\0', '0');
        String hash = b.getHash();

        return !hash.substring(0, Constants.DIFFICULTY).equals(target);
    }

    /**
     * Mines a block until the proof of work (PoW) meets the target difficulty.
     *
     * @param b The block to be mined.
     */
    public void mine(Block b) {
        while (PoW(b)){
            b.incrementNonce();
            b.calculateHash();
        }
        reward += Constants.MINER_REWARD;
    }

    /**
     * Verifica se o hash atual do bloco atende à condição da prova de trabalho,
     * ou seja, se começa com um número específico de zeros definidos pela dificuldade.
     *
     * @param block Bloco cujo hash será validado.
     * @return true se o hash for válido; false caso contrário.
     */
    private boolean isValidProof(Block block) {
        String targetPrefix = "0".repeat(Constants.DIFFICULTY);  // Alvo da dificuldade (ex: "0000")
        return block.getHash().startsWith(targetPrefix);
    }

    /**
     * Retorna a recompensa total acumulada pelo minerador até o momento.
     *
     * @return Recompensa total do minerador.
     */
    public double getReward() {
        return reward;
    }
}
