// Definição do pacote principal onde a classe Peer está localizada
package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.SwingUtilities;


/**
 * Classe Peer que representa um nó em uma rede P2P (Peer-to-Peer).
 * Cada Peer possui uma ID única, pares de chaves pública/privada para criptografia,
 * uma tabela de roteamento DHT simulada, e gerencia conversas criptografadas com outros peers.
 */
public class Peer {
    // Porta na qual o Peer está escutando conexões
    private int porta;
    
    // Identificador único do Peer
    private String idPeer;
    
    // Mapa que simula uma tabela DHT (Distributed Hash Table) para armazenar endereços de outros peers
    Map<String, InetSocketAddress> dht; // Simulação de DHT
    
    // Chaves pública e privada do Peer para criptografia assimétrica (RSA)
    private PublicKey chavePublica;
    private PrivateKey chavePrivada;
    
    // Mapa que armazena as chaves públicas conhecidas de outros peers
    private Map<String, PublicKey> chavesPublicasConhecidas;
    
    // Mapa que armazena conversas (mensagens) por conversa identificada pelo ID do destinatário
    // A lista de Strings alterna estre o remetente da mensagem e a mensagem em si
    Map<String, List<String>> conversas; // Para armazenar mensagens por conversa

    // Lista de ouvintes (listeners) para atualizar a interface gráfica (GUI) quando há novas mensagens
    private List<PeerGUIListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Construtor da classe Peer.
     * Inicializa a porta, gera uma ID única para o Peer, inicializa a DHT e gera as chaves pública e privada.
     *
     * @param porta Porta na qual o Peer irá escutar conexões
     * @param idPeer id do utilizador tem de ser exclusivo
     * @throws NoSuchAlgorithmException Caso o algoritmo de geração de chaves não seja encontrado
     */
    public Peer(int porta, String idPeer) throws NoSuchAlgorithmException {
        this.porta = porta;
        this.idPeer = idPeer;
        this.dht = new HashMap<>();
        gerarChaves(); // Gera as chaves pública e privada
        this.chavesPublicasConhecidas = new HashMap<>();
        this.conversas = new HashMap<>();
    }

