import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Node {
    private final String name;
    private final DatagramSocket socket;
    private HashMap<String, Integer> nodes;
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private VectorClock vc;
    private int port;
    private WhisperGUI gui;

    public Node(String name, WhisperGUI gui) throws IOException {
        this.name = name;
        this.socket = new DatagramSocket();
        this.nodes = new HashMap<>();
        this.port = socket.getLocalPort();
        this.vc = new VectorClock(name);
        this.multicastSocket = new MulticastSocket();
        this.group = null;
        this.gui = gui;
    }

    public InetAddress getGroup() {
        return group;
    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public String getName() {
        return name;
    }

    public HashMap<String, Integer> getNodes() {
        return nodes;
    }

    public void setMultiCastSocket(int port) throws IOException {
        this.multicastSocket = new MulticastSocket(port);
    }

    public void setGroup(String group) throws UnknownHostException {
        this.group = InetAddress.getByName(group);
    }

    /**
     * Connect to the network by sending a CONNECTION_REQUEST message to the auth server
     * @throws IOException if the socket is not valid
     */
    public void connect() throws IOException {
        gui.updateChatArea("#Connecting to the network...");
        Message newMessage = new Message("CONNECTION_REQUEST", name, null, null);
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
                        case "CONNECTION_ACCEPTED" -> ConnectionAccepted(msgObj);
                        case "PRIVATE_MESSAGE" -> gui.updatePrivateChatArea(msgObj.sender() + ": " + msgObj.content(), msgObj.sender());
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
     * When the server accepts the connection, the new node receives the list of nodes and the multicast port
     * and then sends a message to all nodes to update their list too.
     * @param msgObj the message containing the nodes and the multicast port
     * @throws IOException if the socket is not valid
     */
    public void ConnectionAccepted(Message msgObj) throws IOException {
        String[] split = msgObj.content().split("/");
        setGroup(split[1]);
        setMultiCastSocket(Integer.parseInt(split[2]));
        joinMulticastGroup();
        updateNodesFromMessage(split[0]);
        Message msg = new Message("NEW_PEER", this.name, Utils.hashMapToString(nodes), vc);
        Message.broadcast(this.multicastSocket, msg, this.group);
        gui.updateChatArea("#You are now connected to the network");
        gui.updateNodeList();
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
     * @throws IOException if the socket is not valid
     */
    public void joinMulticastGroup() throws IOException {
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
                            if (!msgObj.sender().equals(this.name)) {
                                gui.updateChatArea(msgObj.sender() + ":" + msgObj.content());
                            }
                        }
                        case "NEW_PEER" -> {
                            gui.updateChatArea("#" + msgObj.sender() + " has joined the network");
                            updateNodesFromMessage(msgObj.content());
                            gui.updateNodeList();
                            gui.updateComboBox();
                        }
                        case "DISCONNECT" -> {
                            gui.updateChatArea("#" + msgObj.sender() + " has left the network");
                            nodes.remove(msgObj.sender());
                            gui.updateNodeList(); // must update the node list after removing a node
                            gui.updateComboBox(); // must update the combo box after removing a node
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
}
