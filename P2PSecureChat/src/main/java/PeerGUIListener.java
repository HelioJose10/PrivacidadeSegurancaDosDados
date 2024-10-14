// Definição do pacote principal onde a interface PeerGUIListener está localizada
package main.java;

/**
 * Interface PeerGUIListener.
 * Esta interface é utilizada para notificar mudanças no estado de mensagens
 * entre a lógica de backend e a interface gráfica do usuário (GUI) de um peer.
 * Classes que implementam esta interface podem ser notificadas quando
 * novas mensagens são recebidas.
 */
public interface PeerGUIListener {
    /**
     * Método chamado quando uma nova mensagem é recebida.
     *
     * @param idDestinatario O ID do destinatário da mensagem.
     * @param mensagem       A mensagem recebida.
     */
    void onNewMessage(String idDestinatario, String mensagem);
}