    /**
     * Método para gerar o par de chaves pública e privada usando o algoritmo RSA.
     *
     * @throws NoSuchAlgorithmException Caso o algoritmo RSA não seja suportado
     */
    private void gerarChaves() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA"); // Obtém uma instância do gerador de pares de chaves RSA
        keyGen.initialize(2048); // Inicializa o gerador com tamanho de chave de 2048 bits
        KeyPair parDeChaves = keyGen.generateKeyPair(); // Gera o par de chaves
        chavePublica = parDeChaves.getPublic(); // Obtém a chave pública
        chavePrivada = parDeChaves.getPrivate(); // Obtém a chave privada
    }

    /**
     * Inicia o Peer para escutar conexões na porta especificada.
     * Cria um ServerSocket e lança uma nova thread para aceitar conexões de forma assíncrona.
     *
     * @throws IOException Caso ocorra um erro ao criar o ServerSocket
     */
    public void iniciar() throws IOException {
        ServerSocket serverSocket = new ServerSocket(porta);
        System.out.println("\nPeer iniciado na porta " + porta);
        
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept(); // Aceita uma conexão
                    // Chama o método para receber mensagens do peer conectado
                    receberMensagem(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    /**
     * Envia uma mensagem criptografada para o Peer destinatário especificado.
     * Utiliza criptografia assimétrica (RSA) para criptografar a chave simétrica (AES),
     * e criptografia simétrica para criptografar a mensagem.
     *
     * @param idDestinatario ID do Peer destinatário
     * @param mensagem Mensagem a ser enviada
     */
    public void enviarMensagem(String idDestinatario, String mensagem) {
        InetSocketAddress peerAddress = dht.get(idDestinatario); // Obtém o endereço do destinatário da DHT
        if (peerAddress != null) { // Verifica se o destinatário está registrado na DHT
            try (Socket socket = new Socket(peerAddress.getHostName(), peerAddress.getPort());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // Obtém a chave pública do destinatário
                PublicKey chavePublicaDestinatario = chavesPublicasConhecidas.get(idDestinatario);
                if (chavePublicaDestinatario != null) {
                    // Gera uma chave simétrica AES para criptografar a mensagem
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(256); // Inicializa com chave de 256 bits
                    SecretKey chaveSimetrica = keyGen.generateKey();
                    
                    // Criptografa a chave simétrica usando a chave pública do destinatário
                    byte[] chaveSimetricaCriptografada = criptografarChaveSimetrica(chaveSimetrica, chavePublicaDestinatario);

                    // Criptografa a mensagem usando a chave simétrica
                    byte[] mensagemCriptografada = criptografarMensagem(mensagem, chaveSimetrica);

                    // Estrutura da mensagem a ser enviada: idRemetente|chaveSimetricaCriptografada|mensagemCriptografada
                    out.println(idPeer + "|" +
                            Base64.getEncoder().encodeToString(chaveSimetricaCriptografada) + "|" +
                            Base64.getEncoder().encodeToString(mensagemCriptografada));

                    // Armazena a mensagem localmente e notifica a GUI
                    armazenarMensagem(idDestinatario, idPeer, mensagem);
                    System.out.println("Mensagem enviada para " + idDestinatario + ": " + mensagem);
                } else {
                    System.out.println("Chave pública do destinatário não encontrada.");
                }
            } catch (Exception e) {
                e.printStackTrace(); // Imprime a pilha de erros em caso de exceção
            }
        } else {
            System.out.println("Peer destinatário não encontrado."); // Caso o destinatário não esteja na DHT
        }
    }

    /**
     * Registra um Peer na DHT simulada, associando o ID do Peer ao seu endereço de rede.
     *
     * @param idPeer ID do Peer a ser registrado
     * @param endereco Endereço de rede (InetSocketAddress) do Peer
     */
    public void registrarPeer(String idPeer, InetSocketAddress endereco) {
        dht.put(idPeer, endereco); // Adiciona ou atualiza a entrada na DHT
        System.out.println("\nPeer registrado: " + idPeer + " -> " + endereco);
    }

    /**
     * Armazena a chave pública de um Peer conhecido.
     *
     * @param idPeer ID do Peer
     * @param chavePublica Chave pública do Peer
     */
    public void armazenarChavePublica(String idPeer, PublicKey chavePublica) {
        chavesPublicasConhecidas.put(idPeer, chavePublica);
    }

    /**
     * Método privado para criptografar a chave simétrica (AES) usando a chave pública RSA do destinatário.
     *
     * @param chaveSimetrica Chave simétrica a ser criptografada
     * @param chavePublica Chave pública do destinatário
     * @return Array de bytes representando a chave simétrica criptografada
     * @throws GeneralSecurityException Caso ocorra um erro durante a criptografia
     */
    private byte[] criptografarChaveSimetrica(SecretKey chaveSimetrica, PublicKey chavePublica) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA"); // Obtém uma instância do Cipher para RSA
        cipher.init(Cipher.ENCRYPT_MODE, chavePublica); // Inicializa o Cipher em modo de criptografia com a chave pública
        return cipher.doFinal(chaveSimetrica.getEncoded()); // Executa a criptografia e retorna o resultado
    }

    /**
     * Método privado para criptografar a mensagem usando a chave simétrica (AES).
     *
     * @param mensagem Mensagem a ser criptografada
     * @param chaveSimetrica Chave simétrica para criptografar a mensagem
     * @return Array de bytes representando a mensagem criptografada
     * @throws GeneralSecurityException Caso ocorra um erro durante a criptografia
     */
    private byte[] criptografarMensagem(String mensagem, SecretKey chaveSimetrica) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES"); // Obtém uma instância do Cipher para AES
        cipher.init(Cipher.ENCRYPT_MODE, chaveSimetrica); // Inicializa o Cipher em modo de criptografia com a chave simétrica
        return cipher.doFinal(mensagem.getBytes()); // Executa a criptografia e retorna o resultado
    }

    /**
     * Armazena uma mensagem recebida ou enviada em uma conversa específica.
     * Além disso, notifica a GUI sobre a nova mensagem.
     *
     * @param idDestinatario ID do Peer com quem a conversa está sendo mantida
     * @param mensagem Mensagem a ser armazenada
     */
    public void armazenarMensagem(String idDestinatario, String idRemetente, String mensagem) {
        // Adiciona a mensagem à lista de mensagens da conversa; cria a lista se não existir
        conversas.computeIfAbsent(idDestinatario, k -> new ArrayList<>()).add(idRemetente);
        conversas.computeIfAbsent(idDestinatario, k -> new ArrayList<>()).add(mensagem);
        // Notifica os ouvintes (listeners) que há uma nova mensagem
        notifyNewMessage(idDestinatario, mensagem);
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
    cipher.init(Cipher.DECRYPT_MODE, chavePrivada); // Usar a chave privada do Peer para descriptografar
    
    // Executa a descriptografia da chave simétrica
    byte[] chaveSimetricaDecodificada = cipher.doFinal(chaveSimetricaCriptografada);
    
    // Cria uma SecretKey a partir dos bytes da chave simétrica descriptografada, especificando o algoritmo AES
    return new SecretKeySpec(chaveSimetricaDecodificada, "AES");
}


    /**
     * Notifica todos os ouvintes (listeners) registrados sobre uma nova mensagem recebida.
     *
     * @param idDestinatario ID do Peer destinatário da mensagem
     * @param mensagem Mensagem recebida
     */
    public void notifyNewMessage(String idDestinatario, String mensagem) {
        for (PeerGUIListener listener : listeners) {
            listener.onNewMessage(idDestinatario, mensagem); // Chama o método onNewMessage de cada ouvinte
        }
    }

    /**
     * Adiciona um ouvinte (listener) para ser notificado sobre eventos da GUI.
     *
     * @param listener Ouvinte a ser adicionado
     */
    public void addListener(PeerGUIListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove um ouvinte (listener) da lista de ouvintes registrados.
     *
     * @param listener Ouvinte a ser removido
     */
    public void removeListener(PeerGUIListener listener) {
        listeners.remove(listener);
    }

    /**
     * Obtém a lista de mensagens de uma conversa específica com o Peer destinatário.
     *
     * @param idDestinatario ID do Peer destinatário
     * @return Lista de mensagens da conversa; retorna uma lista vazia se não houver conversas
     */
    public List<String> getMensagens(String idDestinatario) {
        return conversas.getOrDefault(idDestinatario, new ArrayList<>());
    }

    /**
 * Método para receber e processar mensagens de um peer remoto.
 *
 * @param socket Socket representando a conexão com o peer remoto
 */
public void receberMensagem(Socket socket) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        String mensagemCriptografada;
        while ((mensagemCriptografada = in.readLine()) != null) {
            // Divide a mensagem recebida em partes usando o delimitador "|"
            String[] partes = mensagemCriptografada.split("\\|");
            
            // Verifica se a mensagem está no formato esperado (3 partes)
            if (partes.length != 3) {
                System.out.println("Formato de mensagem inválido.");
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
            armazenarMensagem(idRemetente ,idRemetente, mensagem);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}


    /**
     * Obtém o ID único do Peer.
     *
     * @return ID do Peer
     */
    public String getIdPeer() {
        return idPeer;
    }

    /**
     * Obtém a chave pública do Peer.
     *
     * @return Chave pública
     */
    public PublicKey getChavePublica() {
        return chavePublica;
    }

    /**
     * Obtém a chave privada do Peer.
     *
     * @return Chave privada
     */
    public PrivateKey getChavePrivada() {
        return chavePrivada;
    }

    /**
     * Método main para inicializar e executar múltiplos Peers.
     * Cada Peer é iniciado em uma porta diferente, suas chaves públicas são armazenadas entre eles,
     * e GUIs são iniciadas para interação.
     *
     * @param args Argumentos da linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        try {
            // Inicializar três Peers em portas diferentes
            Peer peer1 = new Peer(8081, "peer1");
            peer1.iniciar();

            Peer peer2 = new Peer(8082, "peer2");
            peer2.iniciar();

            Peer peer3 = new Peer(8083, "peer3");
            peer3.iniciar();

            // Armazenar as chaves públicas entre os Peers para permitir comunicação segura
            peer1.armazenarChavePublica(peer2.getIdPeer(), peer2.getChavePublica());
            peer1.armazenarChavePublica(peer3.getIdPeer(), peer3.getChavePublica());
            peer2.armazenarChavePublica(peer1.getIdPeer(), peer1.getChavePublica());
            peer2.armazenarChavePublica(peer3.getIdPeer(), peer3.getChavePublica());
            peer3.armazenarChavePublica(peer1.getIdPeer(), peer1.getChavePublica());
            peer3.armazenarChavePublica(peer2.getIdPeer(), peer2.getChavePublica());

            // Registrar os Peers na DHT de cada um para que saibam os endereços uns dos outros
            peer1.registrarPeer(peer2.getIdPeer(), new InetSocketAddress("localhost", 8082));
            peer1.registrarPeer(peer3.getIdPeer(), new InetSocketAddress("localhost", 8083));
            peer2.registrarPeer(peer1.getIdPeer(), new InetSocketAddress("localhost", 8081));
            peer2.registrarPeer(peer3.getIdPeer(), new InetSocketAddress("localhost", 8083));
            peer3.registrarPeer(peer1.getIdPeer(), new InetSocketAddress("localhost", 8081));
            peer3.registrarPeer(peer2.getIdPeer(), new InetSocketAddress("localhost", 8082));

            // Iniciar a GUI para peer1
            SwingUtilities.invokeLater(() -> {
                try {
                    PeerGUI gui = new PeerGUI(peer1); // Cria a interface gráfica para peer1
                    gui.setVisible(true); // Torna a GUI visível
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Iniciar a GUI para peer2
            SwingUtilities.invokeLater(() -> {
                try {
                    PeerGUI gui = new PeerGUI(peer2); // Cria a interface gráfica para peer2
                    gui.setVisible(true); // Torna a GUI visível
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Iniciar a GUI para peer3
            SwingUtilities.invokeLater(() -> {
                try {
                    PeerGUI gui = new PeerGUI(peer3); // Cria a interface gráfica para peer3
                    gui.setVisible(true); // Torna a GUI visível
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace(); // Imprime a pilha de erros em caso de exceção
        }
    }
}
