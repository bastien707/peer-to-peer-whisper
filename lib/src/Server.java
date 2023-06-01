import java.io.IOException;
import java.net.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
     private final InetAddress address;
     private final int port;
     private final DatagramSocket socket;
     private HashMap<Integer, String> nodeNames; // map of node names and ports

     public Server() throws UnknownHostException, SocketException {
         this.address = InetAddress.getByName("localhost");
         this.port = 5000;
         this.nodeNames = new HashMap<Integer, String>();
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
                    if(nodeNames.size() >= 10) {
                        System.out.println("Maximum number of nodes reached");
                    } else {
                        System.out.println("Node " + parts[0] + " connected on port " + parts[1]);
                        nodeNames.put(Integer.parseInt(parts[1]), parts[0]);
                        System.out.println(nodeNames);
                        sendNodeNamesToNodes();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendNodeNamesToNodes() throws IOException {
        StringBuilder message = new StringBuilder();
        for (Integer p : nodeNames.keySet()) {
            // get the node name and port and append to message
            message.append(nodeNames.get(p)).append(":").append(p).append(",");
        }
        System.out.println(message);
        for (Integer port : nodeNames.keySet()) {
            byte[] buffer = message.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
        }
    }

    public static void main(String[] args) throws UnknownHostException, SocketException {
        Server server = new Server();
        server.startListening();
    }
}
