package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EmojiPanel extends JDialog {
    private JTextField messageInput; // Área de entrada de mensagens

    public EmojiPanel(JTextField messageInput) {
        this.messageInput = messageInput;

        setTitle("Select Emoji");
        setSize(300, 200);
        setLayout(new GridLayout(4, 4)); // 4 linhas, 4 colunas

        // Emojis para escolher
        String[] emojis = {"😀", "😂", "😍", "😎", "😢", "😡", "👍", "🎉",
                           "🥳", "🤔", "😱", "🙌", "🤷‍♂️", "🙈", "✨", "💔"};

        for (String emoji : emojis) {
            JButton button = new JButton(emoji);
            button.addActionListener(new EmojiButtonListener(emoji));
            add(button);
        }
    }

    private class EmojiButtonListener implements ActionListener {
        private String emoji;

        public EmojiButtonListener(String emoji) {
            this.emoji = emoji;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            messageInput.setText(messageInput.getText() + emoji); // Adiciona o emoji à entrada de mensagens
            dispose(); // Fecha o painel de emojis
        }
    }
}
