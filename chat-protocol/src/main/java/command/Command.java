package command;

public enum Command {
    LOGIN("LOGIN:"),
    MSG("MSG:"),
    PRIVADO("PRIVADO:"),
    USUARIOS("USUARIOS:"),
    ERRO("ERRO:"),
    OK("OK"),
    SAIR("SAIR");

    private final String prefix;

    Command(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public static Command fromMessage(String message) {
        for (Command cmd : values()) {
            if (message.startsWith(cmd.prefix)) {
                return cmd;
            }
        }
        return null;
    }
}