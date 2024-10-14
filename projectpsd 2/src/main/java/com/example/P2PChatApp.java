package com.example;

public class P2PChatApp {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -cp <jar-file> com.example.P2PChatApp <port>");
            return;
        }
        
        int port = Integer.parseInt(args[0]);
        System.out.println("P2P Chat Application Starting on port: " + port); // Log de inicialização
        
        new UserInterface(); // Inicializa a interface do usuário
        
        System.out.println("P2P Chat Application Started on port: " + port); // Log de confirmação de inicialização
    }
}
