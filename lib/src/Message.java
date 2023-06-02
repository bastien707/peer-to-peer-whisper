import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public record Message(String type, String sender, String content, VectorClock vectorClock) {

    public String toString() {
        return type + ":" + sender + ":" + content + ":" + vectorClock;
    }

    /**
     * Creates a Message object from a string
     *
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

    /**
     * Sends a Message object to a given port from a socket
     *
     * @param socket          the socket to send the message from
     * @param message         the message to send
     * @param portDestination the port to send the message to
     * @throws IOException    if the socket is not valid
     */
    public static void sendMessageObject(DatagramSocket socket, Message message, int portDestination) throws IOException {
        byte[] buffer = (message.toString()).getBytes();
        InetAddress address = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, portDestination);
        socket.send(packet);
    }

    /**
     * Sends a Message object to all nodes in a HashMap except the sender when the type is NEW_PEER
     * @param socket the socket to send the message from
     * @param message the message to send
     * @param nodes the HashMap of nodes
     * @throws IOException if the socket is not valid
     */
    public static void sendToAllNodes(DatagramSocket socket, Message message, HashMap<String, Integer> nodes) throws IOException {
        for (String name : nodes.keySet()) {
            if (name.equals(message.sender()) && message.type().equals("NEW_PEER")) continue;
            byte[] buffer = (message.toString()).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), nodes.get(name));
            socket.send(packet);
        }
    }
}
