import java.awt.BorderLayout; // Importa componentes da biblioteca Swing para criar a GUI
import java.awt.GridLayout; // Importa a borda vazia
import java.security.GeneralSecurityException;
import java.util.List; // Importa classes de layout e componentes gráficos

import javax.swing.BorderFactory; // Importa classes para tratamento de eventos
import javax.swing.DefaultListModel; // Importa a classe List do Java
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;

// Classe que representa a interface gráfica do usuário para o Peer
public class PeerGUI extends JFrame implements PeerGUIListener {
    private Peer peer; // Referência ao objeto Peer, que contém a lógica do aplicativo
    private JTextArea textAreaConversa; // Área de texto para exibir mensagens da conversa
    private JComboBox<String> comboBoxDestinatario; // ComboBox para selecionar o destinatário
    private JList<String> peerList; //JList para criar grupos
    private DefaultListModel<String> listModel;
    private JTextField textFieldMensagem; // Campo de texto para digitar a mensagem
    private JTextField textFieldGroup; // Campo de texto para nome do grupo
    private DefaultListModel<String> listModelConversas; // Modelo da lista para gerenciar as conversas
    private JList<String> listConversas; // Lista que exibe as conversas disponíveis

    // Construtor da classe PeerGUI
    public PeerGUI(Peer peer) {
        this.peer = peer; // Inicializa a referência ao objeto Peer
        this.peer.addListener(this); // Adiciona esta GUI como um ouvinte para eventos de mensagens
        initialize(); // Chama o método para inicializar a interface
    }

