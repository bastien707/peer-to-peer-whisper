import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Node {
    private final String name;
    private final DatagramSocket socket;
    private HashMap<String, Integer> nodes;
    private VectorClock vc;
    private int port;

    public Node(String name) throws SocketException {
        this.name = name;
        this.socket = new DatagramSocket();
        this.nodes = new HashMap<>();
        this.port = 0;
        this.vc = new VectorClock(name);
    }

    /**
     * Connect to the network by sending a CONNEXION_REQUEST message to the auth server
     * @throws IOException if the socket is not valid
     */
    public void connect() throws IOException {
        System.out.println("#Connecting to the network...");
        Message newMessage = new Message("CONNEXION_REQUEST", name, null, null);
        Message.sendMessageObject(this.socket, newMessage,5000);
    }

    /**
     * Disconnect from the network
     * @throws IOException if the socket cannot be closed
     */
    public void disconnect() throws IOException {
        System.out.println("#Disconnecting from the network...");
        Message newMessage = new Message("DISCONNECT", name, Utils.hashMapToString(nodes), null);
        Message.sendMessageObject(this.socket, newMessage,5000);
        Message.sendToAllNodes(this.socket, newMessage, nodes);
        this.socket.close();
    }

    /**
     * Send a message to a specific node
     */
    public void startListening() {
        AtomicBoolean shouldCloseSocket = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (!shouldCloseSocket.get()) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message msgObj = Message.fromString(message);
                    switch (msgObj.type()) {
                        case "CONNEXION_ACCEPTED" -> {
                            System.out.println("#You are now connected to the network");
                            updateNodesFromMessage(msgObj.content());
                            Message.sendToAllNodes(this.socket, new Message("NEW_PEER", this.name, Utils.hashMapToString(nodes), vc), nodes);
                        }
                        case "MESSAGE" -> System.out.println(msgObj.sender() + ":" + msgObj.content());
                        case "NEW_PEER" -> {
                            System.out.println("#" + msgObj.sender() + " has joined the network");
                            updateNodesFromMessage(msgObj.content());
                        }
                        case "DISCONNECT" -> {
                            System.out.println("#" + msgObj.sender() + " has left the network");
                            nodes.remove(msgObj.sender());
                            // must close socket if the sender is the one who left
                            if (msgObj.sender().equals(this.name)) {
                                shouldCloseSocket.set(true);
                            }
                        }
                        default -> System.out.println("MESSAGE NOT RECOGNIZED");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (shouldCloseSocket.get()) {
                    socket.close();
                }
            }
        }).start();
    }

    /**
     * Update the nodes HashMap from a received message
     * @param message the message containing the nodes
     */
    public void updateNodesFromMessage(String message) {
        String[] nodes = message.split(",");
        for (String node: nodes) {
            String[] nodeInfo = node.split("=");
            this.nodes.put(nodeInfo[0], Integer.parseInt(nodeInfo[1]));
        }
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                ", socket=" + socket +
                ", nodes=" + nodes +
                ", vc=" + vc +
                " port=" + port +
                '}';
    }

    public static void main(String[] args) throws IOException {
        System.out.println("#Enter your name: ");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        Node peer = new Node(name);
        peer.port = peer.socket.getLocalPort();
        System.out.println("#Your port is " + peer.port);
        peer.startListening();
        peer.connect();

        while (true) {
            String message = scanner.nextLine();
            if (message.equals("exit")) {
                peer.disconnect();
                break;
            } else {
                Message.sendToAllNodes(peer.socket, new Message("MESSAGE", peer.name, message, peer.vc), peer.nodes);
            }
        }
    }
}
