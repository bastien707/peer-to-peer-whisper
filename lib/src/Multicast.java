import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

public class Multicast {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter multicast group address: ");
        String groupAddress = scanner.nextLine();
        System.out.print("Enter multicast group port: ");
        int groupPort = scanner.nextInt();
        scanner.nextLine(); // consume the newline character
        InetAddress group = InetAddress.getByName(groupAddress);
        MulticastSocket socket = new MulticastSocket(groupPort);
        socket.joinGroup(group);
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        while (true) {
            String message = scanner.nextLine();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, groupPort);
            socket.send(packet);
        }
    }
}