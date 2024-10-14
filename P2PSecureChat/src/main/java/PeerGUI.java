package main.java;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class PeerGUI extends JFrame implements PeerGUIListener {
    private Peer peer;
    private JTextArea textAreaConversa;
    private JComboBox<String> comboBoxDestinatario;
    private JTextField textFieldMensagem;
    private DefaultListModel<String> listModelConversas;
    private JList<String> listConversas;

    public PeerGUI(Peer peer) {
        this.peer = peer;
        this.peer.addListener(this);
        initialize();
    }

    private void initialize() {
        setTitle("Interface Peer - ID: " + peer.getIdPeer());
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza a janela

        // Layout principal
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(panel);

        // Painel superior para seleção de destinatário e mensagem
        JPanel panelTopo = new JPanel(new BorderLayout(5, 5));
        panel.add(panelTopo, BorderLayout.NORTH);

        // ComboBox para selecionar o destinatário
        comboBoxDestinatario = new JComboBox<>();
        atualizarDestinatarios();
        panelTopo.add(comboBoxDestinatario, BorderLayout.WEST);

        // Campo de texto para a mensagem
        textFieldMensagem = new JTextField();
        panelTopo.add(textFieldMensagem, BorderLayout.CENTER);

        // Botão para enviar a mensagem
        JButton btnEnviar = new JButton("Enviar");
        panelTopo.add(btnEnviar, BorderLayout.EAST);

        // Painel central para conversas
        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(panelCentro, BorderLayout.CENTER);

        // Lista de conversas
        listModelConversas = new DefaultListModel<>();
        listConversas = new JList<>(listModelConversas);
        JScrollPane scrollConversas = new JScrollPane(listConversas);
        scrollConversas.setBorder(BorderFactory.createTitledBorder("Conversas"));
        panelCentro.add(scrollConversas);

        // Área de texto para exibir mensagens
        textAreaConversa = new JTextArea();
        textAreaConversa.setEditable(false);
        JScrollPane scrollArea = new JScrollPane(textAreaConversa);
        scrollArea.setBorder(BorderFactory.createTitledBorder("Mensagens"));
        panelCentro.add(scrollArea);

        // Atualizar a lista de conversas
        atualizarListaConversas();

        // Listeners
        btnEnviar.addActionListener(e -> enviarMensagem());
        listConversas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                exibirConversasSelecionada();
            }
        });

        // Atualizar periodicamente a lista de destinatários
        Timer timer = new Timer(5000, e -> atualizarDestinatarios());
        timer.start();
    }

    private void atualizarDestinatarios() {
        SwingUtilities.invokeLater(() -> {
            comboBoxDestinatario.removeAllItems();
            for (String idPeer : peer.dht.keySet()) {
                if (!idPeer.equals(peer.getIdPeer())) { // Não incluir a si mesmo
                    comboBoxDestinatario.addItem(idPeer);
                }
            }
        });
    }

    private void atualizarListaConversas() {
        SwingUtilities.invokeLater(() -> {
            listModelConversas.clear();
            for (String id : peer.conversas.keySet()) {
                listModelConversas.addElement(id);
            }
        });
    }

    private void enviarMensagem() {
        String destinatario = (String) comboBoxDestinatario.getSelectedItem();
        String mensagem = textFieldMensagem.getText().trim();

        if (destinatario == null) {
            JOptionPane.showMessageDialog(this, "Selecione um destinatário.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (mensagem.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite uma mensagem.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        peer.enviarMensagem(destinatario, mensagem);
        textFieldMensagem.setText("");

        // Atualizar a lista de conversas
        atualizarListaConversas();
        exibirMensagens(destinatario);
    }

    private void exibirConversasSelecionada() {
        String selecionado = listConversas.getSelectedValue();
        if (selecionado != null) {
            exibirMensagens(selecionado);
        }
    }

    private void exibirMensagens(String idPeer) {
        List<String> mensagens = peer.getMensagens(idPeer);
        StringBuilder sb = new StringBuilder();
        for (String msg : mensagens) {
            sb.append(msg).append("\n");
        }
        textAreaConversa.setText(sb.toString());
    }

    @Override
    public void onNewMessage(String idDestinatario, String mensagem) {
        // Atualizar a lista de conversas
        atualizarListaConversas();

        // Se a conversa atual for a mesma, atualizar a área de mensagens
        String selecionado = listConversas.getSelectedValue();
        if (selecionado != null && selecionado.equals(idDestinatario)) {
            exibirMensagens(idDestinatario);
        }
    }
}
