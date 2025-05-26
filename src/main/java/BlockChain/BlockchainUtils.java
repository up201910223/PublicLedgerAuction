package main.java.BlockChain;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Classe utilitária para funções auxiliares relacionadas à Blockchain,
 * como formatação de datas e conversão de arrays de bytes.
 */
public class BlockchainUtils {

    /**
     * Converte um array de bytes para uma representação em string hexadecimal.
     * Utiliza um método modularizado para converter cada byte individualmente.
     *
     * @param bytes Array de bytes a ser convertido.
     * @return Representação em string hexadecimal do array de bytes.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexBuilder.append(formatByteAsHex(b));
        }
        return hexBuilder.toString();
    }

    /**
     * Formata um byte individual como string hexadecimal, com dois dígitos.
     *
     * @param b Byte a ser formatado.
     * @return Representação hexadecimal do byte, com dois dígitos.
     */
    private static String formatByteAsHex(byte b) {
        // Realiza uma operação AND com 0xff para garantir valores positivos,
        // depois converte para string hexadecimal.
        String hex = Integer.toHexString(b & 0xff);
        return hex.length() == 1 ? "0" + hex : hex;
    }

    /**
     * Converte um timestamp Unix (em milissegundos) para uma string
     * formatada com data e hora no padrão "yyyy/MM/dd HH:mm:ss".
     *
     * @param timestamp Timestamp Unix em milissegundos.
     * @return String com data e hora formatada.
     */
    public static String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.format(date);
    }
}
