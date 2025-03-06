package ledger;

/**
 * Classe principal para testar a implementação da Blockchain.
 */
public class Main {
    public static void main(String[] args) {
        // Criamos uma nova instância da Blockchain com dificuldade 4
        Blockchain blockchain = new Blockchain(4);

        // Adicionamos alguns blocos com dados fictícios (agora com mineração)
        System.out.println("A minerar bloco 1...");
        blockchain.addBlock("Primeiro bloco após o Genesis");

        System.out.println("A minerar bloco 2...");
        blockchain.addBlock("Segundo bloco com dados de leilão");

        System.out.println("A minerar bloco 3...");
        blockchain.addBlock("Terceiro bloco: outra transação");

        // Exibimos os blocos da Blockchain no terminal
        blockchain.printBlockchain();
    }
}
