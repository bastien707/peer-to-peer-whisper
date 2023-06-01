import java.io.IOException;
import java.net.*;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Server {
     private final InetAddress address;
     private final int port;
     private final DatagramSocket socket;
    private HashMap<String, Integer> nodes;

     public Server() throws UnknownHostException, SocketException {
         this.address = InetAddress.getByName("localhost");
         this.port = 5000;
         this.nodes = new HashMap<>();
         this.socket = new DatagramSocket(port);
         System.out.println("Server started at " + this.address + ":" + this.port);
     }

    public void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    String[] parts = message.split(":");
                    if(nodes.size() >= 10) {
                        System.out.println("Maximum number of nodes reached");
                    } else {
                        System.out.println("Node " + parts[0] + " connected on port " + parts[1]);
                        nodes.put(parts[0], Integer.parseInt(parts[1]));
                        System.out.println(nodes);
                        sendNodeStringNamesToNode();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendNodeStringNamesToNode() throws IOException {
        StringBuilder message = new StringBuilder();
        for (String name : nodes.keySet()) {
            // get the node name and port and append to message
            message.append(name).append(":").append(nodes.get(name)).append(",");
        }
        for (String name : nodes.keySet()) {
            byte[] buffer = message.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, nodes.get(name));
            socket.send(packet);
        }
    }


    public static void main(String[] args) throws UnknownHostException, SocketException {
        Server server = new Server();
        server.startListening();
    }
}
