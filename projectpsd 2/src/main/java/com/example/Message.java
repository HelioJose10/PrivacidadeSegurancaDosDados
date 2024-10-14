package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException; 

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;

public class Message implements Serializable {
    private String sender;
    private byte[] content; // Conteúdo da mensagem em bytes
    private String fileName; // Nome do arquivo, se aplicável

    // Construtor para mensagens de texto
    public Message(String sender, byte[] content) {
        this.sender = sender;
        this.content = content; 
        this.fileName = null; // Inicialmente, sem arquivo
    }

    // Construtor para mensagens de arquivo
    public Message(String sender, File file) {
        this.sender = sender;
        this.fileName = file.getName(); // Armazena o nome do arquivo
        this.content = new byte[(int) file.length()]; // Inicializa o conteúdo
        try {
            FileInputStream fis = new FileInputStream(file);
            fis.read(this.content); // Lê o arquivo
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSender() {
        return sender;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileName() {
        return fileName; // Retorna o nome do arquivo, se houver
    }

    public void saveFile(String directory) {
        try (FileOutputStream fos = new FileOutputStream(directory + File.separator + fileName)) {
            fos.write(content); // Escreve o conteúdo do arquivo
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Converte a mensagem para JSON
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Converte JSON para uma mensagem
    public static Message fromJson(String json) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(json, Message.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
