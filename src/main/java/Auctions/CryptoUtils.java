package Auctions;

import java.security.*;

/**
 * Utilitários para operações criptográficas relacionadas com assinaturas digitais.
 * Usado para garantir integridade e autenticidade nas comunicações entre utilizadores (ex: lances nas leilões).
 */
public class CryptoUtils {

    /**
     * Assina um array de bytes com uma chave privada usando SHA256 com RSA.
     *
     * @param privateKey A chave privada usada para assinar os dados.
     * @param data Os dados a serem assinados (por exemplo, combinação de ID do licitante + valor da licitação).
     * @return Um array de bytes que representa a assinatura digital.
     */
    public static byte[] sign(PrivateKey privateKey, byte[] data) {
        try {
            // Inicializa um objeto Signature com algoritmo SHA256 + RSA
            Signature signer = Signature.getInstance("SHA256withRSA");

            // Inicializa o objeto Signature para assinatura, com a chave privada
            signer.initSign(privateKey);

            // Passa os dados ao objeto Signature para que sejam "digestados" e preparados para assinatura
            signer.update(data);

            // Gera a assinatura e retorna como array de bytes
            return signer.sign();
        } catch (Exception e) {
            // Lança exceção encapsulada caso ocorra erro durante assinatura
            throw new RuntimeException("Erro ao assinar dados", e);
        }
    }

    /**
     * Verifica se a assinatura digital é válida com base na chave pública fornecida.
     *
     * @param publicKey A chave pública correspondente à chave privada usada para assinar.
     * @param data Os dados que foram assinados.
     * @param signature A assinatura a ser verificada.
     * @return true se a assinatura for válida, false caso contrário.
     */
    public static boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signature) {
        try {
            // Inicializa um verificador com algoritmo SHA256 + RSA
            Signature verifier = Signature.getInstance("SHA256withRSA");

            // Inicializa o objeto Signature para verificação, com a chave pública
            verifier.initVerify(publicKey);

            // Passa os dados ao objeto Signature, da mesma forma como foram passados ao assinar
            verifier.update(data);

            // Verifica se a assinatura fornecida corresponde aos dados e à chave pública
            return verifier.verify(signature);
        } catch (Exception e) {
            // Em caso de falha (ex: algoritmo inválido, exceção de chave, etc), retorna false
            return false;
        }
    }

/**
 * Gera uma representação hexadecimal de um array de bytes.
 *
 * @param bytes O array de bytes a converter.
 * @return Uma string contendo os caracteres hexadecimais que representam os bytes.
 */
    static String getHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();

        for (byte value : bytes) {
            // Converte o byte para inteiro sem sinal (com 0xff)
            int unsignedByte = value & 0xff;

            // Converte o valor para hexadecimal (ex: "0a", "f4", etc.)
            String hex = Integer.toHexString(unsignedByte);

            // Garante dois caracteres (ex: "0a" em vez de "a")
            if (hex.length() < 2) {
                result.append('0');
            }

            result.append(hex);
        }
        return result.toString();
}

}
