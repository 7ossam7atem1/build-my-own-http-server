import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from my program will appear here:");

        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            serverSocket.setReuseAddress(true);

            serverSocket.accept();
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
