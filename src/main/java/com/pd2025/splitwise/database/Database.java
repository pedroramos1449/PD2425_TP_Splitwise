package com.pd2025.splitwise.database;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Database {
    private Connection connection;
    private String dbFilePath;

    public Database(String dbFilePath) {
        this.dbFilePath = dbFilePath;
        this.connection = connect();
    }

    // Estabelece a conexão com a base de dados
    private Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        } catch (SQLException e) {
            System.err.println("Erro ao conectar ao banco de dados: " + e.getMessage());
            return null;
        }
    }

    // Inicializa a base de dados com o init.sql
    public void initializeDatabase() {
        try {
            if (isDatabaseEmpty()) {
                executeSQLScript("C:\\Users\\André\\Desktop\\PD2425_TP_Splitwise-master\\src\\main\\resources\\init.sql"); // Caminho do init.sql
                System.out.println("Base de dados inicializada com o script init.sql.");
            } else {
                System.out.println("Base de dados já inicializada.");
            }
        } catch (Exception e) {
            System.err.println("Erro ao inicializar a base de dados: " + e.getMessage());
        }
    }

    // Verifica se a base de dados está vazia
    private boolean isDatabaseEmpty() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            return !rs.next();
        } catch (SQLException e) {
            System.err.println("Erro ao verificar a base de dados: " + e.getMessage());
            return true;  // Retorna true em caso de erro, para evitar falhas
        }
    }

    // Executa o script SQL para configurar a base de dados
    private void executeSQLScript(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            StringBuilder sql = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sql.append(line);
                if (line.trim().endsWith(";")) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sql.toString());
                        sql.setLength(0); // Limpa o StringBuilder
                    }
                }
            }
        } catch (IOException | SQLException e) {
            System.err.println("Erro ao executar o script SQL: " + e.getMessage());
        }
    }

    public boolean ValidaTelefone(String telefone)
    {
        return telefone.matches("\\d+");
    }

    public boolean registerUser(String nome, String email, String telefone, String senha) {
        String sql = "INSERT INTO utilizadores (nome, email, telefone, senha) VALUES (?, ?, ?, ?)";
        if (!ValidaTelefone(telefone)) {
            System.out.println("Telefone inválido. Apenas números são permitidos.");
            return false;
        }
        telefone = telefone.trim();
        nome = nome.trim();
        email = email.trim();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.setString(2, email);
            pstmt.setString(3, telefone);
            pstmt.setString(4, senha);
            pstmt.executeUpdate();
            incrementDatabaseVersion();
            System.out.println("Usuario registrado com sucesso: " + email);
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao registrar o usuario: " + e.getMessage());
            return false;
        }
    }

    public int authenticateUser(String email, String senha) {
        String sql = "SELECT id FROM utilizadores WHERE email = ? AND senha = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, senha);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Usuario autenticado com sucesso: " + email);
                    return rs.getInt("id");
                } else {
                    System.out.println("Email ou senha invalido: " + email);
                    return -1;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao autenticar o usuario: " + e.getMessage());
            return -1;
        }
    }

    public boolean editUserData(int userId, String newNome, String newTelefone, String newSenha) {
        String sql = "UPDATE utilizadores SET nome = ?, telefone = ?, senha = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newNome);
            pstmt.setString(2, newTelefone);
            pstmt.setString(3, newSenha);
            pstmt.setInt(4, userId);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                incrementDatabaseVersion();
                System.out.println("Informacao de usuario mudada com sucesso para o ID: " + userId);
                return true;
            } else {
                System.out.println("Nao a usuarios com o ID: " + userId);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro mudando informacoes do usuario: " + e.getMessage());
            return false;
        }
    }

        public boolean createGroup(String groupName, int userId) {
            try {
                // Check se o nome do grupo é unico
                String checkQuery = "SELECT COUNT(*) FROM grupos WHERE nome = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, groupName);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; // Group name already exists
                    }
                }

                // Introduz o novo grupo
                String insertQuery = "INSERT INTO grupos (nome, id_utilizador_criador) VALUES (?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, groupName);
                    insertStmt.setInt(2, userId);
                    insertStmt.executeUpdate();
                }
                int groupID = getGroupID(groupName);
                addMemberToGroup(groupID,userId);

                incrementDatabaseVersion();
                return true;

            } catch (SQLException e) {
                System.err.println("Erro ao criar grupo: " + e.getMessage());
                return false;
            }
        }

    public List<String> listGroupsForUser(int userId) {
        String sql = "SELECT g.nome FROM grupos g JOIN membros m ON g.id = m.id_grupo WHERE m.id_utilizador = ?";
        List<String> groups = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    groups.add(rs.getString("nome"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar grupos: " + e.getMessage());
        }
        return groups;
    }

    public boolean ChangeGroupName(int groupID,String novoNome)
    {
        String sql = "UPDATE grupos SET nome = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setString(1,novoNome);
            ps.setInt(2,groupID);
            int mudancas = ps.executeUpdate();
            return mudancas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createInvite(int groupID, String email, int userId) {
        String getUserID = "SELECT id FROM utilizadores WHERE email = ?";
        String ifAlreadyInvitedSql = "SELECT COUNT(*) FROM pedidos_grupo WHERE id_grupo = ? AND id_utilizador = ? AND status = 'pendente'";
        String InviteSql = "INSERT INTO pedidos_grupo (id_grupo, id_utilizador, id_remetente, status) VALUES (?, ?, ?, 'pendente')";

        try (PreparedStatement idStmt = connection.prepareStatement(getUserID);
             PreparedStatement AInvitedStmt = connection.prepareStatement(ifAlreadyInvitedSql);
             PreparedStatement inviteStmt = connection.prepareStatement(InviteSql)) {

            if (!isMember(groupID, userId)) {
                System.err.println("Erro: Utilizador não pertence ao grupo."); // Debug
                return false;
            }

            idStmt.setString(1, email);
            try (ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    int targetID = rs.getInt("id");

                    if (isMember(groupID, targetID)) {
                        System.err.println("Erro: O utilizador que está a tentar convidar já é membro do grupo.");
                        return false;
                    }

                    AInvitedStmt.setInt(1, groupID);
                    AInvitedStmt.setInt(2, targetID);
                    try (ResultSet invitedRS = AInvitedStmt.executeQuery()) {
                        if (invitedRS.next() && invitedRS.getInt(1) > 0) {
                            System.err.println("Erro: O utilizador já tem um convite para este grupo.");
                            return false;
                        }
                    }

                    inviteStmt.setInt(1, groupID);
                    inviteStmt.setInt(2, targetID);
                    inviteStmt.setInt(3, userId);
                    inviteStmt.executeUpdate();
                    return true;
                } else {
                    System.err.println("Erro: E-mail não encontrado."); // Debug
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao criar convite: " + e.getMessage()); // Debug
        }
        return false;
    }


    public boolean addMemberToGroup(int groupId,int ownerId) {
        String sql = "INSERT INTO membros (id_grupo, id_utilizador) VALUES (?, ?)";

        try (PreparedStatement Stmt = connection.prepareStatement(sql)) {
                    Stmt.setInt(1, groupId);
                    Stmt.setInt(2, ownerId);
                    Stmt.executeUpdate();
                    incrementDatabaseVersion();
                    return true;
                }
        catch (SQLException e) {
            System.err.println("Erro ao adicionar membro: " + e.getMessage());
        }
        return false;
    }

    public List<String> ShowInvites(int userId) {
        String sql = " SELECT p.id AS pedido_id, g.nome AS grupo_nome FROM pedidos_grupo p JOIN grupos g ON p.id_grupo = g.id JOIN utilizadores u ON p.id_utilizador = u.id WHERE p.id_utilizador = ? AND p.status = 'pendente' ";
        List<String> pedidos = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String pedido = " Grupo: " + rs.getString("grupo_nome");
                    pedidos.add(pedido);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar pedidos pendentes: " + e.getMessage());
        }
        return pedidos;
    }


    public boolean removeMemberFromGroup(int groupId, String email, int ownerId) {
        String checkOwnershipSql = "SELECT COUNT(*) FROM grupos WHERE id = ? AND id_utilizador_criador = ?";
        String getUserIdSql = "SELECT id FROM utilizadores WHERE email = ?";
        String deleteMemberSql = "DELETE FROM membros WHERE id_grupo = ? AND id_utilizador = ?";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkOwnershipSql);
             PreparedStatement getUserStmt = connection.prepareStatement(getUserIdSql);
             PreparedStatement deleteStmt = connection.prepareStatement(deleteMemberSql)) {

            checkStmt.setInt(1, groupId);
            checkStmt.setInt(2, ownerId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    return false;
                }
            }

            getUserStmt.setString(1, email);
            try (ResultSet rs = getUserStmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    deleteStmt.setInt(1, groupId);
                    deleteStmt.setInt(2, userId);
                    deleteStmt.executeUpdate();
                    incrementDatabaseVersion();
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao remover membro: " + e.getMessage());
        }
        return false;
    }

    public String getGroupDetails(int groupId, int userId) {
        String sql = "SELECT g.nome, u.nome AS membro FROM grupos g JOIN membros m ON g.id = m.id_grupo JOIN utilizadores u ON m.id_utilizador = u.id WHERE g.id = ? AND EXISTS ( SELECT 1 FROM membros WHERE id_grupo = g.id AND id_utilizador = ?)";

        StringBuilder details = new StringBuilder();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (details.length() == 0) {
                        details.append("Nome do grupo: ").append(rs.getString("nome")).append("\nMembros:\n");
                    }
                    details.append("- ").append(rs.getString("membro")).append("\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter detalhes do grupo: " + e.getMessage());
            return null;
        }

        return details.length() > 0 ? details.toString() : null;
    }

    public int getGroupID(String nome) {
        String sql = "SELECT id FROM grupos WHERE nome = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");  // Retorna o ID do grupo encontrado
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter o ID do grupo: " + e.getMessage());
        }
        return -1;  // Retorna -1 caso o grupo não seja encontrado
    }


    public String getGroupName(int groupId) {
        String sql = "SELECT nome FROM grupos WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nome");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao procurar o grupo: " + e.getMessage());
        }
        return null;
    }

    public int getPendingInviteID(String groupName) {
        String sql = "SELECT p.id FROM pedidos_grupo p JOIN grupos g ON p.id_grupo = g.id WHERE g.nome = ? AND p.status = 'pendente' ";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    incrementDatabaseVersion();
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar o ID do convite pendente: " + e.getMessage());
        }
        return -1;
    }

    public boolean acceptInvite(int inviteId, int userId) {
        String validateInviteSql = "SELECT id FROM pedidos_grupo WHERE id = ? AND id_utilizador = ?";
        String addToGroupSql = "INSERT INTO membros (id_grupo, id_utilizador) " +
                "SELECT id_grupo, id_utilizador FROM pedidos_grupo WHERE id = ?";
        String deleteInviteSql = "DELETE FROM pedidos_grupo WHERE id = ?";

        try (PreparedStatement validateStmt = connection.prepareStatement(validateInviteSql);
             PreparedStatement insertStmt = connection.prepareStatement(addToGroupSql);
             PreparedStatement deleteStmt = connection.prepareStatement(deleteInviteSql)) {

            validateStmt.setInt(1, inviteId);
            validateStmt.setInt(2, userId);
            try (ResultSet rs = validateStmt.executeQuery()) {
                if (!rs.next()) {
                    System.err.println("Erro: Convite inválido ou não pertence ao utilizador.");
                    return false;
                }
            }

            insertStmt.setInt(1, inviteId);
            int rowsInserted = insertStmt.executeUpdate();

            if (rowsInserted > 0) {
                deleteStmt.setInt(1, inviteId);
                deleteStmt.executeUpdate();

                incrementDatabaseVersion(); // Atualizar a versão do banco de dados
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao aceitar o convite: " + e.getMessage());
        }
        return false;
    }

    public boolean denyInvite(int inviteId, int userId) {
        String deleteInviteSql = "DELETE FROM pedidos_grupo WHERE id = ?";

        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteInviteSql)) {

            deleteStmt.setInt(1, inviteId);
            int rowsDeleted = deleteStmt.executeUpdate();

            if (rowsDeleted > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao recusar o convite: " + e.getMessage());
        }
        return false; // Caso contrário, falha ao recusar o convite
    }


    public boolean isMember(int groupId, int userId) {
        String query = "SELECT COUNT(*) FROM membros WHERE id_grupo = ? AND id_utilizador = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar associação ao grupo: " + e.getMessage());
        }
        return false;
    }

    public boolean AddDespesa(int groupID,String data,double valor,String descricao,String[] participantes,int idpagador)
    {
        String checkGroupSql = "SELECT COUNT(*) FROM grupos WHERE id = ?";
        try (PreparedStatement psCheckGroup = connection.prepareStatement(checkGroupSql)) {
            psCheckGroup.setInt(1, groupID);
            ResultSet rs = psCheckGroup.executeQuery();
            if (!rs.next() || rs.getInt(1) == 0) {
                System.err.println("Erro: Grupo não encontrado");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar grupo:" + e.getMessage());
        }

        if(!isMember(groupID,idpagador))
        {
            System.err.println("O utilizador pagador não pertence ao grupo.");
            return false;
        }

        List<Integer> participantesID = new ArrayList<>();

        for(String participante : participantes)
        {
            int idParticipante = getUserId(participante);
            if (idParticipante == -1)
            {
                System.err.println("Utilizador: " + participante + " não existe.");
                return false;
            }
            if (!isMember(groupID,idParticipante))
            {
                System.err.println("O utilizador " + participante + "não é membro do grupo.");
                return false;
            }
            participantesID.add(idParticipante);

        }

        String DespesaSql = "INSERT INTO despesas (id_grupo, id_utilizador_pagador, valor, descricao, data) VALUES (?, ?, ?, ?, ?)";
        try(PreparedStatement PS = connection.prepareStatement(DespesaSql,Statement.RETURN_GENERATED_KEYS))
        {
            PS.setInt(1,groupID);
            PS.setInt(2,idpagador);
            PS.setDouble(3,valor);
            PS.setString(4,descricao);
            PS.setString(5,data);
            int affectedRows = PS.executeUpdate();
            if (affectedRows == 0)
            {
                System.err.println("Erro ao adicionar despesa.");
                return false;
            }

            try (ResultSet generatedKeys = PS.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int despesaId = generatedKeys.getInt(1);
                    String insertParticipanteSql = "INSERT INTO despesas_partilhadas (id_despesa, id_utilizador, valor) VALUES (?, ?, ?)";
                    try (PreparedStatement participanteStmt = connection.prepareStatement(insertParticipanteSql)) {
                        for (int idParticipante : participantesID) {
                            participanteStmt.setInt(1, despesaId);
                            participanteStmt.setInt(2, idParticipante);
                            participanteStmt.setDouble(3, valor / participantesID.size()); // Divide o valor igualmente entre os participantes
                            participanteStmt.addBatch();
                        }
                        participanteStmt.executeBatch();
                    } catch (SQLException e) {
                        System.err.println("Erro ao adicionar participantes: " + e.getMessage());
                        return false;
                    }
                    return true;
                } else {
                    System.err.println("Erro ao obter o ID da despesa.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar despesa: " + e.getMessage());
            return false;
        }
    }


    public int getUserId(String email) {
        String sql = "SELECT id FROM utilizadores WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public double TotalDespesas(int groupId) {
        String sql = "SELECT SUM(valor) FROM despesas WHERE id_grupo = ?";
        double total = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getDouble(1);  // A soma das despesas será retornada aqui
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao somar as despesas: " + e.getMessage());
        }

        return total;
    }



    /// DB Versioning

    // Retorna a versão da base de dados
    public int getDatabaseVersion() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT versao FROM versao_bd ORDER BY versao DESC LIMIT 1")) {
            return rs.next() ? rs.getInt("versao") : 0;
        } catch (SQLException e) {
            System.err.println("Erro ao obter a versão da base de dados: " + e.getMessage());
            return 0;
        }
    }

    public void incrementDatabaseVersion() {
        String sql = "INSERT INTO versao_bd (versao) VALUES (?)";
        int currentVersion = getDatabaseVersion();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, currentVersion + 1);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao incrementar versão da base de dados: " + e.getMessage());
        }
    }





    // Métodos adicionais para operações de CRUD
}
