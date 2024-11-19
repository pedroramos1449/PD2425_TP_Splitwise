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
                System.out.println("\nOpções:");
                System.out.println("1. Enviar mensagem para o servidor");
                System.out.println("2. Selecionar grupo");
                System.out.println("Digite 'sair' para encerrar.");
                System.out.print("Escolha uma opção: ");
                String choice = userInput.readLine();

                if ("sair".equalsIgnoreCase(choice)) {
                    out.println("sair");
                    System.out.println("Desconectando...");
                    break;
                }

                switch (choice) {
                    case "1":
                        System.out.print("Digite uma mensagem para o servidor: ");
                        userMessage = userInput.readLine();
                        out.println(userMessage);
                        String response = in.readLine();
                        System.out.println("Resposta do servidor: " + response);
                        break;

                    case "2":
                        System.out.print("Digite o ID do grupo que deseja selecionar: ");
                        int groupId;
                        try {
                            groupId = Integer.parseInt(userInput.readLine());
                            selectGroup(out, in, groupId);
                        } catch (NumberFormatException e) {
                            System.out.println("ID do grupo inválido. Por favor, tente novamente.");
                        }
                        break;

                    default:
                        System.out.println("Opção inválida. Por favor, escolha 1 ou 2.");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro de conexão: " + e.getMessage());
        }
    }

    private static void selectGroup(PrintWriter out, BufferedReader in, int groupId) {
        try {
            out.println("SET_GROUP " + groupId);
            String response = in.readLine();
            System.out.println("Resposta do servidor: " + response);
        } catch (IOException e) {
            System.err.println("Erro ao selecionar grupo: " + e.getMessage());
        }
    }
}