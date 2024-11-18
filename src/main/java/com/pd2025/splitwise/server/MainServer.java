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

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Mensagem do cliente: " + inputLine);

                    // Lista de comandos dependendo do input do usuario no cliente
                    if (inputLine.startsWith("CREATE_GROUP")) {
                        handleCreateGroupCommand(inputLine.substring(13), out);
                    } else if(inputLine.startsWith("REGISTER:")) {
                        handleRegisterCommand(inputLine.substring(9), out);
                    } else if (inputLine.startsWith("LOGIN:")) {
                        handleLoginCommand(inputLine.substring(6), out);
                    } else if (inputLine.startsWith("EDIT:")) {
                        handleEditCommand(inputLine.substring(5), out);
                    } else if ("sair".equalsIgnoreCase(inputLine)) {
                        System.out.println("Cliente desconectado.");
                        break;
                    } else {
                        out.println("Comando desconhecido.");
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

        private void handleRegisterCommand(String data, PrintWriter out) {
            String[] parts = data.split(",");
            if (parts.length == 4) {
                String nome = parts[0];
                String email = parts[1];
                String telefone = parts[2];
                String senha = parts[3];
                boolean success = database.registerUser(nome, email, telefone, senha);
                out.println(success ? "REGISTRATION_SUCCESS" : "REGISTRATION_FAILED");
            } else {
                out.println("Formato Invalido. Use: REGISTER:name,email,phone,password");
            }
        }

        private void handleLoginCommand(String data, PrintWriter out) {
            String[] parts = data.split(",");
            if (parts.length == 2) {
                String email = parts[0];
                String senha = parts[1];
                boolean success = database.authenticateUser(email, senha);
                out.println(success ? "LOGIN_SUCCESS" : "INVALID_CREDENTIALS");
            } else {
                out.println("Formato Invalido. Use: LOGIN:email,password");
            }
        }

        private void handleEditCommand(String data, PrintWriter out) {
            String[] parts = data.split(",");
            if (parts.length == 4) {
                try {
                    int userId = Integer.parseInt(parts[0]);
                    String newNome = parts[1];
                    String newTelefone = parts[2];
                    String newSenha = parts[3];
                    boolean success = database.editUserData(userId, newNome, newTelefone, newSenha);
                    out.println(success ? "EDIT_SUCCESS" : "EDIT_FAILED");
                } catch (NumberFormatException e) {
                    out.println("ID de usuario invalido. Tem que ser uma integer.");
                }
            } else {
                out.println("Formato Invalido. Use: EDIT:id,newName,newPhone,newPassword");
            }
        }

        private void handleCreateGroupCommand(String data, PrintWriter out) {
            String[] parts = data.split(",", 2);
            if (parts.length == 2) {
                try {
                    int userId = Integer.parseInt(parts[0]);
                    String groupName = parts[1];

                    // Check if the group was created successfully
                    boolean success = database.createGroup(groupName, userId);
                    if (success) {
                        out.println("GROUP_CREATION_SUCCESS");
                    } else {
                        out.println("GROUP_CREATION_FAILED: Um grupo com esse nome ja existe.");
                    }
                } catch (NumberFormatException e) {
                    out.println("ID de usuario invalido. Tem que ser uma integer.");
                }
            } else {
                out.println("Formato Invalido. Use: CREATE_GROUP:userId,groupName");
            }
        }


    }
}
