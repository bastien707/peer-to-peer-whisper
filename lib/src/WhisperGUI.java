import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;

public class WhisperGUI {
    private Node node;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton sendPrivateButton;
    private JTextField privateInputField;
    private JButton exitButton;
    private JList<String> nodeList;
    private JComboBox<String> comboBox;
    private HashMap<String, JTextArea> privateChats; // <name, chatArea>
    private JScrollPane currentPrivateChatScrollPane;

    public WhisperGUI() throws IOException {

        //+-------------------------------+
        //|  Create the frame and connect |
        //+-------------------------------+

        String name = showUsernameDialog();
        privateChats = new HashMap<>();
        nodeList = new JList<>();
        comboBox = new JComboBox<>();
        currentPrivateChatScrollPane = new JScrollPane();

        frame = new JFrame("Whisper Chat - " + name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.append("#Welcome to Whisper Chat, " + name + "!\n");
        chatArea.append("#Type your message in the input field and press Send to start chatting.\n");
        chatArea.append("#To see the list of connected nodes, click the Node List button.\n");
        chatArea.append("#To exit the chat, click the Exit button or close the window.\n\n");

        // Connect to the network after having created the GUI
        // so that the GUI is ready to receive messages
        node = new Node(name, this);
        node.connect();
        node.startListening();

        //+-----------------------------+
        //|  GUI Components and Layout  |
        //+-----------------------------+

        // Declate the tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setSize(800, 100);

        // Declare the panels for the tabbed pane
        JPanel generalChat = new JPanel(new BorderLayout());
        JPanel privateChatPanel = new JPanel(new BorderLayout());

        // Declare the node list right pane for the general chat
        JScrollPane scrollPane = new JScrollPane(nodeList);
        Dimension preferredSize = new Dimension(200, 600); // Adjust the width and height as needed
        scrollPane.setPreferredSize(preferredSize);

        // Declare the bottom panel for the general chat
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        exitButton = new JButton("Exit");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(exitButton, BorderLayout.WEST);

        generalChat.add(scrollPane, BorderLayout.EAST);
        generalChat.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        generalChat.add(bottomPanel, BorderLayout.SOUTH);

        // Declare the private chat panel
        JPanel bottomSendPanel = new JPanel(new BorderLayout());

        sendPrivateButton = new JButton("Send");
        privateInputField = new JTextField();

        bottomSendPanel.add(privateInputField, BorderLayout.CENTER);
        bottomSendPanel.add(sendPrivateButton, BorderLayout.EAST);

        privateChatPanel.add(comboBox, BorderLayout.NORTH);
        privateChatPanel.add(bottomSendPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("General", generalChat);
        tabbedPane.addTab("Private", privateChatPanel);

        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setVisible(true);

        //+-------------------------+
        //|  Listen for user input  |
        //+-------------------------+

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
            if (!message.isEmpty() && !message.contains(":")) {
                try {
                    Message msg = new Message("MULTICAST_MESSAGE", node.getName(), message, null);
                    Message.broadcast(node.getMulticastSocket(), msg, node.getGroup());
                    chatArea.append("You: " + message + "\n");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            inputField.setText("");
        });

        sendPrivateButton.addActionListener(e -> {
            String message = privateInputField.getText();
            String recipient = (String) comboBox.getSelectedItem();
            if (!message.isEmpty() && !message.contains(":")) {
                try {
                    Message msg = new Message("PRIVATE_MESSAGE", node.getName(), message, null);
                    Message.sendMessageObject(node.getSocket(), msg, node.getNodes().get(recipient));
                    privateChats.get(recipient).append("You: " + message + "\n");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            privateInputField.setText("");
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendButton.doClick();
                }
            }
        });

        /**
         * Every time the user selects a different recipient from the combo box,
         * the private chat area is updated to show the messages sent to the selected recipient.
         */
        comboBox.addActionListener(e -> {
            String selectedRecipient = (String) comboBox.getSelectedItem();
            if (selectedRecipient != null) {
                if (!privateChats.containsKey(selectedRecipient)) {
                    // Create a new private chat area
                    JTextArea privateChatArea = new JTextArea();
                    privateChatArea.setEditable(false);
                    privateChats.put(selectedRecipient, privateChatArea);
                }

                // Remove the previous JScrollPane if it exists
                if (currentPrivateChatScrollPane != null) {
                    privateChatPanel.remove(currentPrivateChatScrollPane);
                }
                // Add the new JScrollPane and update the reference
                currentPrivateChatScrollPane = new JScrollPane(privateChats.get(selectedRecipient));
                privateChatPanel.add(currentPrivateChatScrollPane, BorderLayout.CENTER);
                privateChatPanel.revalidate();
                privateChatPanel.repaint();
            }
        });
    }

    //+-------------------------+
    //| Functions for the GUI   |
    //+-------------------------+

    public void updateNodeList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String nodeName : node.getNodes().keySet()) {
            listModel.addElement(nodeName);
        }
        nodeList.setModel(listModel);
    }

    public void updateComboBox() {
        String[] list = node.getNodes().keySet().toArray(new String[0]);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(list);
        comboBox.setModel(model);
    }

    public void updatePrivateChatArea(String message, String sender) {
        if (!privateChats.containsKey(sender)) {
            JTextArea privateChat = new JTextArea();
            privateChat.setEditable(false);
            privateChats.put(sender, privateChat);
        }
        privateChats.get(sender).append(message + "\n");
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
                new WhisperGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
