package com.example;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClient {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private String userName;
    private SecretKey secretKey;
    private BlockingQueue<Message> messageQueue;
    private DatagramSocket multicastSocket;
    private final int multicastPort = 4445; // Porta para multicast
    private final String multicastGroup = "230.0.0.0"; // Grupo multicast
    private List<String> discoveredPeers; // Lista de peers descobertos
    private ServerSocket serverSocket; // Socket para aceitar conexões

    public ChatClient(String serverAddress, int port, String userName) throws Exception {
        this.userName = userName;
        System.out.println("Attempting to connect to " + serverAddress + " on port " + port); // Log da tentativa de conexão
        this.socket = new Socket(serverAddress, port); // Tentar conectar ao servidor
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.secretKey = CryptoUtils.generateKey(); // Gerar chave secreta
        this.messageQueue = new LinkedBlockingQueue<>(); // Inicializar fila
        this.multicastSocket = new DatagramSocket(); // Criar socket para multicast
        this.discoveredPeers = new ArrayList<>(); // Inicializar a lista de peers

        // Iniciar thread para ouvir mensagens multicast
        new Thread(() -> listenForPeers()).start();

        // Inicializar o ServerSocket para aceitar conexões na mesma porta
        serverSocket = new ServerSocket(port); // Usar a mesma porta para o servidor
        System.out.println("Server listening on port " + port); // Log de confirmação
        new Thread(() -> acceptConnections()).start(); // Iniciar thread para aceitar conexões
    }

    public void sendMessage(String message) throws Exception {
        byte[] encryptedMessage = CryptoUtils.encrypt(message, secretKey);
        Message msg = new Message(userName, encryptedMessage);
        outputStream.writeObject(msg);
        outputStream.flush();

        // Persistir mensagem em JSON
        persistMessage(msg);
    }

    public void sendFile(File file) throws Exception {
        Message msg = new Message(userName, file); // Cria uma mensagem de arquivo
        outputStream.writeObject(msg); // Envia o arquivo
        outputStream.flush();

        // Persistir mensagem em JSON
        persistMessage(msg);
    }

    public Message receiveMessage() throws Exception {
        Message message = messageQueue.take(); // Aguarda até receber uma mensagem
        if (message.getFileName() != null) {
            message.saveFile("received_files"); // Salva o arquivo recebido
            System.out.println("File received: " + message.getFileName());
        }
        String decryptedContent = CryptoUtils.decrypt(message.getContent(), secretKey);
        return new Message(message.getSender(), decryptedContent.getBytes());
    }

    public void close() throws IOException {
        socket.close();
        multicastSocket.close(); // Fechar o multicastSocket
        serverSocket.close(); // Fechar o servidor quando não for mais necessário
    }

    private void persistMessage(Message msg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("conversas.json", true))) {
            writer.write(msg.toJson());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void discoverPeers() {
        try {
            String message = userName + " is available"; // Mensagem de anúncio
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(multicastGroup), multicastPort);
            multicastSocket.send(packet); // Enviar anúncio de disponibilidade
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForPeers() {
        try {
            MulticastSocket multicastSocket = new MulticastSocket(multicastPort);
            multicastSocket.joinGroup(InetAddress.getByName(multicastGroup));

            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet); // Receber mensagens multicast
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());

                // Adicionar peer descoberto à lista, se não estiver já presente
                if (!discoveredPeers.contains(receivedMessage)) {
                    discoveredPeers.add(receivedMessage);
                    System.out.println("Peer discovered: " + receivedMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void acceptConnections() {
        try {
            while (true) {
                Socket peerSocket = serverSocket.accept(); // Aceitar conexão de um peer
                System.out.println("Accepted connection from " + peerSocket.getRemoteSocketAddress()); // Log de conexão
                new Thread(() -> handlePeerConnection(peerSocket)).start(); // Iniciar thread para comunicação com o peer
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePeerConnection(Socket peerSocket) {
        try (ObjectInputStream peerInput = new ObjectInputStream(peerSocket.getInputStream());
             ObjectOutputStream peerOutput = new ObjectOutputStream(peerSocket.getOutputStream())) {
             
            // Loop para receber mensagens do peer
            while (true) {
                Message message = (Message) peerInput.readObject();
                messageQueue.offer(message); // Adiciona a mensagem à fila
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public List<String> getDiscoveredPeers() {
        return discoveredPeers; // Retornar a lista de peers descobertos
    }
}
