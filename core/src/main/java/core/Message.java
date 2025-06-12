
package core;
import java.io.Serializable;

/**
 * Un mensaje de chat: tipo, remitente y texto (o payload en Base64).
 */
public class Message implements Serializable {
    private MessageType type;
    private String user;
    private String payload; // texto o filename|datosBase64

    public Message(MessageType type, String user, String payload) {
        this.type = type;
        this.user = user;
        this.payload = payload;
    }
    public MessageType getType()    { return type; }
    public String      getUser()    { return user; }
    public String      getPayload() { return payload; }
}
