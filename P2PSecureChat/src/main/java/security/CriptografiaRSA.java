package main.java.security;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class CriptografiaRSA {

    // Método para criptografar a mensagem usando a chave pública do destinatário
    public static String criptografar(String mensagem, PublicKey chavePublica) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, chavePublica);
        byte[] mensagemCriptografada = cipher.doFinal(mensagem.getBytes());
        return Base64.getEncoder().encodeToString(mensagemCriptografada);
    }

    // Método para descriptografar a mensagem usando a chave privada do destinatário
    public static String descriptografar(String mensagemCriptografada, PrivateKey chavePrivada) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, chavePrivada);
        byte[] bytesDescriptografados = Base64.getDecoder().decode(mensagemCriptografada);
        byte[] mensagemDescriptografada = cipher.doFinal(bytesDescriptografados);
        return new String(mensagemDescriptografada);
    }
}
