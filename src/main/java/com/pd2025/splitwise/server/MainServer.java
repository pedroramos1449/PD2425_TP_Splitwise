package com.pd2025.splitwise.server;

import com.pd2025.splitwise.database.Database;
import com.pd2025.splitwise.util.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.sql.PreparedStatement;
import java.util.List;

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
        private int authenticatedUserID = -1;
        private int SelectedGroupID = -1;
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
                    String comando = inputLine.split(":")[0].trim().toLowerCase();
                    if (authenticatedUserID == -1) {
                        switch (comando) {
                            case "register":
                                handleRegisterCommand(inputLine.substring(inputLine.indexOf(":") + 1), out);
                                break;

                            case "login":
                                handleLoginCommand(inputLine.substring(inputLine.indexOf(":") + 1), out);
                                break;
                            default:
                                out.println("Comando desconhecido");
                                break;
                        }
                    } else {
                        {
                            switch (comando) {
                                case "logout":
                                    authenticatedUserID = -1;
                                    SelectedGroupID = -1;
                                    out.println("Cliente Desconectado");
                                    break;

                                case "edit":
                                    handleEditCommand(inputLine.substring(inputLine.indexOf(":") + 1),out);
                                    break;
                                case "create_group":
                                    handleCreateGroupCommand(inputLine.substring(inputLine.indexOf(":") + 1),out);
                                    break;
                                case "add_member":
                                    handleAddMemberCommand(inputLine.substring(inputLine.indexOf(":") + 1),out);
                                    break;
                                case "remove_member":
                                    handleRemoveMemberCommand(inputLine.substring(inputLine.indexOf(":") + 1),out);
                                    break;
                                case "view_group":
                                    if (inputLine.contains(":"))
                                    {
                                        String groupName = inputLine.substring(inputLine.indexOf(":")+1);
                                        handleViewGroupCommand(groupName,out);
                                    }
                                    else
                                    {
                                        if (SelectedGroupID != -1)
                                        {
                                            handleViewGroupCommand("",out);
                                        }
                                        else
                                        {
                                            out.println("Selecione um grupo válido");
                                        }
                                    }
                                    break;
                                case "list_group":
                                    handleListGroupsCommand(out);
                                    break;
                                case "select_group":
                                    handleSelecionaGrupo(inputLine.substring(inputLine.indexOf(":") + 1),out);
                                    break;
                                case "show_invite":
                                    handleShowInvites(out);
                                    break;
                                case "answer_invite":
                                    handleAnswerRequestCommand(inputLine.substring(inputLine.indexOf(":") + 1),out);
                                    break;
                                case "change_group_name":
                                    handleChangeGroupName(inputLine.substring(inputLine.indexOf(":")+1),out);
                                    break;
                                default:
                                    out.println("Comando Desconhecido");
                            }
                        }
                    }
                    out.println("END");
                }
            }
            catch (IOException e) {
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
                authenticatedUserID = database.authenticateUser(email, senha);
                out.println(authenticatedUserID != -1 ? "LOGIN_SUCCESS" : "INVALID_CREDENTIALS");
            } else {
                out.println("Formato Invalido. Use: LOGIN:email,password");
            }
        }

        private void handleSelecionaGrupo(String data,PrintWriter out)
        {
            int GroupID = database.getGroupID(data);
            if ((GroupID != -1 && database.isUserMemberOfGroup(GroupID, authenticatedUserID)))
            {
                SelectedGroupID = GroupID;
                out.println("Grupo Selecionado com sucesso.");
            }
            else
            {
                out.println("Grupo não encontrado");
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
                try {
                    int userId = authenticatedUserID;
                    String groupName = data;

                    // Check if the group was created successfully
                    boolean success = database.createGroup(groupName, userId);
                    if (success) {
                        out.println("GROUP_CREATION_SUCCESS");
                    } else {
                        out.println("GROUP_CREATION_FAILED: Um grupo com esse nome ja existe.");
                    }
                } catch (NumberFormatException e) {
                    out.println("ID de usuario invalido. Tem que ser um número inteiro.");
                }
            }

        private void handleListGroupsCommand(PrintWriter out) {
            List<String> groups = database.listGroupsForUser(authenticatedUserID);
            if (groups.isEmpty()) {
                out.println("Você não está em nenhum grupo.");
            } else {
                StringBuilder response = new StringBuilder("Seus grupos:");
                for (String group : groups) {
                    response.append(" Grupo: ").append(group);
                }
                out.println(response);
            }
        }

        private void handleShowInvites(PrintWriter out)
        {
            List<String> convitesPendentes = database.ShowInvites(authenticatedUserID);
            if(convitesPendentes.isEmpty())
            {
                out.println("Não tem nenhum convite");
            }
            else
            {
                StringBuilder response = new StringBuilder("Convites pendentes:");
                for(String convite:convitesPendentes)
                {
                    response.append(convite);
                }
                out.println(response);
            }
        }

        private void handleAddMemberCommand(String data, PrintWriter out) {
            String[] parts = data.split(",", 2);
            if (parts.length == 2) {
                int groupId = database.getGroupID(parts[0]);
                String email = parts[1];
                boolean success = database.createInvite(groupId, email, authenticatedUserID);
                out.println(success ? "Convite enviado." : "Falha ao enviar convite.");
            } else if (parts.length == 1 && SelectedGroupID != -1)
            {
                String email = parts[0];
                boolean success = database.createInvite(SelectedGroupID, email, authenticatedUserID);
                out.println(success ? "Convite enviado." : "Falha ao enviar convite.");
            }
            else
            {
                out.println("Formato inválido. Use: ADD_MEMBER:groupName,email");
            }
        }

        private void handleRemoveMemberCommand(String data, PrintWriter out) {
            String[] parts = data.split(",", 2);
            if (parts.length == 2) {
                int groupId = database.getGroupID(parts[0]);
                String email = parts[1];
                boolean success = database.removeMemberFromGroup(groupId, email, authenticatedUserID);
                out.println(success ? "Membro removido com sucesso." : "Falha ao remover membro.");
            } else if(parts.length == 1 && SelectedGroupID != -1)
            {
                String email = parts[0];
                boolean success = database.removeMemberFromGroup(SelectedGroupID, email, authenticatedUserID);
                out.println(success ? "Membro removido com sucesso." : "Falha ao remover membro.");
            }
            else {
                out.println("Formato inválido. Use: REMOVE_MEMBER:groupId,email");
            }
        }

        private void handleAnswerRequestCommand(String data, PrintWriter out)
        {
            String [] parts = data.split(",",2);
            if(parts.length == 2 || (parts.length == 1 && SelectedGroupID != -1)) {
                String groupName;
                String resposta;
                if (parts.length == 2) {
                    groupName = parts[0].trim();
                    resposta = parts[1].trim().toLowerCase();
                } else {
                    groupName = database.getGroupName(SelectedGroupID);
                    resposta = parts[0].trim().toLowerCase();
                }
                if (!resposta.equals("accept") && !resposta.equals("deny")) {
                    out.println("Ação inválida. Use 'accept' ou 'deny'.");
                    return;
                }
                int inviteID = database.getPendingInviteID(groupName);
                if (inviteID == -1) {
                    out.println("Não tem pedidos deste grupo/Grupo não existe.");
                }
                boolean success = false;
                if (resposta.equals("accept")) {
                    success = database.acceptInvite(inviteID, authenticatedUserID);
                } else {
                    success = database.denyInvite(inviteID, authenticatedUserID);
                }
                if (success)
                {
                        out.println("Pedido atualizado com sucesso");
                    }
                    else
                    {
                        out.println("Falha ao responder ao pedido");
                    }
            }
            else
            {
                out.println("Formato Inválido");
            }
        }

        private void handleViewGroupCommand(String data, PrintWriter out) {
            int groupID;
            if (data.isEmpty() && SelectedGroupID != -1)
            {
                groupID = SelectedGroupID;
            }
            else {
                groupID = database.getGroupID(data);
            }
            String details = database.getGroupDetails(groupID, authenticatedUserID);
            if (details == null) {
                out.println("Grupo não encontrado ou você não tem acesso.");
            } else {
                out.println("Detalhes do grupo:");
                Resposta(details,out);
            }
        }

        private void handleChangeGroupName(String data, PrintWriter out)
        {
            String[] parts = data.split(",");
            if(parts.length == 2)
            {
                String groupName = parts[0];
                int groupID = database.getGroupID(groupName);
                String newName = parts[1];
                boolean success = database.ChangeGroupName(groupID,newName);
                out.println(success ? "Nome do grupo mudado com sucesso." : "Falha ao mudar nome do grupo.");
            }else
                if (parts.length == 1 && SelectedGroupID != 1)
                {
                    String newName = parts[0];
                    boolean success = database.ChangeGroupName(SelectedGroupID,newName);
                    out.println(success ? "Nome do grupo mudado com sucesso." : "Falha ao mudar nome do grupo.");
                }
                else
                {
                    out.println("Formato inválido!Use: change_group_name:nomeGrupo,novoNome");
                }

        }
        

        private void Resposta(String resposta, PrintWriter out) {
            for (String line : resposta.split("\n")) {
                out.println(line);
            }
        }


    }
}

