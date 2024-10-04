package main.java.client;

import javax.crypto.Cipher;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class Peer {
    private int porta;
    private String idPeer;
    private Map<String, InetSocketAddress> dht; // Simulação de DHT (endereços de peers)
    private PublicKey chavePublica;
    private PrivateKey chavePrivada;
    private Map<String, PublicKey> chavesPublicasConhecidas; // Mapa para armazenar chaves públicas conhecidas

    public Peer(int porta) throws NoSuchAlgorithmException {
        this.porta = porta;
        this.idPeer = UUID.randomUUID().toString();  // Gerar um ID único para o peer
        this.dht = new HashMap<>();  // Simulação de DHT em memória
        gerarChaves(); // Gera as chaves públicas e privadas
        this.chavesPublicasConhecidas = new HashMap<>();
    }

    // Gera um par de chaves RSA
    private void gerarChaves() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair parDeChaves = keyGen.generateKeyPair();
        chavePublica = parDeChaves.getPublic();
        chavePrivada = parDeChaves.getPrivate();
    }

    // Método para iniciar o peer como servidor
    public void iniciar() throws IOException {
        ServerSocket serverSocket = new ServerSocket(porta);
        System.out.println("Peer iniciado na porta " + porta);

        // Thread para escutar conexões de outros peers
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(new PeerHandler(socket, this)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void enviarMensagem(String idDestinatario, String mensagem) {
        InetSocketAddress peerAddress = dht.get(idDestinatario);
        if (peerAddress != null) {
            try {
                Socket socket = new Socket(peerAddress.getHostName(), peerAddress.getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                PublicKey chavePublicaDestinatario = chavesPublicasConhecidas.get(idDestinatario);
                if (chavePublicaDestinatario != null) {
                    byte[] mensagemCriptografada = criptografar(mensagem, chavePublicaDestinatario);
                    out.println(Base64.getEncoder().encodeToString(mensagemCriptografada));
                    System.out.println("Mensagem enviada para " + idDestinatario + ": " + mensagem);
                } else {
                    System.out.println("Chave pública do destinatário não encontrada.");
                }
                socket.close();
            } catch (IOException e) {
                System.out.println("Erro ao conectar ao peer: " + e.getMessage());
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Peer destinatário não encontrado.");
        }
    }
    
    // Método para registrar um peer na DHT
    public void registrarPeer(String idPeer, InetSocketAddress endereco) {
        dht.put(idPeer, endereco);
        System.out.println("Peer registrado: " + idPeer + " -> " + endereco);
    }

    // Método para armazenar a chave pública de um peer
    public void armazenarChavePublica(String idPeer, PublicKey chavePublica) {
        chavesPublicasConhecidas.put(idPeer, chavePublica);
    }

    // Método para criptografar uma mensagem
    private byte[] criptografar(String mensagem, PublicKey chavePublica) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, chavePublica);
        return cipher.doFinal(mensagem.getBytes());
    }

    // Método para descriptografar uma mensagem
    public String descriptografar(byte[] mensagemCriptografada) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, chavePrivada);
        byte[] mensagemDescriptografada = cipher.doFinal(mensagemCriptografada);
        return new String(mensagemDescriptografada);
    }

    public String getIdPeer() {
        return idPeer;
    }

    public int getPorta() {
        return porta;
    }

    public PublicKey getChavePublica() {
        return chavePublica;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // Inicializando o peer
        Peer peer1 = new Peer(8081);  // Exemplo de peer rodando na porta 8081
        peer1.iniciar();

        Peer peer2 = new Peer(8082); // Porta para o Peer 2
        peer2.iniciar();

        // Troca de chaves públicas (simulação)
        peer1.armazenarChavePublica("peer2", peer2.getChavePublica());
        peer2.armazenarChavePublica("peer1", peer1.getChavePublica());

        // Registro de peers
        peer1.registrarPeer("peer2", new InetSocketAddress("localhost", 8082));
        peer2.registrarPeer("peer1", new InetSocketAddress("localhost", 8081));

        // Exemplo de envio de mensagem para outro peer
        peer1.enviarMensagem("peer2", "Olá, Peer 2!");
    }
}

// Atualize a classe PeerHandler conforme abaixo

class PeerHandler implements Runnable {
    private Socket socket;
    private Peer peer;

    public PeerHandler(Socket socket, Peer peer) {
        this.socket = socket;
        this.peer = peer;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String mensagemCriptografada;
            while ((mensagemCriptografada = in.readLine()) != null) {
                byte[] mensagemDecodificada = Base64.getDecoder().decode(mensagemCriptografada);
                String mensagem = peer.descriptografar(mensagemDecodificada);
                System.out.println("Mensagem recebida de " + peer.getIdPeer() + ": " + mensagem);
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}