## Pré Requisitos
- Ter o Java 21 instalado e configurado na máquina.
- Ter o Maven com versão mínima igual a 3.9.9 instalado e configurado. 

### 1. Build do projeto completo
```
./mvnw clean install
```

### 2. Rodar o servidor
```
echo "Iniciando o servidor..."
java -jar chat-server/target/chat-server-fat.jar &
SERVER_PID=$!
echo "Servidor rodando (PID=$SERVER_PID)"
```

### 3. Rodar o cliente
```
echo "Iniciando o cliente..."
./mvnw javafx:run -f chat-client/pom.xml
```
