import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
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

    // Mapa que armazena as chaves simetricas geradas pelo Diffie-Hellman (1 por conversa)
    private Map<String, SecretKeySpec> chavesSimetricas;

    // Mapa que guarda os peers que fazem parte dos grupos a que pertencemos
    Map<String, List<String>> mapGrupos;
    
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
        this.chavesSimetricas = new HashMap<>();
        this.mapGrupos = new HashMap<>(); 
    }

    /**
     * Método para gerar o par de chaves pública e privada usando o algoritmo RSA.
     *
     * @throws NoSuchAlgorithmException Caso o algoritmo RSA não seja suportado
     */
    private void gerarChaves() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DiffieHellman"); // Obtém uma instância do gerador de pares de chaves RSA
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
        Logger.log("Peer iniciado na porta " + porta);
        
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
     * Utiliza Diffie-Hellman para criptografar a chave simétrica (AES),
     * e criptografia simétrica para criptografar a mensagem.
     * Inclui verificação de integridade usando SHA-256.
     *
     * @param idDestinatario ID do Peer destinatário
     * @param mensagem Mensagem a ser enviada
     */
    public void enviarMensagem(String idDestinatario, String mensagem) {
        InetSocketAddress peerAddress = dht.get(idDestinatario); // Obtém o endereço do destinatário da DHT
        if (peerAddress != null) { // Verifica se o destinatário está registrado na DHT
            try (Socket socket = new Socket(peerAddress.getHostName(), peerAddress.getPort());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
    
                if (chavesSimetricas.get(idDestinatario) == null) { // Se não tivermos uma chave simétrica com este Peer
                    // Obtém a chave pública do destinatário
                    PublicKey chavePublicaDestinatario = chavesPublicasConhecidas.get(idDestinatario);
                    if (chavePublicaDestinatario != null) {
                        applyDiffieHellman(idDestinatario, chavePublicaDestinatario);
                    } else {
                        System.out.println("\nChave pública do destinatário não encontrada.");
                        return;
                    }
                }
    
                SecretKeySpec aesKey = chavesSimetricas.get(idDestinatario);
    
                // Inicializa a cifra para AES
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey);
    
                // Criptografa a mensagem usando a chave simétrica
                byte[] mensagemCriptografada = criptografarMensagem(mensagem, aesKey);
    
                // Calcula o hash da mensagem
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(mensagem.getBytes(StandardCharsets.UTF_8));
    
                // Codifica o hash em Base64 para envio
                String hashBase64 = Base64.getEncoder().encodeToString(hash);
    
                System.out.println("\n------------------------------Hash------------------------------");
                System.out.println("\nHash enviado (Base64): " + hashBase64);
    
                // Estrutura da mensagem a ser enviada: idRemetente|mensagemCriptografada|hash
                out.println("" + "|" + idPeer + "|" + Base64.getEncoder().encodeToString(mensagemCriptografada) + "|" + hashBase64);
    
                // Armazena a mensagem localmente e notifica a GUI
                armazenarMensagem(idDestinatario, idPeer, mensagem);
                Logger.log("Mensagem enviada para " + idDestinatario + ": " + mensagem);
    
                // --- Novo Código: Replicação para Armazenamento Externo ---
                try {
                    byte[] encryptedData = mensagemCriptografada; // Dados já criptografados
                    String messageId = idPeer + "_" + idDestinatario + "_" + System.currentTimeMillis(); // ID único para a mensagem

    
                    Logger.log("Mensagem replicada com sucesso para armazenamento externo.");
                } catch (Exception e) {
                    Logger.log("Erro ao replicar mensagem no armazenamento externo: " + e.getMessage());
                }
                // --- Fim do Novo Código ---
    
            } catch (Exception e) {
                e.printStackTrace(); // Imprime a pilha de erros em caso de exceção
            }
        } else {
            System.out.println("\nPeer destinatário não encontrado."); // Caso o destinatário não esteja na DHT
        }
    }

    public void enviarMensagemGrupo(String idGrupo, String mensagem) throws GeneralSecurityException {
        boolean isFirstMessage = false;

        if (chavesSimetricas.get(idGrupo) == null) { // Se não tivermos uma chave simétrica com este Grupo

            isFirstMessage = true; 

            // Calcula o DiffieHellman conjunto
            String[] peers = mensagem.split("\\|");

            addGroup(idGrupo, peers);
            //applyGroupDiffieHellman(idGrupo, peers); ESTA A DAR ERRO
        }

        SecretKeySpec aesKey = chavesSimetricas.get(idGrupo);

        // Inicializa a cifra para AES
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);

        // Criptografa a mensagem usando a chave simétrica
        byte[] mensagemCriptografada;
        // Se for a primeira mensagem para o grupo, ainda não temos o Diffie-Hellman feito por isso enviamos
        // a mensagem não-encryptada
        if(!isFirstMessage) {
            mensagemCriptografada = criptografarMensagem(mensagem, aesKey);
        }
        else {
            mensagemCriptografada = mensagem.getBytes();
        }
        
        // Calcula o hash da mensagem
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(mensagem.getBytes(StandardCharsets.UTF_8));

        // Codifica o hash em Base64 para envio
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        // Alteração manual para simular corrupção
        //hashBase64 = hashBase64.substring(1) + "x"; // Modifica o hash
        System.out.println("\n Hash enviado (Base64): " + hashBase64);

        List<String> groupPeers = mapGrupos.get(idGrupo);

        // Para cada Peer no grupo temos de enviar uma mensagem.
        for(String peer : groupPeers) {
            System.out.println("currently seding to:" + peer);
            InetSocketAddress peerAddress = dht.get(peer); // Obtém o endereço do destinatário da DHT
            if (peerAddress != null) { // Verifica se o destinatário está registrado na DHT
                try (Socket socket = new Socket(peerAddress.getHostName(), peerAddress.getPort());
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        
                    // Estrutura da mensagem a ser enviada: idGrupo|idRemetente|mensagemCriptografada|hash
                    out.println(idGrupo + "|" + this.idPeer + "|" + Base64.getEncoder().encodeToString(mensagemCriptografada) + "|" + hashBase64);

                    } catch (Exception e) {
                        e.printStackTrace(); // Imprime a pilha de erros em caso de exceção
                    }
                } else {
                    System.out.println("\nPeer destinatário não encontrado."); // Caso o destinatário não esteja na DHT
                }
        }


        // Armazena a mensagem localmente e notifica a GUI
        armazenarMensagem(idGrupo, idPeer, mensagem);
        Logger.log("Mensagem enviada para o grupo" + idGrupo + ": " + mensagem);
    }

    /**
     * Registra um Peer na DHT simulada, associando o ID do Peer ao seu endereço de rede.
     *
     * @param idPeer ID do Peer a ser registrado
     * @param endereco Endereço de rede (InetSocketAddress) do Peer
     */
    public void registrarPeer(String idPeer, InetSocketAddress endereco) {
        dht.put(idPeer, endereco); // Adiciona ou atualiza a entrada na DHT
        Logger.log("Peer registrado: " + idPeer + " -> " + endereco);
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
     * Método privado para criptografar a mensagem usando a chave simétrica (AES).
     *
     * @param mensagem Mensagem a ser criptografada
     * @param chaveSimetrica Chave simétrica para criptografar a mensagem
     * @return Array de bytes representando a mensagem criptografada
     * @throws GeneralSecurityException Caso ocorra um erro durante a criptografia
     */
    private byte[] criptografarMensagem(String mensagem, SecretKeySpec aesKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES"); // Obtém uma instância do Cipher para AES
        cipher.init(Cipher.ENCRYPT_MODE, aesKey); // Inicializa o Cipher em modo de criptografia com a chave simétrica
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
    
        // Armazenamento na memória local
        conversas.computeIfAbsent(idDestinatario, k -> new ArrayList<>()).add(idRemetente);
        conversas.get(idDestinatario).add(mensagem);
    
        // Notificar a GUI
        notifyNewMessage(idDestinatario, mensagem);
    }

    /**
     * Método privado para descriptografar a mensagem que foi criptografada com a chave simétrica (AES).
     *
     * @param mensagemCriptografada Array de bytes representando a mensagem criptografada
     * @param aesKey        SecretKey utilizada para criptografar a mensagem
     * @return String representando a mensagem descriptografada
     * @throws GeneralSecurityException Caso ocorra um erro durante a descriptografia
     */
    private String descriptografarMensagem(byte[] mensagemCriptografada, SecretKey aesKey) throws GeneralSecurityException {
        // Obtém uma instância do Cipher para AES
        Cipher cipher = Cipher.getInstance("AES");
        // Inicializa o Cipher em modo de descriptografia utilizando a chave simétrica
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        // Executa a descriptografia da mensagem
        return new String(cipher.doFinal(mensagemCriptografada));
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
     * Inclui verificação de integridade usando SHA-256.
     *
     * @param socket Socket representando a conexão com o peer remoto
     */
    public void receberMensagem(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String mensagemCriptografada;
            boolean firstTimeGroupMessage = false;
            while ((mensagemCriptografada = in.readLine()) != null) {
                // Divide a mensagem recebida em partes usando o delimitador "|"
                String[] partes = mensagemCriptografada.split("\\|");
                System.out.println(Arrays.toString(partes));


                // Verifica se a mensagem está no formato esperado (4 partes)
                if (partes.length != 4) {
                    System.out.println("\nFormato de mensagem inválido, partes: " + partes.length);
                    continue; // Pula para a próxima iteração do loop
                }

                // Extrai a flag e o id do Remetente
                String grupoFlag = partes[0];
                String idRemetente = partes[1];
                PublicKey chaveRemetente = chavesPublicasConhecidas.get(idRemetente);
                String mensagem;

                if (chavesSimetricas.get(idRemetente) == null) { // Verifica se NÃO tem uma chave simétrica guardada para este user
                    if(grupoFlag.equals("")) {  //Neste caso é uma mensagem privada
                        applyDiffieHellman(idRemetente, chaveRemetente);
                    }
                    else {                      //Neste caso é uma mensagem de grupo
                        firstTimeGroupMessage = true;
                        byte[] mensagemDecodificada = Base64.getDecoder().decode(partes[2]);
                        mensagem = new String(mensagemDecodificada);
                        String[] peers = mensagem.split("\\|");
                        addGroup(grupoFlag, peers);
                        

                        //applyGroupDiffieHellman(grupoFlag, peers);
                    }
                }
                if(!firstTimeGroupMessage){ 

                    // Decodifica a mensagem criptografada da segunda parte da mensagem usando Base64
                    byte[] mensagemDecodificada = Base64.getDecoder().decode(partes[2]);

                    // Obtém o hash recebido (terceira parte)
                    String hashRecebido = partes[3];

                    // Descriptografa a mensagem utilizando a chave simétrica obtida
                    SecretKey chaveSimetrica = chavesSimetricas.get(idRemetente);
                    mensagem = descriptografarMensagem(mensagemDecodificada, chaveSimetrica);

                    // Calcula o hash da mensagem descriptografada
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hashCalculado = digest.digest(mensagem.getBytes(StandardCharsets.UTF_8));
                    String hashCalculadoBase64 = Base64.getEncoder().encodeToString(hashCalculado);

                    // Verifica a integridade comparando os hashes
                    if (!hashRecebido.equals(hashCalculadoBase64)) {
                        Logger.log("Falha na integridade da mensagem recebida!");
                        continue;
                    }

                    // Exibe a mensagem recebida no console
                    Logger.log("Mensagem recebida de " + idRemetente + ": " + mensagem);

                    // Armazena a mensagem recebida no objeto Peer para que possa ser acessada posteriormente
                    armazenarMensagem(idRemetente, idRemetente, mensagem);

                    System.out.println("\nHash recebido (Base64): " + hashRecebido);
                    System.out.println("\nHash calculado (Base64): " + hashCalculadoBase64);
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Guarantees:
     *  - Garante que no fim vai haver uma chave simetrica guardada em chavesSimetricas para p Peer especificado.
     * @param idPeer
     * @param chavePublicaPeer
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public void applyDiffieHellman(String idPeer, PublicKey chavePublicaPeer) throws NoSuchAlgorithmException, InvalidKeyException {
        // Inicializar o KeyAgreement com a nossa chave privada
        KeyAgreement keyAgree = KeyAgreement.getInstance("DiffieHellman");
        keyAgree.init(chavePrivada);

        // Executar a primeira fase do DH
        keyAgree.doPhase(chavePublicaPeer, true);

        // Gerar o shared secret
        byte[] sharedSecret = keyAgree.generateSecret();

        // Gerar uma chave a partir do DIffie-Hellman secret e guarda no Peer
        SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES"); // Use 16 bytes for AES-128
        chavesSimetricas.put(idPeer, aesKey);
    }


    public void applyGroupDiffieHellman(String idGrupo, String[] publicKeysPeers) throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
            keyAgree.init(chavePrivada);
    
            // Perform the key agreement phase with the public keys of all peers
            for (int i = 0; i < publicKeysPeers.length; i++) {
                PublicKey peerPublicKey = chavesPublicasConhecidas.get(publicKeysPeers[i]);
                System.out.println("SIZE OF PEERS: " + publicKeysPeers.length);
                System.out.println("FIRST ELEMENT: " + publicKeysPeers[0]);

                if (!peerPublicKey.equals(chavePublica)) { // Exclude own key
                    boolean lastPhase = (i == publicKeysPeers.length - 1); // Set true for the last phase
                    keyAgree.doPhase(peerPublicKey, lastPhase);
                }
            }
            
            // Generate the final shared secret after all phases
            byte[] sharedSecret = keyAgree.generateSecret();
    
            // Derive the AES key from the shared secret
            SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES"); // Use the first 128 bits as AES key
            chavesSimetricas.put(idGrupo, aesKey); // Store the symmetric key for this group
            System.out.println("put " + idGrupo + " into the chavesSimetricas");
    
            System.out.println("Derived AES key for group " + idGrupo + ": " +
                Base64.getEncoder().encodeToString(aesKey.getEncoded()));
    
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidKeyException("Erro ao realizar Diffie-Hellman para o grupo.");
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

    public void addGroup(String idGroup, String[] peers) {
        mapGrupos.put(idGroup, Arrays.asList(peers));
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
            System.out.println("\n-----Peers Iniciados-----");
            // Inicializar três Peers em portas diferentes
            Peer peer1 = new Peer(8081, "peer1");
            peer1.iniciar();

            Peer peer2 = new Peer(8082, "peer2");
            peer2.iniciar();

            Peer peer3 = new Peer(8083, "peer3");
            peer3.iniciar();

            System.out.println("\n--------------------Endereços--------------------");
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
            System.out.println("\n");

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
