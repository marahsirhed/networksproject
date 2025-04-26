package com.example.networks1project.controller;

import com.example.networks1project.model.ArchiveManager;
import com.example.networks1project.model.Message;
import com.example.networks1project.network.UDPPeer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UDPGUIController {

    @FXML private TextField LocalAddressField;
    @FXML private TextField LocalPortField;
    @FXML private TextField RemoteAdressField;
    @FXML private TextField RemotePortField;
    @FXML private TextArea EnterTextArea;
    @FXML private Button SendButton;
    @FXML private ListView<String> chatListView;
    @FXML private Button deleteAllButton;
    @FXML private Button deleteButton;
    @FXML private Button archiveButton;

    private UDPPeer udpPeer;
    private static UDPGUIController instance;
    public UDPGUIController() {
        instance = this;
    }
    public static UDPGUIController getInstance() {
        return instance;
    }
    @FXML
    public void initialize() {
        instance = this;
        chatListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("You:")) setStyle("-fx-text-fill: blue;");
                    else if (item.startsWith("Remote:")) setStyle("-fx-text-fill: orange;");
                    else setStyle("");
                }
            }
        });

    }

    @FXML
    private void startReceiver() {
        String localIP = LocalAddressField.getText();
        int localPort = Integer.parseInt(LocalPortField.getText());
        String remoteIP = RemoteAdressField.getText();
        int remotePort = Integer.parseInt(RemotePortField.getText());

        udpPeer = new UDPPeer(localIP, localPort, remoteIP, remotePort);
        udpPeer.setMessageHandler(this::handleIncomingMessage);
        udpPeer.startListening();
    }

//    private void handleIncomingMessage(String rawMessage) {
//        Platform.runLater(() -> {
//            if (rawMessage.startsWith("DELETE|")) {
//                String toDelete = rawMessage.substring("DELETE|".length()).trim();
//
//                boolean removed = chatListView.getItems().removeIf(item -> {
//                    String normalizedItem = item.replace("You: ", "").replace("Remote: ", "").trim();
//                    return normalizedItem.equals(toDelete);
//                });
//
//                if (removed) {
//                    // Determine whether the deleted message was sent or received
//                    String archivedVersion = chatListView.getItems().stream()
//                            .anyMatch(msg -> msg.startsWith("You: ") && msg.contains(toDelete))
//                            ? "You: " + toDelete
//                            : "Remote: " + toDelete;
//                    ArchiveController.addMessageToArchive(archivedVersion);
//                }
//            }
//            else if (rawMessage.equals("CLEAR")) {
//                List<String> currentMessages = new ArrayList<>(chatListView.getItems());
//                chatListView.getItems().clear();
//                currentMessages.forEach(ArchiveController::addMessageToArchive);
//            }
//            else if (rawMessage.startsWith("RESTORE|")) {
//                String restored = rawMessage.substring("RESTORE|".length());
//                chatListView.getItems().add("Remote: " + restored);
//            } else {
//                chatListView.getItems().add("Remote: " + rawMessage);
//            }
//        });
//    }
@FXML
private void sendMessage() {
    String text = EnterTextArea.getText();
    if (text.isEmpty() || udpPeer == null) return;
    Message message = new Message("You", text);
    udpPeer.sendMessage("MESSAGE|" + text); // include a prefix to distinguish
    chatListView.getItems().add(message.getFormatted());
    EnterTextArea.clear();
}

    private void handleIncomingMessage(String rawMessage) {
        Platform.runLater(() -> {
            if (rawMessage.startsWith("DELETE_MESSAGE|")) {
                String toDelete = rawMessage.substring("DELETE_MESSAGE|".length()).trim();
                for (String item : new ArrayList<>(chatListView.getItems())) {
                    String normalized = item.replace("You: ", "").replace("Remote: ", "").trim();
                    if (normalized.equals(toDelete)) {
                        chatListView.getItems().remove(item);
                        ArchiveController.addMessageToArchive(item);
                        break;
                    }
                }
            } else if (rawMessage.startsWith("CLEAR")) {
                List<String> messages = new ArrayList<>(chatListView.getItems());
                chatListView.getItems().clear();
                messages.forEach(ArchiveController::addMessageToArchive);

            }  else if (rawMessage.startsWith("MESSAGE|")) {
                String messageContent = rawMessage.substring("MESSAGE|".length()).trim();
                String timestamp = java.time.LocalTime.now().withNano(0).toString();
                String messageWithTimestamp = "Remote: " + messageContent + " [" + timestamp + "]";
                chatListView.getItems().add(messageWithTimestamp);
            }
        });
    }

    @FXML
    private void deleteMessage() {
        int selectedIndex = chatListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedMessage = chatListView.getItems().get(selectedIndex);
            ArchiveController.addMessageToArchive(selectedMessage);
            chatListView.getItems().remove(selectedIndex);
        }
    }
    @FXML
    private void deleteAllmessages() {
        // Create a list of all the messages in the current chat
        List<String> allMessages = new ArrayList<>(chatListView.getItems());
        // Check if there are any messages to delete
        if (!allMessages.isEmpty()) {
            // Clear all messages from the chat
            chatListView.getItems().clear();
            // Iterate through each message
            for (String msg : allMessages) {
                // Archive the message before deleting
                ArchiveController.addMessageToArchive(msg);
                // Send DELETE message to the receiver, stripped of "You:" and "Remote:" prefixes
                String messageToDelete = msg.replace("You: ", "").replace("Remote: ", "").trim();
                // Ensure udpPeer is not null before attempting to send a message
                if (udpPeer != null) {
                    udpPeer.sendMessage("DELETE_MESSAGE|" + messageToDelete);  // Send DELETE message with identifier
                }
            }
        }
    }




    @FXML
    private void archiveMessage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/networks1project/archive.fxml"));
            Parent root = fxmlLoader.load();

            ArchiveController controller = fxmlLoader.getController();

            Stage newStage = new Stage();
            controller.setStage(newStage);

            newStage.setTitle("Archived Messages");
            newStage.setScene(new Scene(root, 403, 350));
            newStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void restoreMessageToChat(String rawMessage) {
        chatListView.getItems().add("You: " + rawMessage);
        if (udpPeer != null) {
            udpPeer.sendMessage("RESTORE|" + rawMessage);
        }
    }
    public void stop() {
        if (udpPeer != null) {
            udpPeer.stop();
        }
    }

    public void restoreFromArchive(String message) {
        chatListView.getItems().add(message);
        if (udpPeer != null) {
            udpPeer.sendMessage("RESTORE|" + message.replace("You: ", "").replace("Remote: ", "").trim());
        }

    }
}
