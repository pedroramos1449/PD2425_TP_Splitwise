CREATE TABLE IF NOT EXISTS utilizadores (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        nome TEXT NOT NULL,
                                        email TEXT NOT NULL UNIQUE,
                                        telefone TEXT,
                                        senha TEXT NOT NULL
);

DROP TABLE IF EXISTS grupos;

CREATE TABLE grupos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nome TEXT NOT NULL UNIQUE,
                        id_utilizador_criador INTEGER NOT NULL
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

CREATE TABLE IF NOT EXISTS grupo_utilizadores (
                                                  id_grupo INTEGER NOT NULL,
                                                  id_utilizador INTEGER NOT NULL,
                                                  PRIMARY KEY (id_grupo, id_utilizador),
                                                  FOREIGN KEY (id_grupo) REFERENCES grupos(id),
                                                  FOREIGN KEY (id_utilizador) REFERENCES utilizadores(id)
);