package BlockChain;

/**
 * Classe Miner: Representa um minerador na blockchain.
 * Responsável por realizar a prova de trabalho (PoW) e acumular recompensas.
 */
public class Miner {

    /** Recompensa acumulada pelo minerador. */
    private double reward;

    /**
     * Realiza a mineração de um bloco através do algoritmo de prova de trabalho (PoW).
     * Incrementa o nonce até que o hash do bloco satisfaça a dificuldade alvo.
     *
     * @param block Bloco a ser minerado.
     */
    public void mine(Block block) {
        // Loop até que o hash do bloco atinja o alvo de dificuldade.
        while (!isValidProof(block)) {
            block.incrementNonce();
            block.calculateHash();
        }

        // Quando encontra o hash válido, recompensa o minerador.
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
