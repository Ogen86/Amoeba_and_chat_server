package server;

public class Server_Main {
    public static void main(String[] args) {
        int port = 10001;
        Server server = new Server(port);
        server.start();
    }
}