    // Método para inicializar a interface gráfica
    private void initialize() {
        // Configurações da janela principal
        setTitle("Interface Peer - ID: " + peer.getIdPeer()); // Título da janela com o ID do Peer
        setSize(600, 500); // Define o tamanho da janela
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Fecha o aplicativo ao fechar a janela
        setLocationRelativeTo(null); // Centraliza a janela na tela

        // Layout principal da interface
        JPanel panel = new JPanel(new BorderLayout(10, 10)); // Painel com layout de borda
        panel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Define uma borda vazia ao redor do painel
        setContentPane(panel); // Define o painel como o conteúdo da janela

        // Painel superior para seleção de destinatário e entrada de mensagem
        JPanel panelTopo = new JPanel(new BorderLayout(5, 5)); // Painel com layout de borda
        panel.add(panelTopo, BorderLayout.NORTH); // Adiciona o painel ao topo do painel principal

        // Painel superior para seleção de destinatário e entrada de mensagem
        JPanel panelBot = new JPanel(new BorderLayout(5, 5)); // Painel com layout de borda
        panel.add(panelBot, BorderLayout.SOUTH); // Adiciona o painel ao topo do painel principal

        // Painel com nomes para criar grupos
        listModel = new DefaultListModel<>();
        peerList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(peerList);
        scrollPane.setPreferredSize(new Dimension(100, 30)); // Limit size of the JScrollPane
        panelBot.add(scrollPane, BorderLayout.WEST); // Adiciona o campo ao painel inferior

        // Campo de texto para o nome do grupo
        textFieldGroup = new JTextField("Nome do Grupo"); // Inicializa o campo de texto
        panelBot.add(textFieldGroup, BorderLayout.CENTER); // Adiciona o campo ao painel superior
        
        // ComboBox para selecionar o destinatário
        comboBoxDestinatario = new JComboBox<>(); // Inicializa o ComboBox
        atualizarDestinatarios(); // Atualiza a lista de destinatários disponíveis
        panelTopo.add(comboBoxDestinatario, BorderLayout.WEST); // Adiciona o ComboBox ao painel superior

        // Campo de texto para a mensagem
        textFieldMensagem = new JTextField(); // Inicializa o campo de texto
        panelTopo.add(textFieldMensagem, BorderLayout.CENTER); // Adiciona o campo ao painel superior

        // Botão para enviar a mensagem
        JButton btnEnviar = new JButton("Enviar"); // Cria o botão de enviar
        panelTopo.add(btnEnviar, BorderLayout.EAST); // Adiciona o botão ao painel superior

        // Botão para criar um grupo
        JButton btnGroup = new JButton("Criar Grupo"); // Cria o botão de criar grupo
        panelBot.add(btnGroup, BorderLayout.EAST); // Adiciona o botão ao painel superior

        // Painel central para exibir conversas e mensagens
        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 10, 10)); // Painel com grid layout
        panel.add(panelCentro, BorderLayout.CENTER); // Adiciona o painel central ao painel principal

        // Lista de conversas
        listModelConversas = new DefaultListModel<>(); // Modelo para a lista de conversas
        listConversas = new JList<>(listModelConversas); // Inicializa a lista de conversas
        JScrollPane scrollConversas = new JScrollPane(listConversas); // Adiciona rolagem à lista
        scrollConversas.setBorder(BorderFactory.createTitledBorder("Conversas")); // Define título para a lista
        panelCentro.add(scrollConversas); // Adiciona a lista ao painel central

        // Área de texto para exibir mensagens
        textAreaConversa = new JTextArea(); // Inicializa a área de texto
        textAreaConversa.setEditable(false); // Define a área como não editável
        JScrollPane scrollArea = new JScrollPane(textAreaConversa); // Adiciona rolagem à área de texto
        scrollArea.setBorder(BorderFactory.createTitledBorder("Mensagens")); // Define título para a área de texto
        panelCentro.add(scrollArea); // Adiciona a área de texto ao painel central

        // Atualiza a lista de conversas ao iniciar
        atualizarListaConversas();

        // Listeners para eventos
        btnEnviar.addActionListener(e -> {
            try {
                enviarMensagem();
            } catch (GeneralSecurityException e1) {
                e1.printStackTrace();
            }
        }); // Ação ao clicar no botão de enviar
        listConversas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Verifica se a seleção foi finalizada
                exibirConversasSelecionada(); // Exibe as mensagens da conversa selecionada
            }
        });

        // Listeners para evento de criar Grupo
        btnGroup.addActionListener(e -> {
            try {
                criarGrupo();
            } catch (GeneralSecurityException e1) {
                e1.printStackTrace();
            }
        }); // Ação ao clicar no botão de enviar

        // Atualiza periodicamente a lista de destinatários a cada 5 segundos
        Timer timer = new Timer(5000, e -> atualizarDestinatarios());
        timer.start(); // Inicia o timer
    }

    // Método para atualizar a lista de destinatários disponíveis
    private void atualizarDestinatarios() {
        SwingUtilities.invokeLater(() -> { // Garante que a atualização ocorra na thread da GUI
            comboBoxDestinatario.removeAllItems(); // Remove todos os itens atuais
            listModel.clear();
            // Adiciona todos os peers conhecidos, exceto a si mesmo
            for (String idPeer : peer.dht.keySet()) {
                if (!idPeer.equals(peer.getIdPeer())) {
                    comboBoxDestinatario.addItem(idPeer); // Adiciona o peer ao ComboBox  
                    listModel.addElement(idPeer);
                }
            }
            for(String idGroup : peer.mapGrupos.keySet()) {
                comboBoxDestinatario.addItem(idGroup); // Adiciona o ID da conversa
            }
        });
    }

    // Método para atualizar a lista de conversas
    private void atualizarListaConversas() {
        SwingUtilities.invokeLater(() -> { // Garante que a atualização ocorra na thread da GUI
            listModelConversas.clear(); // Limpa a lista de conversas
            // Adiciona todos os IDs de conversa ao modelo
            for (String id : peer.conversas.keySet()) {
                for(String idGroup : peer.mapGrupos.keySet()) {
                    listModelConversas.addElement(idGroup); // Adiciona o ID da conversa
                }
                listModelConversas.addElement(id); // Adiciona o ID da conversa
            }
        });
    }

    // Método para enviar mensagem
    private void enviarMensagem() throws GeneralSecurityException {
        // Obtém o destinatário selecionado e a mensagem a ser enviada
        String destinatario = (String) comboBoxDestinatario.getSelectedItem();
        String mensagem = textFieldMensagem.getText().trim(); // Remove espaços em branco

        // Verifica se um destinatário foi selecionado
        if (destinatario == null) {
            JOptionPane.showMessageDialog(this, "Selecione um destinatário.", "Erro", JOptionPane.ERROR_MESSAGE);
            return; // Retorna se nenhum destinatário foi selecionado
        }

        // Verifica se a mensagem não está vazia
        if (mensagem.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite uma mensagem.", "Erro", JOptionPane.ERROR_MESSAGE);
            return; // Retorna se a mensagem estiver vazia
        }

        // Envia a mensagem através do peer
        if(peer.mapGrupos.containsKey(destinatario)) { // se o destinatario for um grupo
            peer.enviarMensagemGrupo(destinatario, mensagem);
        }
        else {
            peer.enviarMensagem(destinatario, mensagem);
        }
        textFieldMensagem.setText(""); // Limpa o campo de texto após enviar

        // Atualiza a lista de conversas
        atualizarListaConversas();
        exibirMensagens(destinatario); // Exibe as mensagens enviadas
    }

    private void criarGrupo() throws GeneralSecurityException {
        // Obtém os Peers selecionados e o nome do grupo
        List<String> selectedPeers = peerList.getSelectedValuesList(); // Get selected items
        String nomeGrupo = textFieldGroup.getText().trim(); // Remove espaços em branco

        // Print para debug
        System.out.println("Selected Peers: " + selectedPeers);
        System.out.println("Group Name: " + nomeGrupo);

        if (selectedPeers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione pelo menos um Peer.", "Erro", JOptionPane.ERROR_MESSAGE);
            return; // Retorna se nenhum Peer foi selecionado
        }

        // Verifica se o nome
        if (nomeGrupo.equals("")) {
            JOptionPane.showMessageDialog(this, "O nome para o grupo não pode ser vazio.", "Erro", JOptionPane.ERROR_MESSAGE);
            return; // Retorna se o nome do grupo estiver vazio
        }

        // Envia a mensagem através do peer
        selectedPeers.add(peer.getIdPeer()); // Adicionamos o proprio ID para fazer parte do grupo
        peer.enviarMensagemGrupo(nomeGrupo, String.join("|", selectedPeers));
        textFieldGroup.setText("Nome do Grupo"); // Limpa o campo de texto após enviar
        // Atualiza a lista de conversas
        atualizarListaConversas();
    }

    // Método para exibir conversas selecionadas
    private void exibirConversasSelecionada() {
        String selecionado = listConversas.getSelectedValue(); // Obtém a conversa selecionada
        if (selecionado != null) {
            exibirMensagens(selecionado); // Exibe as mensagens dessa conversa
        }
    }

    // Método para exibir mensagens de uma conversa
    private void exibirMensagens(String idPeer) {
    List<String> mensagens = peer.getMensagens(idPeer); // Obtém mensagens da conversa
    StringBuilder sb = new StringBuilder(); // Cria um StringBuilder para compor as mensagens
    String meuId = peer.getIdPeer(); // Obtém o ID do próprio peer para verificar autor da mensagem
    
        for (int i = 0; i < mensagens.size(); i += 2) {
        String remetente = mensagens.get(i);
        String conteudo = mensagens.get(i + 1);
        
        if (remetente.equals(meuId)) {
            sb.append(" ".repeat(40)); // Adiciona espaço para alinhamento à direita
            sb.append("Eu: ").append(conteudo).append("\n"); // Exibe a mensagem com rótulo "Eu"
        } else {
            sb.append(remetente).append(": ").append(conteudo).append("\n"); // Alinhamento padrão para mensagens recebidas
        }
    }
    
    textAreaConversa.setText(sb.toString()); // Atualiza a área de texto com as mensagens formatadas
}


    // Método chamado quando uma nova mensagem é recebida
    @Override
    public void onNewMessage(String idDestinatario, String mensagem) {
        // Atualiza a lista de conversas
        atualizarListaConversas();

        // Se a conversa atual for a mesma, atualiza a área de mensagens
        String selecionado = listConversas.getSelectedValue();
        if (selecionado != null && selecionado.equals(idDestinatario)) {
            exibirMensagens(idDestinatario); // Exibe as mensagens da conversa atualizada
        }
    }
}
