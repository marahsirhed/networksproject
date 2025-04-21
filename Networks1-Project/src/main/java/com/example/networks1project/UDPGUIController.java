package com.example.networks1project;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UDPGUIController {

    @FXML private TextField LocalAddressField;
    @FXML private TextField LocalPortField;
    @FXML private TextField RemoteAdressField;
    @FXML private TextField RemotePortField;
    @FXML private TextArea EnterTextArea;
    @FXML private Button SendButton;
    @FXML private ListView<String> chatListView;
    @FXML
    private Button deleteAllButton;
    @FXML
    private Button deleteButton;
    private DatagramSocket socket;
    private boolean listening = false;

    private boolean isControlMessage(String message) {
        return message.startsWith("_");
    }

    private boolean isDeleteMessage(String message) {
        return message.startsWith("DELETE");
    }

    private boolean isClearMessage(String message) {
        return message.equals("CLEAR");
    }

    private int getDeleteIndex(String message) {
        try {
            return Integer.parseInt(message.split(":")[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private void sendControlMessage(String controlMessage) {
        try {
            if (socket == null || socket.isClosed()) return;

            InetAddress remoteIP = InetAddress.getByName(RemoteAdressField.getText());
            int remotePort = Integer.parseInt(RemotePortField.getText());

            byte[] data = controlMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, remoteIP, remotePort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void startReceiver() {  // Called when "start chat" button is pressed
        try {
            int localPort = Integer.parseInt(LocalPortField.getText());
            socket = new DatagramSocket(localPort);
            listening = true;

            System.out.println("Started listening on port: " + localPort);

            Thread listenerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];  // Buffer to hold incoming data
                    while (listening) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            socket.receive(packet);
                            String message = new String(packet.getData(), 0, packet.getLength());

                            if (message.startsWith("DELETE")) {
                                String toDelete = message.substring("DELETE".length());
                                Platform.runLater(() -> chatListView.getItems().remove(toDelete));
                            }
                             else if (isClearMessage(message)) {
                                Platform.runLater(() -> chatListView.getItems().clear());
                            } else {
                                    String timestamp = java.time.LocalDateTime.now()
                                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                                    String formattedMessage = "Remote: "  + message +"    "+"[ "+ timestamp + " ]";
                                    updateChatListView(formattedMessage);
                            }

                        } catch (Exception e) {
                            if (listening) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

            listenerThread.setDaemon(true);  // Mark the thread as a daemon so it stops when the application exits
            listenerThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateChatListView(String message) {
        // This method updates the chat list view with the received message
        Platform.runLater(new Runnable() {                                  // this will be modified later to accomodate
            @Override                                                       // for future features - abdalruhamn
            public void run() {
                chatListView.getItems().add(message);  // accept fully formatted message
            }
        });
    }
    @FXML
    private void sendMessage() {
        try {
            if (socket == null || socket.isClosed()) return;

            InetAddress remoteIP = InetAddress.getByName(RemoteAdressField.getText());
            int remotePort = Integer.parseInt(RemotePortField.getText());

            String rawMessage = EnterTextArea.getText();
            byte[] data = rawMessage.getBytes();

            DatagramPacket packet = new DatagramPacket(data, data.length, remoteIP, remotePort);
            socket.send(packet);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String message = "You: " + rawMessage + "           " +"[ "+ timestamp + "]";
            updateChatListView(message);
            System.out.println(message);
            EnterTextArea.clear();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        public void stop() {
            listening = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

    @FXML
    public void initialize() {
        chatListView.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);

                    // Clean logic to avoid overlapping
                    if (item.startsWith("You: ")) {
                        setStyle("-fx-text-fill: blue;");
                    } else if (item.startsWith("Remote: ")) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }
    @FXML
    void deleteMessage() {
        int selectedIndex = chatListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedMessage = chatListView.getItems().get(selectedIndex);
            chatListView.getItems().remove(selectedIndex);
            sendControlMessage("DELETE" + selectedMessage);

        }
    }

    @FXML
    void deleteAllmessages() {
        chatListView.getItems().clear();
        sendControlMessage("CLEAR");
    }

}
