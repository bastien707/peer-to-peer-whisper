import javax.swing.plaf.multi.MultiListUI;
import java.io.IOException;
import java.net.*;
import java.net.UnknownHostException;
import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;

public class Server {
    private final DatagramSocket socket;
     private HashMap<String, Integer> nodes;

     public Server() throws IOException {
         InetAddress address = InetAddress.getByName("localhost");
         int port = 5000;
         this.nodes = new HashMap<>();
         this.socket = new DatagramSocket(port);
         System.out.println("Server started at " + address + ":" + port);
     }

    public void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                InetAddress group = InetAddress.getByName("233.1.1.1");
                int multicastPort = 1234;
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message msgObj = Message.fromString(message);
                    switch (msgObj.type()) {
                        case "CONNECTION_REQUEST" -> {
                            nodes.put(msgObj.sender(), packet.getPort());
                            String stringToSend = Utils.hashMapToString(nodes) + group + "/" + multicastPort;
                            Message res = new Message("CONNECTION_ACCEPTED", "Server", stringToSend, null);
                            Message.sendMessageObject(this.socket, res, packet.getPort());
                            System.out.println("#" + msgObj.sender() + " has joined the network");
                            System.out.println("Nodes: " + nodes);
                        }
                        case "DISCONNECT" -> {
                            nodes.remove(msgObj.sender());
                            System.out.println("#" + msgObj.sender() + " has left the network");
                            System.out.println("Nodes: " + nodes);
                        }
                        default -> System.out.println("MESSAGE NOT RECOGNIZED");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.startListening();
    }
}
