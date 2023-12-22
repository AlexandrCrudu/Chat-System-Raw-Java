package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int SERVER_PORT = 1337;
    private static final int SERVER_PORT_FILE_TRANSFER = 1338;
    private final ServerSocket serverSocket;
    private final ServerSocket fileSocket;

    public Server(ServerSocket serverSocket, ServerSocket fileSocket) {
        this.serverSocket = serverSocket;
        this.fileSocket = fileSocket;
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        ServerSocket fileSocket = new ServerSocket(SERVER_PORT_FILE_TRANSFER);
        Server server = new Server(serverSocket, fileSocket);
        new Thread(server::init).start();
        new Thread(server::initFileTransfer).start();
    }

    private void initFileTransfer() {
        try{
            while(!fileSocket.isClosed()) {
                Socket clientFileSocket = fileSocket.accept();
                TransferManager fileHandler = new TransferManager(clientFileSocket);
                Thread newFileThread = new Thread(fileHandler);
                newFileThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        System.out.println("Server running on port " + SERVER_PORT + " ... ");
        try {
            while(!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
