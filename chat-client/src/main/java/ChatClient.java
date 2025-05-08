import command.Command;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChatClient extends Application {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String apelido;

    private final ListView<String> userList = new ListView<>();
    private final TabPane chatTabs = new TabPane();

    private VBox chatVBox = new VBox();
    private ScrollPane scrollPane;
    private TextArea publicChatArea;

    @Override
    public void start(Stage primaryStage) throws Exception {
        apelido = mostrarDialogApelido();

        socket = new Socket("localhost", 8089);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(Command.LOGIN.getPrefix() + apelido);

        BorderPane root = new BorderPane();

        VBox leftPane = new VBox(new Label("Usuários Online"), userList);
        leftPane.setPadding(new Insets(10));
        leftPane.setSpacing(5);
        leftPane.setPrefWidth(150);

        root.setLeft(leftPane);
        root.setCenter(chatTabs);

        publicChatArea = new TextArea();
        publicChatArea.setEditable(false);

        Tab publicTab = getPublicTab();
        chatTabs.getTabs().add(publicTab);

        userList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String selectedUser = userList.getSelectionModel().getSelectedItem();
                if (selectedUser != null && !selectedUser.equals(apelido)) {
                    abreChatPrivado(selectedUser, true);
                }
            }
        });

        new Thread(this::listen).start();

        primaryStage.setOnCloseRequest(event -> encerrarConexao());

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Chat - " + apelido);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab getPublicTab() {
        TextField publicInput = new TextField();
        publicInput.setPromptText("Mensagem pública...");
        publicInput.setOnAction(e -> {
            String msg = publicInput.getText();
            if (!msg.trim().isBlank()) {
                out.println(Command.MSG.getPrefix() + msg);
                publicInput.clear();
            }
        });

        VBox publicBox = new VBox(publicChatArea, publicInput);
        Tab publicTab = new Tab("Geral", publicBox);
        publicTab.setClosable(false);
        return publicTab;
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Recebendo: " + line);
                Command cmd = Command.fromMessage(line);
                if (cmd == null) continue;
                String payload = line.substring(cmd.getPrefix().length());

                switch (cmd) {
                    case USUARIOS:
                        String[] users = payload.split(",");
                        List<String> filtered = new ArrayList<>();
                        for (String u : users) if (!u.equals(apelido)) filtered.add(u);
                        Platform.runLater(() -> userList.getItems().setAll(filtered));
                        break;
                    case PRIVADO:
                        String[] parts = payload.split(":", 2);
                        String remetente = parts[0];
                        String mensagemPriv = parts[1];
                        Platform.runLater(() -> handlePrivado(remetente, mensagemPriv));
                        break;
                    case MSG:
                        Platform.runLater(() -> publicChatArea.appendText(payload + "\n"));
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePrivado(String remetente, String mensagem) {
        boolean encontrada = false;
        for (Tab tab : chatTabs.getTabs()) {
            if (tab.getText().equals(remetente)) {
                exibirMensagem(mensagem, false);
                encontrada = true;
                break;
            }
        }
        if (!encontrada) {
            abreChatPrivado(remetente, false);
            exibirMensagem(mensagem, false);
        }
    }

    private void abreChatPrivado(String destinatario, boolean setarFoco) {
        for (Tab tab : chatTabs.getTabs()) if (tab.getText().equals(destinatario)) return;

        VBox box = new VBox(5);
        scrollPane = new ScrollPane();
        chatVBox = new VBox(5);
        chatVBox.setStyle("-fx-padding: 5px;");
        scrollPane.setContent(chatVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        box.getChildren().add(scrollPane);

        TextField field = new TextField();
        box.getChildren().add(field);
        field.setOnAction(e -> {
            String msg = field.getText();
            if (!msg.trim().isBlank()) {
                out.println(Command.PRIVADO.getPrefix() + destinatario + ":" + msg);
                exibirMensagem(msg, true);
                field.clear();
            }
        });

        Tab tab = new Tab(destinatario, box);
        chatTabs.getTabs().add(tab);
        if (setarFoco) {
            chatTabs.getSelectionModel().select(tab);
            field.requestFocus();
        }
    }

    private void exibirMensagem(String mensagem, boolean isEnviada) {
        HBox hbox = new HBox();
        hbox.setSpacing(10);

        Label mensagemLabel = new Label(mensagem);
        mensagemLabel.setWrapText(true);
        mensagemLabel.setMaxWidth(250);

        hbox.getChildren().add(mensagemLabel);

        if (isEnviada) {
            hbox.setStyle("-fx-alignment: center-right;");
            mensagemLabel.setStyle("-fx-background-color: #ADD8E6; -fx-background-radius: 15px; -fx-padding: 5px 10px;");
        } else {
            hbox.setStyle("-fx-alignment: center-left;");
            mensagemLabel.setStyle("-fx-background-color: white; -fx-background-radius: 15px; -fx-padding: 5px 10px;");
        }

        Platform.runLater(() -> {
            chatVBox.getChildren().add(hbox);
            chatVBox.requestLayout();

            if (scrollPane != null) {
                scrollPane.setVvalue(1);
            }
        });
    }

    private String mostrarDialogApelido() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Apelido");
        dialog.setHeaderText("Escolha seu apelido");
        return dialog.showAndWait().orElse("User" + new Random().nextInt(1000));
    }

    private void encerrarConexao() {
        try {
            if (out != null) out.println(Command.SAIR.getPrefix());
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Conexão encerrada corretamente.");
        } catch (IOException e) {
            System.out.println("Erro ao encerrar a conexão: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
