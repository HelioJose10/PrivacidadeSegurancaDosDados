package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*; // Import necessário para manipulação de arquivos
import java.util.List;

public class UserInterface {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField messageInput;
    private JTextField searchInput; // Campo de entrada para pesquisa
    private JTextField peerInput; // Campo para o endereço do peer
    private JList<String> peerList; // Lista para mostrar peers descobertos
    private DefaultListModel<String> peerListModel; // Modelo da lista
    private ChatClient chatClient;

    public UserInterface() {
        frame = new JFrame("P2P Chat Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // Ajuste o tamanho conforme necessário
        
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageInput = new JTextField();
        searchInput = new JTextField(); // Inicializa o campo de pesquisa
        peerInput = new JTextField(15); // Campo para o endereço do peer (15 é o número de colunas visíveis)

        peerListModel = new DefaultListModel<>();
        peerList = new JList<>(peerListModel); // Inicializa a lista de peers
        peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Menu de seleção de tema
        String[] themes = {"Default", "Dark", "Light"};
        JComboBox<String> themeSelector = new JComboBox<>(themes);
        themeSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeTheme((String) themeSelector.getSelectedItem());
            }
        });

        JButton connectButton = new JButton("Connect to Peer");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String peerAddress = peerInput.getText(); // Obtém o endereço do peer
                if (!peerAddress.isEmpty()) {
                    connectToPeer(peerAddress);
                } else {
                    messageArea.append("Please enter a valid peer address.\n");
                }
            }
        });

        JButton discoverPeersButton = new JButton("Discover Peers");
        discoverPeersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (chatClient != null) {
                    chatClient.discoverPeers(); // Anunciar disponibilidade
                    messageArea.append("Announcing availability to peers...\n");
                    updatePeerList(); // Atualiza a lista de peers
                } else {
                    messageArea.append("Error: Not connected to a peer.\n");
                }
            }
        });

        JButton loadConversationsButton = new JButton("Load Conversations");
        loadConversationsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadConversations(); // Chama o método para carregar conversas
            }
        });

        JButton sendFileButton = new JButton("Send File");
        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile(); // Método para enviar arquivo
            }
        });

        JButton emojiButton = new JButton("Emoji");
        emojiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EmojiPanel emojiPanel = new EmojiPanel(messageInput); // Painel de seleção de emojis
                emojiPanel.setVisible(true);
            }
        });

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchMessages(searchInput.getText());
            }
        });

        messageInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageInput.getText());
                messageInput.setText("");
            }
        });

        // Organizar os componentes
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Adiciona a lista de peers
        JPanel peerPanel = new JPanel();
        peerPanel.setLayout(new BorderLayout());
        peerPanel.add(new JScrollPane(peerList), BorderLayout.CENTER);
        
        JPanel connectPanel = new JPanel();
        connectPanel.add(new JLabel("Peer Address (IP:Port):")); // Rótulo para o campo de entrada
        connectPanel.add(peerInput); // Campo para o endereço do peer
        connectPanel.add(connectButton); // Botão de conexão
        connectPanel.add(discoverPeersButton); // Botão de descoberta
        peerPanel.add(connectPanel, BorderLayout.SOUTH);
        panel.add(peerPanel, BorderLayout.WEST); // Adiciona a lista de peers ao painel

        // Área de mensagens e entrada
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        messagePanel.add(messageInput, BorderLayout.SOUTH);
        
        panel.add(messagePanel, BorderLayout.CENTER); // Adiciona a área de mensagens ao painel

        // Adiciona botões ao painel superior
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loadConversationsButton);
        buttonPanel.add(sendFileButton);
        buttonPanel.add(emojiButton);
        buttonPanel.add(new JLabel("Search: "));
        buttonPanel.add(searchInput);
        buttonPanel.add(searchButton);
        buttonPanel.add(themeSelector); // Adiciona o menu de seleção de tema

        panel.add(buttonPanel, BorderLayout.NORTH); // Adiciona os botões ao painel

        frame.add(panel); // Adiciona o painel principal à janela
        frame.setVisible(true); // Certifique-se de que a janela está visível
    }

    private void changeTheme(String theme) {
        switch (theme) {
            case "Dark":
                frame.getContentPane().setBackground(Color.DARK_GRAY);
                messageArea.setBackground(Color.BLACK);
                messageArea.setForeground(Color.WHITE);
                messageInput.setBackground(Color.LIGHT_GRAY);
                break;
            case "Light":
                frame.getContentPane().setBackground(Color.WHITE);
                messageArea.setBackground(Color.LIGHT_GRAY);
                messageArea.setForeground(Color.BLACK);
                messageInput.setBackground(Color.WHITE);
                break;
            default: // Default
                frame.getContentPane().setBackground(Color.WHITE);
                messageArea.setBackground(Color.WHITE);
                messageArea.setForeground(Color.BLACK);
                messageInput.setBackground(Color.WHITE);
                break;
        }
    }

    private void connectToPeer(String peerAddress) {
        try {
            String[] parts = peerAddress.split(":"); // Obter endereço e porta do peer
            String host = parts[0]; // localhost
            int port = Integer.parseInt(parts[1]); // 12348 ou 12347
            System.out.println("Connecting to " + host + " on port " + port); // Log antes da conexão
            chatClient = new ChatClient(host, port, "User");
            messageArea.append("Connected to peer: " + peerAddress + "\n");
            new Thread(() -> receiveMessages()).start(); // Inicia a thread para receber mensagens
        } catch (Exception e) {
            messageArea.append("Failed to connect to peer: " + e.getMessage() + "\n");
            chatClient = null; // Se a conexão falhar, garantir que chatClient seja nulo
        }
    }

    private void sendMessage(String message) {
        if (chatClient == null) {
            messageArea.append("Error: Not connected to a peer.\n");
            return; // Não tenta enviar a mensagem se chatClient for nulo
        }

        try {
            chatClient.sendMessage(message);
            messageArea.append("You: " + message + "\n");
        } catch (Exception e) {
            messageArea.append("Failed to send message: " + e.getMessage() + "\n");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                chatClient.sendFile(selectedFile); // Envia o arquivo selecionado
                messageArea.append("File sent: " + selectedFile.getName() + "\n");
            } catch (Exception e) {
                messageArea.append("Failed to send file: " + e.getMessage() + "\n");
            }
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                Message message = chatClient.receiveMessage();
                if (message.getFileName() != null) {
                    messageArea.append(message.getSender() + " sent a file: " + message.getFileName() + "\n");
                    showNotification("File Received", "You received a file: " + message.getFileName());
                } else {
                    messageArea.append(message.getSender() + ": " + new String(message.getContent()) + "\n");
                    showNotification("New Message", message.getSender() + ": " + new String(message.getContent()));
                }
            }
        } catch (Exception e) {
            messageArea.append("Error receiving message: " + e.getMessage() + "\n");
        }
    }

    private void showNotification(String title, String message) {
        // Exibir uma notificação simples
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadConversations() {
        File file = new File("conversas.json");
        // Verifica se o arquivo existe, se não, cria um novo
        if (!file.exists()) {
            try {
                file.createNewFile(); // Cria um novo arquivo se não existir
                System.out.println("conversas.json created."); // Log de criação
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        
        // Agora, lê as conversas do arquivo
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Message msg = Message.fromJson(line); // Lê cada linha como JSON
                if (msg.getFileName() != null) {
                    messageArea.append(msg.getSender() + " sent a file: " + msg.getFileName() + "\n");
                } else {
                    messageArea.append(msg.getSender() + ": " + new String(msg.getContent()) + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void searchMessages(String query) {
        // Implementação básica da pesquisa
        StringBuilder results = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("conversas.json"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Message msg = Message.fromJson(line);
                if (msg.getFileName() != null) {
                    if (msg.getFileName().contains(query)) {
                        results.append(msg.getSender()).append(" sent a file: ").append(msg.getFileName()).append("\n");
                    }
                } else {
                    String content = new String(msg.getContent());
                    if (content.contains(query)) {
                        results.append(msg.getSender()).append(": ").append(content).append("\n");
                    }
                }
            }
            if (results.length() > 0) {
                messageArea.setText(results.toString()); // Atualiza a área de mensagens com os resultados da pesquisa
            } else {
                messageArea.append("No results found for: " + query + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePeerList() {
        peerListModel.clear(); // Limpa a lista antes de atualizar
        for (String peer : chatClient.getDiscoveredPeers()) {
            peerListModel.addElement(peer); // Adiciona cada peer à lista
        }
    }
}
