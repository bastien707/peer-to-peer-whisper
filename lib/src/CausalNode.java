import jdk.jshell.execution.Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class CausalNode {
    private String name;
    private HashMap<String, Integer> nodes;
    private HashMap<String, Integer> delivered;
    private HashMap<String, Integer> deps;
    private int sendSeq;
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private ArrayList<Message> buffer;

    public CausalNode(String name, int multicastPort) throws IOException {
        this.name = name;
        this.nodes = new HashMap<>();
        this.delivered = new HashMap<>();
        this.deps = new HashMap<>();
        this.sendSeq = 0;
        this.multicastSocket = new MulticastSocket(multicastPort);
        this.group = InetAddress.getByName("233.1.1.2"); // broadcast address
        this.multicastSocket.joinGroup(group);
        this.buffer = new ArrayList<>();
    }

    public record Message (String senderName, HashMap<String, Integer> deps, String message) {

        public static void broadcast(MulticastSocket socket, Message message, InetAddress groupAddress) throws IOException {
            byte[] buffer = (message.toString()).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupAddress, 6666);
            socket.send(packet);
        }

        public static Message fromString(String message) {
            String[] parts = message.split(":");
            String sender = parts[0];
            HashMap<String, Integer> deps = Utils.stringToHashMap(parts[1]);
            String content = parts[2];
            return new Message(sender, deps, content);
        }

        @Override
        public String toString() {
            return senderName + ":" + deps + ":" + message;
        }

    }

    public void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                Random random = new Random();
                while (true) {
                    multicastSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message msgObj = Message.fromString(message);
                    onReceive(msgObj);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void charlieListen() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                Random random = new Random();
                while (true) {
                    multicastSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    Message msgObj = Message.fromString(message);
                    new Thread(() -> {
                        try {
                            if (Objects.equals(msgObj.senderName, "alice")) Thread.sleep(5000);
                            onReceive(msgObj);
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

    /**
     * Checks if deps is less than or equal to delivered
     * Since we use hashmaps, we need to ensure that deps and delivered have the same keys
     * @param deps dependencies of message
     * @param delivered delivered messages
     * @return boolean
     */
    private boolean isDepsLessThanOrEqualToDelivered(HashMap<String, Integer> deps, HashMap<String, Integer> delivered) {
        //make copy of deps and delivered
        HashMap<String, Integer> depsCopy = new HashMap<>(deps);
        HashMap<String, Integer> deliveredCopy = new HashMap<>(delivered);
        // Ensure deps and delivered have the same keys
        if (depsCopy.size() < deliveredCopy.size()) {
            // put absent keys in deps
            for (String key : deliveredCopy.keySet()) {
                if (!depsCopy.containsKey(key)) {
                    depsCopy.put(key, 0);
                }
            }
        } else if (depsCopy.size() > deliveredCopy.size()) {
            // put absent keys in delivered
            for (String key : depsCopy.keySet()) {
                if (!deliveredCopy.containsKey(key)) {
                    deliveredCopy.put(key, 0);
                }
            }
        }

        for (String key : depsCopy.keySet()) {
            if (depsCopy.get(key) > deliveredCopy.get(key)) {
                return false;  // deps > delivered for at least one key
            }
        }

        return true;  // deps <= delivered for all keys
    }


    public void onRequest(String m) throws IOException, InterruptedException {
        String namedMessage = name + "# " + m;
        deps.putAll(delivered); // deps := delivered
        if(!deps.containsValue(name)) {
            deps.put(name, sendSeq); // deps[i] := sendSeq
        }
        InetAddress address = InetAddress.getByName("233.1.1.2");
        Message msgObj = new Message(name, deps, namedMessage);
        Message.broadcast(multicastSocket, msgObj, address);
        sendSeq++;
        System.out.println("\n" + this.name + " sent: " + namedMessage);
    }

    public void onReceive(Message m) throws IOException, InterruptedException {
        if(!m.senderName.equals(this.name)) System.out.println("\n" + this.name + " received: " + m.message);
        if(!delivered.containsKey(m.senderName)) {
            delivered.put(m.senderName, 0);
        }
        if(!deps.containsKey(m.senderName)) {
            deps.put(m.senderName, 0);
        }

        buffer.add(m); // buffer := buffer U {m}

        boolean deliverMessage;
        do {
            deliverMessage = false;
            for(Message msg : buffer) {
                if(isDepsLessThanOrEqualToDelivered(msg.deps, delivered)) { // deps <= delivered
                    if(!msg.senderName.equals(this.name)) System.out.println(this.name + " delivered: " + msg); // deliver(m)
                    buffer.remove(msg); // buffer := buffer \ {m}
                    delivered.put(msg.senderName, delivered.get(msg.senderName) + 1); // delivered[sender] := delivered[sender] + 1
                    deliverMessage = true;
                    break;
                }
            }
        } while(deliverMessage);
    }

    public static void main(String[] args) {
        try {
            CausalNode alice = new CausalNode("alice", 6666);
            CausalNode bob = new CausalNode("bob", 6666);
            CausalNode charlie = new CausalNode("charlie", 6666);

            alice.startListening();
            bob.startListening();
            charlie.charlieListen();

            new Thread(() -> {
                try {
                    // Simulate causal dependency where bob replies to alice
                    // without the sleep, messages are concurrent so, they would be delivered in any order
                    alice.onRequest("Hey Guys, wanna hang out?");
                    Thread.sleep(1000);
                    bob.onRequest("I'm up for it!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
