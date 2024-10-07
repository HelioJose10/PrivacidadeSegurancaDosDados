package main.java;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class Peer {
    private int porta;
    private String idPeer;
    private Map<String, InetSocketAddress> dht; // Simulação de DHT
    private PublicKey chavePublica;
    private PrivateKey chavePrivada;
    private Map<String, PublicKey> chavesPublicasConhecidas;
    Map<String, List<String>> conversas; // Para armazenar mensagens por conversa

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

                    // Estrutura da mensagem: chave simétrica criptografada + mensagem criptografada
                    out.println(Base64.getEncoder().encodeToString(chaveSimetricaCriptografada) + "|" +
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

    public void armazenarMensagem(String idDestinatario, String mensagem) {
        conversas.computeIfAbsent(idDestinatario, k -> new ArrayList<>()).add(mensagem);
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

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
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

        // Iniciar a interface de cliente
        new ClientInterface(peer1).start();
    }
}

// Classe PeerHandler para lidar com as conexões de entrada
class PeerHandler implements Runnable {
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
                byte[] chaveSimetricaCriptografada = Base64.getDecoder().decode(partes[0]);
                byte[] mensagemDecodificada = Base64.getDecoder().decode(partes[1]);

                SecretKey chaveSimetrica = descriptografarChaveSimetrica(chaveSimetricaCriptografada);
                String mensagem = descriptografarMensagem(mensagemDecodificada, chaveSimetrica);
                System.out.println("\nMensagem recebida de " + peer.getIdPeer() + ": " + mensagem);
                peer.armazenarMensagem(peer.getIdPeer(), mensagem); // Armazena a mensagem recebida
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

class ClientInterface extends Thread {
    private Peer peer;
    private Scanner scanner;

    public ClientInterface(Peer peer) {
        this.peer = peer;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Enviar mensagem para Peer 2");
            System.out.println("2. Enviar mensagem para Peer 3");
            System.out.println("3. Listar conversas");
            System.out.println("4. Ver conteúdo de uma conversa");
            System.out.println("5. Sair");
            System.out.print("Escolha uma opção: ");

            try {
                int escolha = scanner.nextInt();
                scanner.nextLine(); // Consumir nova linha

                switch (escolha) {
                    case 1:
                    case 2:
                        enviarMensagem(escolha);
                        break;
                    case 3:
                        listarConversas();
                        break;
                    case 4:
                        verConteudoConversa();
                        break;
                    case 5:
                        System.out.println("\nSaindo...");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("\nOpção inválida!");
                }
            } catch (InputMismatchException e) {
                System.out.println("\nPor favor, insira um número válido.");
                scanner.nextLine(); // Limpar a entrada inválida
            }
        }
    }

    private void enviarMensagem(int escolha) {
        System.out.print("\nDigite a mensagem: ");
        String mensagem = scanner.nextLine();
        String destinatario = escolha == 1 ? "peer2" : "peer3"; // Determina o destinatário
        peer.enviarMensagem(destinatario, mensagem); // Enviar mensagem para o destinatário escolhido
    }

    private void listarConversas() {
        System.out.println("\nConversas:");
        for (String id : peer.conversas.keySet()) {
            System.out.println("- " + id);
        }
    }

    private void verConteudoConversa() {
        System.out.print("\nDigite o ID da conversa: ");
        String id = scanner.nextLine();
        List<String> mensagens = peer.getMensagens(id);
        if (!mensagens.isEmpty()) {
            System.out.println("\nMensagens da conversa com " + id + ":");
            for (String msg : mensagens) {
                System.out.println(msg);
            }
        } else {
            System.out.println("\nConversa não encontrada.");
        }
    }
}
