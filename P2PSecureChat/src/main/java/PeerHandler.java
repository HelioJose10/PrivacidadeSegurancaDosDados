// Definição do pacote principal onde a classe PeerHandler está localizada
package main.java;

// Importações necessárias para criptografia, I/O, rede, segurança e codificação Base64
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Classe PeerHandler que implementa Runnable.
 * Responsável por gerenciar a comunicação recebida de outros peers.
 * Cada instância de PeerHandler lida com uma conexão específica, lendo mensagens criptografadas,
 * descriptografando-as e armazenando-as no Peer correspondente.
 */
public class PeerHandler implements Runnable {
    // Socket que representa a conexão com o peer remoto
    private Socket socket;

    // Referência ao objeto Peer que está gerenciando este handler
    private Peer peer;

    // Chave pública do peer remoto
    private PublicKey chavePublicaPeerRemoto;

    // Chave secreta compartilhada gerada pelo protocolo Diffie-Hellman
    private SecretKey chaveSecretaCompartilhada;

    /**
     * Construtor da classe PeerHandler.
     *
     * @param socket Socket representando a conexão com o peer remoto
     * @param peer   Objeto Peer que está gerenciando esta conexão
     */
    public PeerHandler(Socket socket, Peer peer) {
        this.socket = socket;
        this.peer = peer;
    }

