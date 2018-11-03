package org.academiadecodigo.hashtronauts.server.clients;

import org.academiadecodigo.hashtronauts.comms.Communication;
import org.academiadecodigo.hashtronauts.server.users.User;
import org.academiadecodigo.hashtronauts.server.utils.ServerMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.academiadecodigo.hashtronauts.comms.Communication.*;

/**
 * Represents an client itself
 */
public class Client implements Runnable {

    /** Connection to the client */
    private final Socket clientSocket;

    /** Client Streams */
    private PrintWriter outputStream;
    private BufferedReader inputStream;

    /** Connection to the server */
    private ClientConnector serverBridge;

    /** Associated user to this client */
    private User user;

    public Client(Socket socket) {
        this.clientSocket = socket;

        try {
            this.outputStream = new PrintWriter(socket.getOutputStream(), true);
            this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Validates if Client connected successfully
     *
     * @return true if connected
     */
    public boolean clientConnected() {
        return !clientSocket.isClosed() && (outputStream != null && inputStream != null);
    }

    public void setServerBridge(ClientConnector serverBridge) {
        this.serverBridge = serverBridge;
    }

    /**
     * Client Thread Entry Point
     */
    @Override
    public void run() {
        while (!clientSocket.isClosed()) {
            try {
                String message = receiveFromClient();

                handleInput(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(ServerMessages.CLIENT_DISCONNECTED);

    }


    /**
     * Handles messages received from client
     *
     * @param message the message received
     */
    private void handleInput(String message) {

        if (!isValid(message)) {
            return;
        }

        Method method = Communication.getMethodFromMessage(message);

        Command command = Communication.getCommandFromMessage(message);

        String[] args = message.split(" ")[2].split(",");

        if (method == Method.POST) {
            switch (command) {
                case LOGIN:
                    user = serverBridge.login(args[0], args[1].hashCode());
                    if (user == null ){
                        sendToClient(Communication.buildMessage(Command.RESPONSE, new String[]{"true"}));
                    }
            }
        }
    }

    /**
     * Gets a message from the client
     *
     * @return client message
     */
    public String receiveFromClient() throws IOException {
        StringBuilder sb = new StringBuilder();
        String message;

        while ((message = inputStream.readLine()) != null && !message.isEmpty()) {
            sb.append(message);
        }

        return sb.toString();
    }

    /**
     * Sends a message to the client
     *
     * @param text message
     * @return true if successful
     */
    public boolean sendToClient(String text) {
        if (clientSocket.isClosed()) {
            return false;
        }

        outputStream.println(text);
        return true;
    }

    /**
     * Disconnects this client
     */
    public void disconnect() {
        sendToClient(buildMessage(Command.SHUTDOWN, null));
        try {
            clientSocket.close();
        } catch (IOException e) {
        }
    }
}