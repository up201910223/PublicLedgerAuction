package ledger;

/**
 * Classe principal para testar a implementação da Blockchain.
 */
public class Main {
    public static void main(String[] args) {
        // Criamos uma nova instância da Blockchain
        Blockchain blockchain = new Blockchain();

        // Adicionamos alguns blocos com dados fictícios
        blockchain.addBlock("Primeiro bloco após o Genesis");
        blockchain.addBlock("Segundo bloco com dados de leilão");
        blockchain.addBlock("Terceiro bloco: outra transação");

        // Exibimos os blocos da Blockchain no terminal
        blockchain.printBlockchain();
    }
}
