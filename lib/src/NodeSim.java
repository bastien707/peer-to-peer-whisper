import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class NodeSim {
    private String name;
    private DatagramSocket socket;
    private HashMap<String, Integer> nodes;
    private HashMap<String, Integer> delivered;
    int port;
    private int sendSeq;
    private ArrayList<Message> buffer;

    public record Message (String senderName, int sendSeq, String message) {

        public Message(String senderName, int sendSeq, String message) {
            this.senderName = senderName;
            this.sendSeq = sendSeq;
            this.message = message;
        }

        public static void sendMessageObject(DatagramSocket socket, Message message, int portDestination) throws IOException, InterruptedException {
            byte[] buffer = (message.toString()).getBytes();
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, portDestination);
            socket.send(packet);
        }

        public static Message fromString(String message) {
            String[] parts = message.split(":");
            String senderName = parts[0];
            int sendSeq = Integer.parseInt(parts[1]);
            String content = parts[2];
            return new Message(senderName, sendSeq, content);
        }

        public String toString() {
            return senderName + ":" + sendSeq + ":" + message;
        }
    }

    public NodeSim(String name, int port) throws IOException {
        this.name = name;
        this.socket = new DatagramSocket(port);
        this.nodes = new HashMap<>();
        this.port = port;
        this.delivered = new HashMap<>();
        this.sendSeq = 0;
        this.buffer = new ArrayList<>();
    }

    /**
     * A node listen on its own port for incoming messages
     */
    public void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                Random random = new Random();
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message m = Message.fromString(message);

                    // Simulate the fact that the message is not received instantly
                    // Use a separate thread for each received message to process them concurrently
                    new Thread(() -> {
                        try {
                            int delay = random.nextInt(5000); // Random delay between 0 and 1000 milliseconds
                            Thread.sleep(delay);
                            onReceiving(m);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void onReceiving(Message m) throws IOException {
        System.out.println("Received: " + m);
        buffer.add(m);
        if(!delivered.containsKey(m.senderName)) delivered.put(m.senderName, 0);
        Message m1 = new Message(m.senderName, delivered.get(m.senderName), m.message);
        System.out.println("Is " +  m1 + " E " + buffer);

        while(buffer.contains(m1)) {
            buffer.remove(m1);
            delivered.put(m.senderName, delivered.get(m.senderName) + 1);
            System.out.println("Delivered: " + m1);
        }
    }

    public void requestToBroadcast(String m, int port) throws IOException, InterruptedException {
        Message msgObj = new Message(this.name, this.sendSeq, m);
        System.out.println("Requesting to broadcast: " + msgObj);
        Message.sendMessageObject(this.socket, msgObj, port);
        this.sendSeq++;
    }

    @Override
    public String toString() {
        return "NodeSim{" +
                "name='" + name + '\'' +
                ", delivered=" + delivered +
                ", port=" + port +
                ", sendSeq=" + sendSeq +
                ", buffer=" + buffer +
                '}';
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        NodeSim bob = new NodeSim("bob", 5000);
        NodeSim alice = new NodeSim("alice", 5001);
        System.out.println(bob);
        System.out.println(alice);

        bob.startListening();
        alice.startListening();

        new Thread(() -> {
            try {
                alice.requestToBroadcast("first", bob.port);
                alice.requestToBroadcast("mid", bob.port);
                alice.requestToBroadcast("last", bob.port);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

    }
}

