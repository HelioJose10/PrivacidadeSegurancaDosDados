package com.example;

import java.io.*;
import java.net.*;
import javax.crypto.SecretKey;
import java.util.concurrent.BlockingQueue;

public class PeerClientHandler implements Runnable {
    private Socket socket;
    private SecretKey key;
    private BlockingQueue<Message> messageQueue;

    public PeerClientHandler(Socket socket, SecretKey key, BlockingQueue<Message> messageQueue) {
        this.socket = socket;
        this.key = key; 
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Message message = (Message) inputStream.readObject();
                String decryptedMessage = CryptoUtils.decrypt(message.getContent(), key);
                
                // Adiciona a mensagem à fila de mensagens
                messageQueue.offer(new Message(message.getSender(), decryptedMessage.getBytes())); // Armazenar como byte[]
                
                // Exibir mensagem recebida
                System.out.println("Received: " + decryptedMessage);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
