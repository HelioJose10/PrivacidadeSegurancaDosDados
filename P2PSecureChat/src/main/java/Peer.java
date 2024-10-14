package main.java;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Peer {
    private int porta;
    private String idPeer;
    Map<String, InetSocketAddress> dht; // Simulação de DHT
    private PublicKey chavePublica;
    private PrivateKey chavePrivada;
    private Map<String, PublicKey> chavesPublicasConhecidas;
    Map<String, List<String>> conversas; // Para armazenar mensagens por conversa

    // Lista de ouvintes para atualizar a GUI
    private List<PeerGUIListener> listeners = new CopyOnWriteArrayList<>();

    public Peer(int porta) throws NoSuchAlgorithmException {
        this.porta = porta;
        this.idPeer = UUID.randomUUID().toString();
        this.dht = new HashMap<>();
        gerarChaves();
        this.chavesPublicasConhecidas = new HashMap<>();
        this.conversas = new HashMap<>();
    }

    private void gerarChaves() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair parDeChaves = keyGen.generateKeyPair();
        chavePublica = parDeChaves.getPublic();
        chavePrivada = parDeChaves.getPrivate();
    }

    public void iniciar() throws IOException {
        ServerSocket serverSocket = new ServerSocket(porta);
        System.out.println("\nPeer iniciado na porta " + porta);
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
            try (Socket socket = new Socket(peerAddress.getHostName(), peerAddress.getPort());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                PublicKey chavePublicaDestinatario = chavesPublicasConhecidas.get(idDestinatario);
                if (chavePublicaDestinatario != null) {
                    // Gerar chave simétrica
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(256);
                    SecretKey chaveSimetrica = keyGen.generateKey();
                    byte[] chaveSimetricaCriptografada = criptografarChaveSimetrica(chaveSimetrica, chavePublicaDestinatario);

                    // Criptografar a mensagem
                    byte[] mensagemCriptografada = criptografarMensagem(mensagem, chaveSimetrica);

                    // Estrutura da mensagem: idRemetente|chaveSimetricaCriptografada|mensagemCriptografada
                    out.println(idPeer + "|" +
                            Base64.getEncoder().encodeToString(chaveSimetricaCriptografada) + "|" +
                            Base64.getEncoder().encodeToString(mensagemCriptografada));

                    armazenarMensagem(idDestinatario, mensagem);
                    System.out.println("Mensagem enviada para " + idDestinatario + ": " + mensagem);
                } else {
                    System.out.println("Chave pública do destinatário não encontrada.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Peer destinatário não encontrado.");
        }
    }

    public void registrarPeer(String idPeer, InetSocketAddress endereco) {
        dht.put(idPeer, endereco);
        System.out.println("\nPeer registrado: " + idPeer + " -> " + endereco);
    }

    public void armazenarChavePublica(String idPeer, PublicKey chavePublica) {
        chavesPublicasConhecidas.put(idPeer, chavePublica);
    }

    private byte[] criptografarChaveSimetrica(SecretKey chaveSimetrica, PublicKey chavePublica) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, chavePublica);
        return cipher.doFinal(chaveSimetrica.getEncoded());
    }

    private byte[] criptografarMensagem(String mensagem, SecretKey chaveSimetrica) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, chaveSimetrica);
        return cipher.doFinal(mensagem.getBytes());
    }

    // Atualize o método armazenarMensagem para notificar a GUI
    public void armazenarMensagem(String idDestinatario, String mensagem) {
        conversas.computeIfAbsent(idDestinatario, k -> new ArrayList<>()).add(mensagem);
        notifyNewMessage(idDestinatario, mensagem);
    }

    // Método para notificar os ouvintes sobre novas mensagens
    public void notifyNewMessage(String idDestinatario, String mensagem) {
        for (PeerGUIListener listener : listeners) {
            listener.onNewMessage(idDestinatario, mensagem);
        }
    }

    // Método para adicionar ouvintes
    public void addListener(PeerGUIListener listener) {
        listeners.add(listener);
    }

    // Método para remover ouvintes
    public void removeListener(PeerGUIListener listener) {
        listeners.remove(listener);
    }

    public List<String> getMensagens(String idDestinatario) {
        return conversas.getOrDefault(idDestinatario, new ArrayList<>());
    }

    public String getIdPeer() {
        return idPeer;
    }

    public PublicKey getChavePublica() {
        return chavePublica;
    }

    public PrivateKey getChavePrivada() {
        return chavePrivada;
    }

    public static void main(String[] args) {
        try {
            // Inicializar Peers
            Peer peer1 = new Peer(8081);
            peer1.iniciar();

            Peer peer2 = new Peer(8082);
            peer2.iniciar();

            Peer peer3 = new Peer(8083);
            peer3.iniciar();

            // Armazenar chaves públicas
            peer1.armazenarChavePublica("peer2", peer2.getChavePublica());
            peer1.armazenarChavePublica("peer3", peer3.getChavePublica());
            peer2.armazenarChavePublica("peer1", peer1.getChavePublica());
            peer2.armazenarChavePublica("peer3", peer3.getChavePublica());
            peer3.armazenarChavePublica("peer1", peer1.getChavePublica());
            peer3.armazenarChavePublica("peer2", peer2.getChavePublica());

            // Registrar peers
            peer1.registrarPeer("peer2", new InetSocketAddress("localhost", 8082));
            peer1.registrarPeer("peer3", new InetSocketAddress("localhost", 8083));
            peer2.registrarPeer("peer1", new InetSocketAddress("localhost", 8081));
            peer2.registrarPeer("peer3", new InetSocketAddress("localhost", 8083));
            peer3.registrarPeer("peer1", new InetSocketAddress("localhost", 8081));
            peer3.registrarPeer("peer2", new InetSocketAddress("localhost", 8082));

            // Iniciar a GUI para peer1
            SwingUtilities.invokeLater(() -> {
                try {
                    PeerGUI gui = new PeerGUI(peer1);
                    gui.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Opcional: Iniciar GUIs para peer2 e peer3 em threads separadas
            
            SwingUtilities.invokeLater(() -> {
                PeerGUI gui2 = new PeerGUI(peer2);
                gui2.setVisible(true);
            });

            SwingUtilities.invokeLater(() -> {
                PeerGUI gui3 = new PeerGUI(peer3);
                gui3.setVisible(true);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
