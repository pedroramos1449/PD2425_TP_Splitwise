package com.pd2025.splitwise.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
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
                executeSQLScript("...\\src\\main\\resources\\init.sql"); // Caminho do init.sql
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

    public boolean registerUser(String nome, String email, String telefone, String senha) {
        String sql = "INSERT INTO utilizadores (nome, email, telefone, senha) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.setString(2, email);
            pstmt.setString(3, telefone);
            pstmt.setString(4, senha);
            pstmt.executeUpdate();
            System.out.println("Usuario registrado com sucesso: " + email);
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao registrar o usuario: " + e.getMessage());
            return false;
        }
    }

    public boolean authenticateUser(String email, String senha) {
        String sql = "SELECT id FROM utilizadores WHERE email = ? AND senha = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, senha);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Usuario autenticado com sucesso: " + email);
                    return true;
                } else {
                    System.out.println("Email ou senha invalido: " + email);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao autenticar o usuario: " + e.getMessage());
            return false;
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
        if (connection == null) {
            System.err.println("Conexão com o banco de dados não foi estabelecida.");
            return false;
        }

        try {
            connection.setAutoCommit(false); // Start transaction

            // Normalize input
            groupName = groupName.trim();

            // Check if the group name is unique (case-insensitive)
            String checkQuery = "SELECT COUNT(*) FROM grupos WHERE LOWER(nome) = LOWER(?)";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, groupName);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Group name already exists: " + groupName);
                    return false; // Group name already exists
                }
            }

            // Insert the new group
            String insertGroupQuery = "INSERT INTO grupos (nome, id_utilizador_criador) VALUES (?, ?)";
            try (PreparedStatement insertGroupStmt = connection.prepareStatement(insertGroupQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertGroupStmt.setString(1, groupName);
                insertGroupStmt.setInt(2, userId);
                insertGroupStmt.executeUpdate();

                // Get the generated group ID
                ResultSet generatedKeys = insertGroupStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int groupId = generatedKeys.getInt(1);

                    // Add the creator to the group
                    String insertMembershipQuery = "INSERT INTO grupo_utilizadores (id_grupo, id_utilizador) VALUES (?, ?)";
                    try (PreparedStatement insertMembershipStmt = connection.prepareStatement(insertMembershipQuery)) {
                        insertMembershipStmt.setInt(1, groupId);
                        insertMembershipStmt.setInt(2, userId);
                        insertMembershipStmt.executeUpdate();
                    }
                }
            }

            connection.commit(); // Commit transaction
            System.out.println("Group created successfully: " + groupName);
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback in case of an error
            } catch (SQLException rollbackEx) {
                System.err.println("Erro ao reverter transação: " + rollbackEx.getMessage());
            }
            System.err.println("Erro ao criar grupo: " + e.getMessage());
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Reset auto-commit
            } catch (SQLException autoCommitEx) {
                System.err.println("Erro ao redefinir auto-commit: " + autoCommitEx.getMessage());
            }
        }
    }

    public boolean isUserInGroup(int groupId, int userId) {
        if (connection == null) {
            System.err.println("Conexão com o banco de dados não foi estabelecida.");
            return false;
        }

        String query = "SELECT COUNT(*) FROM grupo_utilizadores WHERE id_grupo = ? AND id_utilizador = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.err.println("Erro ao verificar se o utilizador pertence ao grupo: " + e.getMessage());
            return false;
        }
    }

    public int authenticateUserAndGetId(String email, String senha) {
        String sql = "SELECT id FROM utilizadores WHERE email = ? AND senha = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, senha);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Usuario autenticado com sucesso: " + email);
                    return rs.getInt("id"); // Return the user's ID
                } else {
                    System.out.println("Email ou senha invalido: " + email);
                    return -1; // Return -1 to indicate failure
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao autenticar o usuario: " + e.getMessage());
            return -1; // Return -1 in case of an error
        }
    }
    // Métodos adicionais para operações de CRUD
}