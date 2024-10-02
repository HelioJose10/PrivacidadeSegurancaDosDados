package main.java.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import main.java.security.EncryptionUtils;

public class client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String secretKey = "1234567890123456";  // Chave de 128 bits


    public client() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Thread para receber mensagens
            new Thread(new IncomingMessagesHandler()).start();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Digite a mensagem: ");
                String message = scanner.nextLine();

                // Criptografar a mensagem antes de enviar
                String encryptedMessage = EncryptionUtils.encrypt(message, secretKey);
                out.println(encryptedMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class IncomingMessagesHandler implements Runnable {
        @Override
        public void run() {
            String encryptedMessage;
            try {
                while ((encryptedMessage = in.readLine()) != null) {
                    // Descriptografar a mensagem recebida
                    String decryptedMessage = EncryptionUtils.decrypt(encryptedMessage, secretKey);
                    System.out.println("Mensagem recebida: " + decryptedMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new client();
    }
}