    /**
     * Método principal que será executado quando a thread for iniciada.
     * Lê mensagens recebidas através do socket, descriptografa-as e as armazena no Peer.
     */
    @Override
    public void run() {
        try (
            // Cria um BufferedReader para ler as mensagens recebidas através do InputStream do socket
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String mensagemCriptografada;
            // Lê mensagens linha por linha até que não haja mais dados
            while ((mensagemCriptografada = in.readLine()) != null) {
                // Divide a mensagem recebida em partes usando o delimitador "|"
                String[] partes = mensagemCriptografada.split("\\|");

                // Verifica se a mensagem está no formato esperado (3 partes)
                if (partes.length != 3) {
                    System.out.println("Formato de mensagem inválido."); // Mensagem de erro para formato inválido
                    continue; // Pula para a próxima iteração do loop
                }

                // Extrai o ID do remetente da primeira parte da mensagem
                String idRemetente = partes[0];

                // Decodifica a chave simétrica criptografada da segunda parte da mensagem usando Base64
                byte[] chaveSimetricaCriptografada = Base64.getDecoder().decode(partes[1]);

                // Decodifica a mensagem criptografada da terceira parte da mensagem usando Base64
                byte[] mensagemDecodificada = Base64.getDecoder().decode(partes[2]);

                // Descriptografa a chave simétrica utilizando a chave privada do Peer
                SecretKey chaveSimetrica = descriptografarChaveSimetrica(chaveSimetricaCriptografada);

                // Descriptografa a mensagem utilizando a chave simétrica obtida
                String mensagem = descriptografarMensagem(mensagemDecodificada, chaveSimetrica);

                // Exibe a mensagem recebida no console
                System.out.println("\nMensagem recebida de " + idRemetente + ": " + mensagem);

                // Armazena a mensagem recebida no objeto Peer para que possa ser acessada posteriormente
                peer.armazenarMensagem(idRemetente, idRemetente, mensagem);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Imprime a pilha de erros em caso de exceção
        }
    }

    /**
     * Método privado para descriptografar a chave simétrica (AES) que foi criptografada com a chave pública RSA do remetente.
     *
     * @param chaveSimetricaCriptografada Array de bytes representando a chave simétrica criptografada
     * @return SecretKey representando a chave simétrica descriptografada
     * @throws GeneralSecurityException Caso ocorra um erro durante a descriptografia
     */
    private SecretKey descriptografarChaveSimetrica(byte[] chaveSimetricaCriptografada) throws GeneralSecurityException {
        // Obtém uma instância do Cipher para RSA
        Cipher cipher = Cipher.getInstance("RSA");

        // Inicializa o Cipher em modo de descriptografia utilizando a chave privada do Peer
        cipher.init(Cipher.DECRYPT_MODE, peer.getChavePrivada());

        // Executa a descriptografia da chave simétrica
        byte[] chaveSimetricaDecodificada = cipher.doFinal(chaveSimetricaCriptografada);

        // Cria uma SecretKey a partir dos bytes da chave simétrica descriptografada, especificando o algoritmo AES
        return new SecretKeySpec(chaveSimetricaDecodificada, "AES");
    }

    /**
     * Método privado para descriptografar a mensagem que foi criptografada com a chave simétrica (AES).
     *
     * @param mensagemCriptografada Array de bytes representando a mensagem criptografada
     * @param chaveSimetrica        SecretKey utilizada para criptografar a mensagem
     * @return String representando a mensagem descriptografada
     * @throws GeneralSecurityException Caso ocorra um erro durante a descriptografia
     */
    private String descriptografarMensagem(byte[] mensagemCriptografada, SecretKey chaveSimetrica) throws GeneralSecurityException {
        // Obtém uma instância do Cipher para AES
        Cipher cipher = Cipher.getInstance("AES");

        // Inicializa o Cipher em modo de descriptografia utilizando a chave simétrica
        cipher.init(Cipher.DECRYPT_MODE, chaveSimetrica);

        // Executa a descriptografia da mensagem
        byte[] mensagemDescriptografada = cipher.doFinal(mensagemCriptografada);

        // Converte os bytes descriptografados de volta para uma String usando a codificação padrão
        return new String(mensagemDescriptografada);
    }

    /**
     * Método para processar a chave pública recebida de outro peer e gerar a chave secreta compartilhada.
     *
     * @param chavePublicaRemota Chave pública recebida do peer remoto
     */
    public void processarChavePublica(String chavePublicaRemota) {
        try {
            // Decodifica a chave pública recebida
            byte[] chaveRemotaBytes = Base64.getDecoder().decode(chavePublicaRemota);
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(chaveRemotaBytes);
            chavePublicaPeerRemoto = keyFactory.generatePublic(x509KeySpec);

            // Gera a chave secreta compartilhada
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(peer.getChavePrivada());
            keyAgreement.doPhase(chavePublicaPeerRemoto, true);
            byte[] chaveCompartilhada = keyAgreement.generateSecret();
            chaveSecretaCompartilhada = new SecretKeySpec(chaveCompartilhada, "AES");

            System.out.println("Chave secreta compartilhada gerada.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método para enviar uma mensagem (criptografada) para o peer remoto.
     *
     * @param mensagem Mensagem a ser enviada
     */
    public void enviarMensagem(String mensagem) {
        try {
            if (chaveSecretaCompartilhada != null) {
                // Criptografa a mensagem antes de enviá-la
                byte[] mensagemCriptografada = criptografarMensagem(mensagem);
                String mensagemParaEnviar = peer.getIdPeer() + "|" + Base64.getEncoder().encodeToString(chaveSecretaCompartilhada.getEncoded()) + "|" + Base64.getEncoder().encodeToString(mensagemCriptografada);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(mensagemParaEnviar);
            } else {
                System.out.println("Chave secreta ainda não foi gerada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método para criptografar uma mensagem com a chave secreta compartilhada.
     *
     * @param mensagem Mensagem a ser criptografada
     * @return Array de bytes representando a mensagem criptografada
     * @throws GeneralSecurityException Caso ocorra um erro durante a criptografia
     */
    private byte[] criptografarMensagem(String mensagem) throws GeneralSecurityException {
        // Obtém uma instância do Cipher para AES
        Cipher cipher = Cipher.getInstance("AES");

        // Inicializa o Cipher em modo de criptografia utilizando a chave secreta compartilhada
        cipher.init(Cipher.ENCRYPT_MODE, chaveSecretaCompartilhada);

        // Executa a criptografia da mensagem
        return cipher.doFinal(mensagem.getBytes());
    }
}
