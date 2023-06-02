public class Message {
    private final String type;
    private final String sender;
    private final String content;
    private final VectorClock vectorClock;

    public Message(String type, String sender, String content, VectorClock vectorClock) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.vectorClock = vectorClock;
    }

    public String getContent() {
        return content;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public String getSender() {
        return sender;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return type + ":" + sender + ":" + content + ":" + vectorClock;
    }

    /**
     * Creates a Message object from a string
     * @param message the string to parse
     * @return the Message object
     */
    public static Message fromString(String message) {
        String[] parts = message.split(":");
        String type = parts[0];
        String sender = parts[1];
        String content = parts[2];
        if (parts[3].equals("null")) return new Message(type, sender, content, null);
        VectorClock vectorClock = VectorClock.fromString(parts[3]);
        return new Message(type, sender, content, vectorClock);
    }

    public static void main(String[] args) {
        Message message = new Message("test", "test", "test", null);
        System.out.println(message);
        Message message1 = Message.fromString(message.toString());
        System.out.println(message1);
        String test = "SERVER:bob:salut comment tu vas ?:null";
        Message m2 = Message.fromString(test);
        System.out.println(m2.vectorClock);
    }
}
