import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Node {
    private final String name;
    private final DatagramSocket socket;
    private HashMap<String, Integer> nodes;
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private VectorClock vc;
    private int port;
    private NodeGUI gui;

    public Node(String name, NodeGUI gui) throws IOException {
        this.name = name;
        this.socket = new DatagramSocket();
        this.nodes = new HashMap<>();
        this.port = socket.getLocalPort();
        this.vc = new VectorClock(name);
        this.multicastSocket = new MulticastSocket(1234);
        this.group = InetAddress.getByName("233.1.1.1");
        this.gui = gui;
    }

    public InetAddress getGroup() {
        return group;
    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    public String getName() {
        return name;
    }

    public HashMap<String, Integer> getNodes() {
        return nodes;
    }

    /**
     * Connect to the network by sending a CONNEXION_REQUEST message to the auth server
     * @throws IOException if the socket is not valid
     */
    public void connect() throws IOException {
        gui.updateChatArea("#Connecting to the network...");
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
        Message.broadcast(this.multicastSocket, newMessage, this.group);
        this.multicastSocket.close();
    }

    /**
     * A node listen on its own port for incoming messages
     */
    public void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message msgObj = Message.fromString(message);
                    switch (msgObj.type()) {
                        // when the server accepts the connection, the new node receives the list of nodes
                        // and then sends a message to all nodes to update their list too.
                        case "CONNEXION_ACCEPTED" -> {
                            gui.updateChatArea("#You are now connected to the network");
                            gui.updateChatArea("#Your port is " + this.port + " and your address is " + InetAddress.getLocalHost() + "\n" + "#Multicast group: " + this.group);
                            updateNodesFromMessage(msgObj.content());
                            Message msg = new Message("NEW_PEER", this.name, Utils.hashMapToString(nodes), vc);
                            Message.broadcast(this.multicastSocket, msg, this.group);
                        }
                        case "MESSAGE" -> gui.updateChatArea(msgObj.sender() + ": " + msgObj.content());
                        default -> System.out.println("#SOMETHING WENT WRONG...");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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

    /**
     * Join a multicast group to listen for multicast messages
     * @param group the multicast group
     * @throws IOException if the socket is not valid
     */
    public void joinMulticastGroup(InetAddress group) throws IOException {
        AtomicBoolean shouldCloseSocket = new AtomicBoolean(false);
        multicastSocket.joinGroup(group);
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (!shouldCloseSocket.get()) {
                    multicastSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message msgObj = Message.fromString(message);
                    switch (msgObj.type()) {
                        case "MULTICAST_MESSAGE" -> {
                            gui.updateChatArea(msgObj.sender() + ":" + msgObj.content());
                        }
                        case "NEW_PEER" -> {
                            gui.updateChatArea("#" + msgObj.sender() + " has joined the network");
                            updateNodesFromMessage(msgObj.content());

                        }
                        case "DISCONNECT" -> {
                            gui.updateChatArea("#" + msgObj.sender() + " has left the network");
                            nodes.remove(msgObj.sender());
                            // must close socket if the sender is the one who left
                            if (msgObj.sender().equals(this.name)) {
                                shouldCloseSocket.set(true);
                            }
                        }
                        default -> System.out.println("#SOMETHING WENT WRONG...");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (shouldCloseSocket.get()) {
                    multicastSocket.close();
                }
            }
        }).start();
    }

    @Override
    public String toString() {
        return "#Your name is " + this.name + "\n" + "#Your port is " + this.port + "\n" + "#Your address is " + this.socket.getLocalAddress() + "\n" + "#Multicast group: " + this.group;
    }


    /**
    public static void main(String[] args) throws IOException {

        System.out.println("#Enter your name: ");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        NodeGUI gui = new NodeGUI();
        Node peer = new Node(name, gui);
        peer.port = peer.socket.getLocalPort();
        System.out.println("#Your port is " + peer.port);
        peer.startListening();
        peer.connect();

        peer.joinMulticastGroup(peer.group);

        while (true) {
            String message = scanner.nextLine();
            if (message.equals("exit")) {
                peer.disconnect();
                break;
            } else if(message.equals("list")) {
                System.out.println(peer.nodes);
                int port = scanner.nextInt();
                scanner.nextLine(); // prevent following "else" from being triggered
                Message msgObj = new Message("MESSAGE", peer.name, "Hello", null);
                Message.sendMessageObject(peer.socket, msgObj, port);
            } else {
                Message msgObj = new Message("MULTICAST_MESSAGE", peer.name, message, null);
                Message.broadcast(peer.multicastSocket, msgObj, peer.group);
            }
        }
    }
    */
}
