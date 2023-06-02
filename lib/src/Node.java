import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;

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
        this.vc = new VectorClock();
    }

    public void connect() throws IOException {
        System.out.println("#Connecting to server...");
        byte[] buffer = (this.name + ":" + this.socket.getLocalPort()).getBytes();
        InetAddress address = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5000);
        socket.send(packet);
    }

    public void sendMessage(String message, int port, String name) throws IOException {
        byte[] buffer = (name + ":" + message).getBytes();
        InetAddress address = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    public void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if(packet.getPort() == 5000) {
                        System.out.println("#You are now connected to the network");
                        nodeNamesToHashMap(message);
                        System.out.println("Server:" + nodes);
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void nodeNamesToHashMap(String message) {
        String[] nodes = message.split(",");
        for (String node: nodes) {
            String[] nodeInfo = node.split(":");
            this.nodes.put(nodeInfo[0], Integer.parseInt(nodeInfo[1]));
        }
    }

    public void sendToAllNodes(String message) throws IOException {
        for (String port: nodes.keySet()) {
            sendMessage(message, nodes.get(port), name);
        }
    }

    public void incrementVectorClock() {
        vc.increment(name);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("#Enter your name: ");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        Node n = new Node(name);
        System.out.println("#Your port is socket" + n.socket.getLocalPort());
        n.port = n.socket.getLocalPort();
        System.out.println("#Your port is " + n.port);
        n.startListening();
        n.connect(); // send information to server
        while (true) {
            String message = scanner.nextLine();
            try {
                n.sendToAllNodes(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
