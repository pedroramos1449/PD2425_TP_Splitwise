package com.pd2025.splitwise.server;

import com.pd2025.splitwise.database.Database;
import com.pd2025.splitwise.util.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class MainServer {
    private static Database database;  // Instância da classe Database para gerenciar o banco de dados

    public static void main(String[] args) {
        // Inicializa o banco de dados usando a classe Database
        database = new Database(Constants.DATABASE_PATH);
        database.initializeDatabase();  // Método que executa o init.sql se necessário

        // Inicia o servidor
        try (ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT)) {
            System.out.println("Servidor principal iniciado e escutando na porta " + Constants.SERVER_PORT);

            // Inicia a thread de heartbeat
            new Thread(new HeartbeatTask()).start();

            // Loop para aceitar conexões de clientes
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                // Cria uma nova thread para o cliente
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    // Classe interna para tratar o heartbeat
    static class HeartbeatTask implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress group = InetAddress.getByName(Constants.HEARTBEAT_ADDRESS);
                MulticastSocket multicastSocket = new MulticastSocket();
                int versao = database.getDatabaseVersion(); // Obter a versão da base de dados

                while (true) {
                    String message = "HEARTBEAT: Versao=" + versao + " PORT=" + Constants.SERVER_PORT;
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, Constants.HEARTBEAT_PORT);
                    multicastSocket.send(packet);
                    System.out.println("Heartbeat enviado: " + message);

                    Thread.sleep(10000); // Heartbeat a cada 10 segundos
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Erro no heartbeat: " + e.getMessage());
            }
        }
    }

    // Classe interna para lidar com clientes
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Mensagem do cliente: " + inputLine);
                    out.println("Servidor: Mensagem recebida - " + inputLine);

                    if ("sair".equalsIgnoreCase(inputLine)) {
                        System.out.println("Cliente desconectado.");
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro de comunicação com o cliente: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Erro ao fechar o socket do cliente: " + e.getMessage());
                }
            }
        }
    }
}
