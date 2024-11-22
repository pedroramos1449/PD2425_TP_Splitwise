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

CREATE TABLE IF NOT EXISTS despesas_partilhadas (
                                         id_despesa INTEGER NOT NULL,
                                         id_utilizador INTEGER NOT NULL,
                                         valor REAL NOT NULL,
                                         PRIMARY KEY (id_despesa, id_utilizador),
                                         FOREIGN KEY (id_despesa) REFERENCES despesas(id),
                                         FOREIGN KEY (id_utilizador) REFERENCES utilizadores(id)
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

CREATE TABLE IF NOT EXISTS membros (
                                       id_grupo INTEGER NOT NULL,
                                       id_utilizador INTEGER NOT NULL,
                                       PRIMARY KEY (id_grupo, id_utilizador),
                                       FOREIGN KEY (id_grupo) REFERENCES grupos(id),
                                       FOREIGN KEY (id_utilizador) REFERENCES utilizadores(id)
);

CREATE TABLE IF NOT EXISTS pedidos_grupo (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                                             id_grupo INTEGER NOT NULL,
                                             id_utilizador INTEGER NOT NULL,
                                             id_remetente INTEGER NOT NULL,
                                             status TEXT NOT NULL DEFAULT 'pendente',
                                             FOREIGN KEY (id_grupo) REFERENCES grupos (id),
                                             FOREIGN KEY (id_utilizador) REFERENCES utilizadores (id)
);

CREATE TABLE IF NOT EXISTS versao_bd (
                                         versao INTEGER PRIMARY KEY
);
