package main.java;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;

public class PeerHandler implements Runnable {
    private Socket socket;
    private Peer peer;

    public PeerHandler(Socket socket, Peer peer) {
        this.socket = socket;
        this.peer = peer;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String mensagemCriptografada;
            while ((mensagemCriptografada = in.readLine()) != null) {
                String[] partes = mensagemCriptografada.split("\\|");
                if (partes.length != 3) {
                    System.out.println("Formato de mensagem inválido.");
                    continue;
                }
                String idRemetente = partes[0];
                byte[] chaveSimetricaCriptografada = Base64.getDecoder().decode(partes[1]);
                byte[] mensagemDecodificada = Base64.getDecoder().decode(partes[2]);

                SecretKey chaveSimetrica = descriptografarChaveSimetrica(chaveSimetricaCriptografada);
                String mensagem = descriptografarMensagem(mensagemDecodificada, chaveSimetrica);

                System.out.println("\nMensagem recebida de " + idRemetente + ": " + mensagem);
                peer.armazenarMensagem(idRemetente, mensagem); // Armazena a mensagem recebida
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SecretKey descriptografarChaveSimetrica(byte[] chaveSimetricaCriptografada) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, peer.getChavePrivada());
        byte[] chaveSimetricaDecodificada = cipher.doFinal(chaveSimetricaCriptografada);
        return new SecretKeySpec(chaveSimetricaDecodificada, "AES");
    }

    private String descriptografarMensagem(byte[] mensagemCriptografada, SecretKey chaveSimetrica) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, chaveSimetrica);
        byte[] mensagemDescriptografada = cipher.doFinal(mensagemCriptografada);
        return new String(mensagemDescriptografada);
    }
}
