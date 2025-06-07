package client;

/**
 * Datos del usuario para iniciar sesión/chat:
 * nombre y servidor (IP+puerto).
 */
public class LoginData {
    private String username;
    private String serverIp;
    private int serverPort;

    public LoginData(String username, String serverIp, int serverPort) {
        this.username = username;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public String getUsername() {
        return username;
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }
}
