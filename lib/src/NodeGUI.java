import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class NodeGUI {
    private Node node;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton exitButton;
    private JButton nodeListButton;
    private JList<String> nodeList;

    public NodeGUI() throws IOException {
        String name = showUsernameDialog();

        frame = new JFrame("Whisper Chat - " + name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        chatArea.append("#Welcome to Whisper Chat, " + name + "!\n");
        chatArea.append("#Type your message in the input field and press Send to start chatting.\n");
        chatArea.append("#To see the list of connected nodes, click the Node List button.\n");
        chatArea.append("#To exit the chat, click the Exit button or close the window.\n\n");

        // Connect to the network after having created the GUI
        // so that the GUI is ready to receive messages
        node = new Node(name, this);
        node.connect();
        node.startListening();
        node.joinMulticastGroup(node.getGroup());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        bottomPanel.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        exitButton = new JButton("Exit");

        exitButton.addActionListener(e -> {
            try {
                node.disconnect();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        sendButton.addActionListener(e -> {
            String message = inputField.getText();
            System.out.println(message);
            if (!message.isEmpty()) {
                try {
                    Message msg = new Message("MULTICAST_MESSAGE", node.getName(), message, null);
                    Message.broadcast(node.getMulticastSocket(), msg, node.getGroup());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            inputField.setText("");
        });
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(exitButton, BorderLayout.WEST);

        nodeListButton = new JButton("Node List");
        nodeListButton.addActionListener(e -> {
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String nodeName : node.getNodes().keySet()) {
                listModel.addElement(nodeName);
            }
            nodeList.setModel(listModel);
        });

        bottomPanel.add(nodeListButton, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        nodeList = new JList<>();
        frame.add(new JScrollPane(nodeList), BorderLayout.EAST);

        frame.setVisible(true);
    }

    public void updateChatArea(String message) {
        chatArea.append(message + "\n");
    }
    private String showUsernameDialog() {
        String username = "";
        JTextField usernameField = new JTextField(20);
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Enter your name:"));
        panel.add(usernameField);
        int result = JOptionPane.showConfirmDialog(null, panel, "Username", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            username = usernameField.getText();
        } else {
            System.exit(0);
        }

        return username;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new NodeGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
