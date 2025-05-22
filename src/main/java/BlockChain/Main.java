package BlockChain;

import Auctions.*;

public class Main {
    public static void main(String[] args) {
        // Inicializa o remetente (singleton)
        Wallet senderWallet = Wallet.getInstance();

        // Inicializa o recetor (nova carteira para simular outra entidade)
        Wallet receiverWallet = new Wallet(); 
        // Cria a transação
        Transaction tx = new Transaction(receiverWallet.getPublicKey(), 100.0);
        System.out.println("Transação criada:\n" + tx);

        // Assina a transação com a chave privada do remetente
        tx.signTransaction(senderWallet.getPrivateKey());
        System.out.println("Transação assinada:\n" + tx);

        // Verifica a assinatura
        boolean isValid = tx.verifySignature();
        System.out.println("Assinatura válida? " + isValid);
    }
}
