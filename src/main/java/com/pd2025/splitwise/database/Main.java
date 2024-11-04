package com.pd2025.splitwise.database;

public class Main {
    public static void main(String[] args) {
        database dbManager = new database();
        dbManager.loadSqlScript(); // Call the method to load and execute the SQL script
    }
}