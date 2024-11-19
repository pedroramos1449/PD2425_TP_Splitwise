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
                String mail = getUserEmail(userId);
                addMemberToGroup(groupID,mail,userId);

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

    public boolean addMemberToGroup(int groupId, String email, int ownerId) {
        String checkOwnershipSql = "SELECT COUNT(*) FROM grupos WHERE id = ? AND id_utilizador_criador = ?";
        String getUserIdSql = "SELECT id FROM utilizadores WHERE email = ?";
        String insertMemberSql = "INSERT INTO membros (id_grupo, id_utilizador) VALUES (?, ?)";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkOwnershipSql);
             PreparedStatement getUserStmt = connection.prepareStatement(getUserIdSql);
             PreparedStatement insertStmt = connection.prepareStatement(insertMemberSql)) {

            checkStmt.setInt(1, groupId);
            checkStmt.setInt(2, ownerId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    return false; // Only owners can add members
                }
            }

            getUserStmt.setString(1, email);
            try (ResultSet rs = getUserStmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    insertStmt.setInt(1, groupId);
                    insertStmt.setInt(2, userId);
                    insertStmt.executeUpdate();
                    incrementDatabaseVersion();
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar membro: " + e.getMessage());
        }
        return false;
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
                    return false; // Only owners can remove members
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
                    return rs.getInt("id"); 
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter o ID do grupo: " + e.getMessage());
        }
        return -1;  
    }

    public String getUserEmail(int userId) {
        String sql = "SELECT email FROM utilizadores WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId); 
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar e-mail: " + e.getMessage());
        }
        return null;
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
