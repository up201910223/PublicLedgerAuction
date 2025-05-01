package Auctions;

import java.security.*;

public class CryptoUtils {

    /** Assina os dados com a chave privada. */
    public static byte[] sign(PrivateKey privateKey, byte[] data) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(data);
            return signer.sign();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao assinar dados", e);
        }
    }

    /** Verifica a assinatura com a chave pública. */
    public static boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signature) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
