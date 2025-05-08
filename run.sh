#!/bin/bash

# 1. Build do projeto completo
./mvnw clean install

# 2. Rodar o servidor em segundo plano
echo "Iniciando o servidor..."
java -jar chat-server/target/chat-server-fat.jar &
SERVER_PID=$!
echo "Servidor rodando (PID=$SERVER_PID)"

# 3. Aguardar 1 segundo para garantir que o servidor suba
sleep 3

# 4. Rodar o cliente
echo "Iniciando o cliente..."
./mvnw javafx:run -f chat-client/pom.xml

# 5. Após fechar o cliente, encerrar o servidor
echo "Encerrando o servidor..."
kill $SERVER_PID
