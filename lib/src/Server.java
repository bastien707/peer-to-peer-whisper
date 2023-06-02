import java.io.IOException;
import java.net.*;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Server {
     private final InetAddress address;
    private final DatagramSocket socket;
     private HashMap<String, Integer> nodes;

     public Server() throws UnknownHostException, SocketException {
         this.address = InetAddress.getByName("localhost");
         int port = 5000;
         this.nodes = new HashMap<>();
         this.socket = new DatagramSocket(port);
         System.out.println("Server started at " + this.address + ":" + port);
     }

    public void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message msgObj = Message.fromString(message);
                    if (msgObj.type().equals("CONNEXION_REQUEST")) {
                        nodes.put(msgObj.sender(), packet.getPort());
                        Message.sendMessageObject(this.socket, new Message("CONNEXION_ACCEPTED", "Server", Utils.hashMapToString(nodes), null), packet.getPort());
                        System.out.println("#" + msgObj.sender() + " has joined the network");
                        System.out.println("Nodes: " + nodes);
                    } else if (msgObj.type().equals("DISCONNECT")) {
                        nodes.remove(msgObj.sender());
                        System.out.println("#" + msgObj.sender() + " has left the network");
                        System.out.println("Nodes: " + nodes);
                    } else {
                        System.out.println("MESSAGE NOT RECOGNIZED");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) throws UnknownHostException, SocketException {
        Server server = new Server();
        server.startListening();
    }
}
