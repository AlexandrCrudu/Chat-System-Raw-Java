package Server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static int SERVER_PORT = 1338;
    ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }


    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        Server server = new Server(serverSocket);
        server.init();
    }

    public void init() {
        System.out.println("Server running on port " + SERVER_PORT + " ... ");
        try {
            while(!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
//                System.out.println("Clients:");
//                for (ClientHandler handler: ClientHandler.handlers) {
//                    System.out.println(handler.getClientUsername());
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
