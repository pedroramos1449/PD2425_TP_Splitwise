package com.pd2025.splitwise.database;

import com.pd2025.splitwise.server.MainServer;
import com.pd2025.splitwise.util.Constants;

public class Main {
    public static void main(String[] args) {
        // Inicializa o banco de dados
        Database dbManager = new Database(Constants.DATABASE_PATH);
        dbManager.initializeDatabase(); // Executa init.sql se necessário

        // Inicia o servidor
        System.out.println("Iniciando o servidor...");
        MainServer.main(args);  // Inicia o servidor principal para escutar conexões de clientes
    }
}