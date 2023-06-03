import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;

public class WhisperGUI {
    private Node node;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton sendPrivateButton;
    private JTextField privateInputField;
    private JButton exitButton;
    private JList<String> nodeList;
    private JComboBox<String> comboBox;
    private HashMap<String, JTextArea> privateChats;
    private JScrollPane currentPrivateChatScrollPane;
    private JPanel privateChatPanel;

    public WhisperGUI() throws IOException {
        initComponents();
        initListeners();
        node.connect();
        node.startListening();
    }

    /**
     * Initialize the components of the GUI
     * @throws IOException if the socket is not valid
     */
    private void initComponents() throws IOException {
        String name = showUsernameDialog();
        privateChats = new HashMap<>();
        nodeList = new JList<>();
        comboBox = new JComboBox<>();
        currentPrivateChatScrollPane = new JScrollPane();

        JFrame frame = new JFrame("Whisper Chat - " + name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        chatArea = createChatArea(name);
        node = new Node(name, this);
        JTabbedPane tabbedPane = createTabbedPane();
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    /**
     * Create the general chat area with welcome messages
     * @param name the name of the user
     * @return the chat area
     */
    private JTextArea createChatArea(String name) {
        JTextArea chat = new JTextArea();
        chat.setEditable(false);
        chat.append("#Welcome to Whisper Chat, " + name + "!\n");
        chat.append("#Type your message in the input field and press Send to start chatting\n");
        chat.append("#To exit the chat, click the Exit\n\n");

        return chat;
    }

    /**
     * Create the tabbed pane with the general chat and the private chat
     * @return the tabbed pane
     */
    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setSize(800, 100);

        JPanel generalChat = createGeneralChatPanel();
        JPanel privateChatPanel = createPrivateChatPanel();

        tabbedPane.add("General", generalChat);
        tabbedPane.add("Private", privateChatPanel);

        return tabbedPane;
    }

    private JPanel createGeneralChatPanel() {
        JPanel generalChat = new JPanel(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(nodeList);
        Dimension preferredSize = new Dimension(200, 600);

        JPanel bottomPanel = createBottomPanel();

        generalChat.add(scrollPane, BorderLayout.EAST);
        generalChat.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        generalChat.add(bottomPanel, BorderLayout.SOUTH);

        scrollPane.setPreferredSize(preferredSize);

        return generalChat;
    }

    /**
     * Create the bottom panel which contains the input field and the buttons
     * It is used for general chat
     * @return the bottom panel
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());

        inputField = new JTextField();
        sendButton = new JButton("Send");
        exitButton = new JButton("Exit");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(exitButton, BorderLayout.WEST);

        return bottomPanel;
    }

    /**
     * Create the private chat panel which contains the combo box and the input field
     * Combo box is used to select the recipient of the private message
     * @return the private chat panel
     */
    private JPanel createPrivateChatPanel() {
        privateChatPanel = new JPanel(new BorderLayout());
        JPanel bottomSendPanel = new JPanel(new BorderLayout());

        sendPrivateButton = new JButton("Send");
        privateInputField = new JTextField();

        bottomSendPanel.add(privateInputField, BorderLayout.CENTER);
        bottomSendPanel.add(sendPrivateButton, BorderLayout.EAST);

        privateChatPanel.add(comboBox, BorderLayout.NORTH);
        privateChatPanel.add(bottomSendPanel, BorderLayout.SOUTH);

        return privateChatPanel;
    }

    /**
     * Initialize all listeners
     */
    private void initListeners() {
        exitButton.addActionListener(e -> {
            try {
                node.disconnect();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendButton.doClick();
                }
            }
        });

        privateInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendPrivateButton.doClick();
                }
            }
        });

        sendButton.addActionListener(e -> sendGeneralMessage());
        sendPrivateButton.addActionListener(e -> sendPrivateMessage());
        comboBox.addActionListener(e -> updatePrivateChat());
    }

    /**
     * Broadcast a message to all nodes
     * Message is sent only if it is not empty and does not contain ":"
     */
    private void sendGeneralMessage() {
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
    }

    /**
     * Send a private message to a specific node
     * Message is sent only if it is not empty and does not contain ":"
     */
    private void sendPrivateMessage() {
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
    }

    /**
     * Update the private chat area when a new recipient is selected
     * If the private chat area does not exist, it is created
     * If the private chat area exists, it is removed and the new one is added with the updated reference
     * If the selected recipient is null, nothing happens
     */
    private void updatePrivateChat() {
        String selectedRecipient = (String) comboBox.getSelectedItem();
        if (selectedRecipient != null) {
            if (!privateChats.containsKey(selectedRecipient)) {
                // Create a new private chat area
                JTextArea privateChatArea = new JTextArea();
                privateChatArea.setEditable(false);
                privateChats.put(selectedRecipient, privateChatArea);
            }

            if (currentPrivateChatScrollPane != null) {
                privateChatPanel.remove(currentPrivateChatScrollPane);
            }

            currentPrivateChatScrollPane = new JScrollPane(privateChats.get(selectedRecipient));
            privateChatPanel.add(currentPrivateChatScrollPane, BorderLayout.CENTER);
            privateChatPanel.revalidate();
            privateChatPanel.repaint();
        }
    }

    /**
     * Update the list of nodes in the general chat right panel
     */
    public void updateNodeList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String nodeName : node.getNodes().keySet()) {
            listModel.addElement(nodeName);
        }
        nodeList.setModel(listModel);
    }

    /**
     * Update the combo box with the list of nodes on the private chat panel
     */
    public void updateComboBox() {
        String[] list = node.getNodes().keySet().toArray(new String[0]);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(list);
        comboBox.setModel(model);
    }

    /**
     * Update the private chat area when a new message is received for the selected recipient
     * If the private chat area does not exist with the sender, it is created
     * @param message the message received
     * @param sender the sender of the message
     */
    public void updatePrivateChatArea(String message, String sender) {
        if (!privateChats.containsKey(sender)) {
            JTextArea privateChat = new JTextArea();
            privateChat.setEditable(false);
            privateChats.put(sender, privateChat);
        }
        privateChats.get(sender).append(message + "\n");
    }

    /**
     * Update the general chat area when a new message is received
     * @param message the message received
     */
    public void updateChatArea(String message) {
        chatArea.append(message + "\n");
    }

    /**
     * Show a dialog to enter the username before starting the chat
     * @return the username entered
     */
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
