CREATE TABLE IF NOT EXISTS test (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    message TEXT NOT NULL
);

INSERT INTO test (message) VALUES ('Hello, World!');