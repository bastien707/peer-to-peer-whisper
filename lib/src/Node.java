import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class Node {
    private String name;
    private DatagramSocket socket;
    private HashMap<Integer, String> nodes;
    private int port;

    public Node(String name) throws SocketException {
        this.name = name;
        this.socket = new DatagramSocket();
        this.nodes = new HashMap<>();
        this.port = getRandomPort();
    }

    public String getName() {
        return name;
    }

    public static int getRandomPort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Can't get random port");
        }
    }

    public void sendInformationToServer(Node n) throws IOException {
        byte[] buffer = (name + ":" + n.port).getBytes();
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

    public void startListening(int port) {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                DatagramSocket receiveSocket = new DatagramSocket(port);
                while (true) {
                    receiveSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if(packet.getPort() == 5000) {
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
            this.nodes.put(Integer.parseInt(nodeInfo[1]), nodeInfo[0]);
        }
    }

    public void sendToAllNodes(String message) throws IOException {
        for (int port: nodes.keySet()) {
            sendMessage(message, port, name);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("#Enter your name: ");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        Node n = new Node(name);
        n.startListening(n.port);
        System.out.println("#You are now connected to the network" + "\n" + "Your port is: " + n.port);
        n.sendInformationToServer(n); // send information to server
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
