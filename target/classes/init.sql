CREATE TABLE IF NOT EXISTS utilizadores (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        nome TEXT NOT NULL,
                                        email TEXT NOT NULL UNIQUE,
                                        telefone TEXT,
                                        senha TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS grupos (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      nome TEXT NOT NULL UNIQUE,
                                      id_utilizador_criador INTEGER NOT NULL,
                                      FOREIGN KEY (id_utilizador_criador) REFERENCES utilizadores(id)
    );

CREATE TABLE IF NOT EXISTS despesas (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        id_grupo INTEGER NOT NULL,
                                        id_utilizador_pagador INTEGER NOT NULL,
                                        valor REAL NOT NULL,
                                        descricao TEXT,
                                        data TEXT NOT NULL,
                                        FOREIGN KEY (id_grupo) REFERENCES grupos(id),
    FOREIGN KEY (id_utilizador_pagador) REFERENCES utilizadores(id)
    );

CREATE TABLE IF NOT EXISTS versao_bd (
                                         versao INTEGER PRIMARY KEY
);