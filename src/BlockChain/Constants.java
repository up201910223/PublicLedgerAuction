/**
 * Classe Constants: Armazena constantes globais utilizadas no sistema de Blockchain.
 * Centraliza valores fixos como dificuldade de mineração, recompensa do minerador
 * e o hash anterior do bloco génesis, promovendo clareza, consistência e facilidade de manutenção.
 */

package BlockChain;

public class Constants {
    private Constants(){}

    public static final int DIFFICULTY = 3;

    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    public static final double MINER_REWARD = 10;

}