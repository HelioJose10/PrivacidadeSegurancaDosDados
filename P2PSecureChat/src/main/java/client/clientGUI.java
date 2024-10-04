package main.java.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import main.java.security.EncryptionUtils;  // Importa a classe de criptografia

public class clientGUI {

    private static JTextArea messageArea;
    private static JTextField inputField;
    private static List<String> peerAddresses = new ArrayList<>();  // Armazena endereços de peers disponíveis
    private static String secretKey = "1234567890123456";  // Chave de 128 bits
    private static DatagramSocket socket;
    private static Component frame;

    public static void main(String[] args) {
        JFrame frame = new JFrame("P2P Messaging App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        messageArea = new JTextArea();
        messageArea.setEditable(false);  // Área para mostrar mensagens, não editável
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(300, 30));  // Campo de texto para input de mensagem

        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JPanel panel = new JPanel();  // Painel inferior com campo de input e botão
        panel.setLayout(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);  // Área de mensagens no centro
        frame.getContentPane().add(panel, BorderLayout.SOUTH);  // Painel de input e botão na parte inferior

        frame.setVisible(true);

        // Iniciar servidor para receber mensagens
        new Thread(() -> startServer()).start();

        // Iniciar broadcast para descobrir peers
        new Thread(() -> discoverPeers()).start();
    }

    // Método para iniciar o servidor
    private static void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(12350);  // Altere a porta aqui
            while (true) {
                Socket socket = serverSocket.accept();  // Aceitar conexão de outro peer
                new Thread(() -> handleConnection(socket)).start();  // Tratar cada conexão em uma nova thread
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para lidar com a conexão
    private static void handleConnection(Socket socket) {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            String encryptedMessage = (String) in.readObject();
            // Descriptografar a mensagem recebida
            String decryptedMessage = EncryptionUtils.decrypt(encryptedMessage, secretKey);
            messageArea.append("Recebido de um peer: " + decryptedMessage + "\n");
            in.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            messageArea.append("Erro ao receber a mensagem.\n");
        }
    }
    private static void discoverPeers() {
        try {
            socket = new DatagramSocket();  // Socket UDP
            socket.setBroadcast(true);  // Habilitar broadcast
    
            // Enviar pacote de broadcast
            byte[] buf = "DISCOVER_PEER".getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("255.255.255.255"), 8888);
            socket.send(packet);
            System.out.println("Pacote de descoberta enviado."); // Mensagem de depuração
    
            // Ouvir respostas de peers
            byte[] recvBuf = new byte[1024];
            while (true) {
                DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(recvPacket);  // Receber resposta
                String response = new String(recvPacket.getData(), 0, recvPacket.getLength());
                System.out.println("Resposta recebida: " + response); // Mensagem de depuração
    
                if (response.startsWith("PEER:")) {
                    String peerAddress = response.substring(5);  // Extrair endereço do peer
                    if (!peerAddresses.contains(peerAddress)) {
                        peerAddresses.add(peerAddress);
                        messageArea.append("Peer descoberto: " + peerAddress + "\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    // Método para enviar mensagem
    private static void sendMessage() {
        String message = inputField.getText();
        if (message != null && !message.isEmpty()) {
            try {
                // Criptografar a mensagem antes de enviar
                String encryptedMessage = EncryptionUtils.encrypt(message, secretKey);
                String peerIP = (String) JOptionPane.showInputDialog(frame, "Escolha um peer:", "Enviar mensagem", JOptionPane.QUESTION_MESSAGE, null, peerAddresses.toArray(), peerAddresses.isEmpty() ? null : peerAddresses.get(0));

                if (peerIP != null) {
                    Socket socket = new Socket(peerIP, 12345);  // Conectar ao peer destino
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(encryptedMessage);  // Enviar a mensagem criptografada
                    out.close();
                    socket.close();
                    messageArea.append("Enviado (Criptografado): " + message + "\n");  // Exibe a mensagem no cliente
                    inputField.setText("");  // Limpa o campo de input
                }
            } catch (Exception e) {
                e.printStackTrace();
                messageArea.append("Erro ao criptografar ou enviar a mensagem.\n");
            }
        }
    }
}
