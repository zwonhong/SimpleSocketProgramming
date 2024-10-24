import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try {
            // Reading server IP and port from serverinfo.dat
            BufferedReader configReader = new BufferedReader(new FileReader("serverinfo.dat"));
            String serverIP = configReader.readLine();
            int serverPort = Integer.parseInt(configReader.readLine());
            configReader.close();

            // Connecting to the server using the IP and port from the file
            Socket socket = new Socket(serverIP, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            String userInput; // variablt to store the client request
            System.out.println("Enter command (CHECK/DEPOSIT/WITHDRAW/TRANSFER) or 'exit' to quit:");

            // get client reqeust repeatedly while no 'exit'
            while ((userInput = stdIn.readLine()) != null) {
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }

                // Sending user input to the server
                out.println(userInput);
                // Receiving and printing the response from the server
                String response = in.readLine();
                System.out.println("Server: " + response);
            }
            // Socket close when connecting finished
            socket.close();
        } catch (IOException e) {
            // if Exception, print stack trace to debug the problem
            e.printStackTrace();
        }
    }
}
