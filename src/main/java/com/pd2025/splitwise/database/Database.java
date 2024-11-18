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
            System.out.println("User registered successfully: " + email);
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
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
                    System.out.println("User authenticated successfully: " + email);
                    return true;
                } else {
                    System.out.println("Invalid email or password: " + email);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
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
                System.out.println("User data updated successfully for ID: " + userId);
                return true;
            } else {
                System.out.println("No user found with ID: " + userId);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user data: " + e.getMessage());
            return false;
        }
    }

    // Métodos adicionais para operações de CRUD
}