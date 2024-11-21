package com.pd2025.splitwise.client;

import com.pd2025.splitwise.util.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", Constants.SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado ao servidor em 127.0.0.1:" + Constants.SERVER_PORT);
            System.out.println("Digite 'sair' para encerrar.");

            String userMessage;
            while (true) {
                System.out.print("Digite uma mensagem para o servidor: ");
                userMessage = userInput.readLine();

                if ("sair".equalsIgnoreCase(userMessage)) {
                    out.println(userMessage);
                    System.out.println("Desconectando...");
                    break;
                }

                out.println(userMessage); // Envia a mensagem ao servidor
                String resposta; // Lê a resposta do servidor
                while((resposta = in.readLine()) != null) {
                    if ("END".equals(resposta))
                    {
                        break;
                    }
                    System.out.println("Resposta do servidor: " + resposta);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro de conexão: " + e.getMessage());
        }
    }
}
