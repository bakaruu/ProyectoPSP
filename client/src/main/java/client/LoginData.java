// src/main/java/client/LoginData.java
package client;

import javax.swing.*;

/**
 * Contiene los datos de login: nick, avatar, IP y puerto.
 */
public class LoginData {
    private final String username;
    private final ImageIcon avatar;
    private final String serverIp;
    private final int serverPort;

    public LoginData(String username, ImageIcon avatar, String serverIp, int serverPort) {
        this.username   = username;
        this.avatar     = avatar;
        this.serverIp   = serverIp;
        this.serverPort = serverPort;
    }

    public String getUsername() {
        return username;
    }

    public ImageIcon getAvatar() {
        return avatar;
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }
}
