import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static Map<String, Integer> accounts = new HashMap<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        try {
            // Reading server IP and Port number from serverinfo.dat
            BufferedReader configReader = new BufferedReader(new FileReader("serverinfo.dat"));
            String serverIP = configReader.readLine();  // Server IP from first line
            int serverPort = Integer.parseInt(configReader.readLine());  // Port number from second line

            // Initializing account information
            accounts.put("Kim", 20);
            accounts.put("Lee", 50);
            accounts.put("Park", 30);
            accounts.put("Choi", 80);
            accounts.put("Jung", 100);

            // Create server socket by using serverinfo.dat information
            ServerSocket serverSocket = new ServerSocket(serverPort, 50, InetAddress.getByName(serverIP));
            System.out.println("Server started on IP: " + serverIP + " and Port: " + serverPort);

            // define variable to name the clients by thread
            int threadCounter = 1;
            // Allow client connection
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Server print accept message
                System.out.println("Client connected...");
                // Handling client requests in a new thread
                ClientHandler handler = new ClientHandler(clientSocket);
                 // Define Thread name (by granting increasing number)
                handler.setName("Client #" + threadCounter++);
                handler.start();
            }
            // if error, print stacktrace
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handling client connection
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out; // Output stream to send data to the client
        private BufferedReader in; // Input stream to receive data from the client

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            // Initialize the PrintWriter to send messages to the client
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
             // Initialize the BufferedReader to receive messages from the client
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String message; // Holds the Client message
                // Continuously read client input until disconnection or null
                while ((message = in.readLine()) != null) {
                    // Client request acceptance log
                    String[] command = message.split(" ");
                    String response; // Storing the Server response
                    // Extract the used account, command type, and target account from the command
                    String accountUsed = command.length > 1 ? command[1] : "N/A";
                    String methodUsed = command[0];
                    String targetAccount = (command.length > 2) ? command[2] : "N/A"; // for TRANSFER, if not, N/A

                    // Print client thread name
                    System.out.println("Thread " + Thread.currentThread().getName() + " : " + accountUsed + " " + methodUsed + " " + targetAccount);

                     // Determine which command the client sent and process accordingly
                    switch (command[0]) {
                        case "CHECK":
                            response = checkBalance(command[1]);
                            break;
                        case "DEPOSIT":
                            response = deposit(command[1], Integer.parseInt(command[2]));
                            break;
                        case "WITHDRAW":
                            response = withdraw(command[1], Integer.parseInt(command[2]));
                            break;
                        case "TRANSFER":
                            response = transfer(command[1], command[2], Integer.parseInt(command[3]));
                            break;
                        default:
                            // Add log for invalid requests
                            System.out.println("Thread " + Thread.currentThread().getName() + " : Error - Invalid Request");
                            response = "Error: Invalid command!";
                    }
                    // send result to client
                    out.println(response);
                }
            } catch (IOException e) {
                // Handle any IOExceptions that occur during communication
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                     // Log the disconnection of the client along with the thread name
                    System.out.println("Client disconnected from thread " + Thread.currentThread().getName());
                } catch (IOException e) {
                     // If an error occurs while closing the socket, log the error
                    System.err.println("Error closing socket in thread " + Thread.currentThread().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // check current Balance
        private String checkBalance(String accountName) {
            // Synchronize the access to the shared accounts map to ensure thread safety
            synchronized (lock) {
                // Check if the requested account exists in the accounts map
                if (accounts.containsKey(accountName)) {
                    // Return the current balance of the specified account
                    return accountName + " has $" + accounts.get(accountName);
                } else { // If the account doesn't exist
                    System.out.println("Thread " + Thread.currentThread().getName() + " : Error - Account '" + accountName + "' not found");
                    return "Error: Account not found!";
                }
            }
        }

        // put deposit
        private String deposit(String accountName, int amount) {
            // Synchronize the access to the shared accounts map to ensure thread safety
            synchronized (lock) {
                // Check if the account exists in the accounts map
                if (accounts.containsKey(accountName)) {
                    // Add the specified amount to the current balance of the account
                    accounts.put(accountName, accounts.get(accountName) + amount);
                    // Return a message with the updated balance
                    return accountName + " now has $" + accounts.get(accountName);
                } else {
                    System.out.println("Thread " + Thread.currentThread().getName() + " : Error - Account '" + accountName + "' not found");
                    return "Error: Account not found!";
                }
            }
        }

        // withdraw from current account
        private String withdraw(String accountName, int amount) {
            // Synchronize the access to the shared accounts map to ensure thread safety
            synchronized (lock) {
                // Add the specified amount to the current balance of the account
                if (accounts.containsKey(accountName)) {
                    // Check if the account has sufficient funds to withdraw the specified amount
                    if (accounts.get(accountName) >= amount) {
                        // Subtract the amount from the current balance of the account
                        accounts.put(accountName, accounts.get(accountName) - amount);
                        return accountName + " now has $" + accounts.get(accountName);
                    } else { // Balance is less than your planned withdrawal amount
                        System.out.println("Thread " + Thread.currentThread().getName() + " : Error - Insufficient funds for account '" + accountName + "'");
                        return "Error: Insufficient funds!";
                    }
                } else { // If the account doesn't exist
                System.out.println("Thread " + Thread.currentThread().getName() + " : Error - Account '" + accountName + "' not found");
                    return "Error: Account not found!";
                }
            }
        }

        // transfer my account to others account
        private String transfer(String fromAccount, String toAccount, int amount) {
            synchronized (lock) {
                // Check if both the source (fromAccount) and target (toAccount) accounts exist
                if (accounts.containsKey(fromAccount) && accounts.containsKey(toAccount)) {
                    // If sources havd sufficient funds, transfer the money
                    if (accounts.get(fromAccount) >= amount) {
                        accounts.put(fromAccount, accounts.get(fromAccount) - amount);
                        accounts.put(toAccount, accounts.get(toAccount) + amount);
                        return "Transferred $" + amount + " from " + fromAccount + " to " + toAccount;
                    } else { // Balance is less than your planned withdrawal amount
                        System.out.println("Thread " + Thread.currentThread().getName() + " : Error - Insufficient funds for account '" + fromAccount + "'");
                        return "Error: Insufficient funds!";
                    }
                } else { // One or more of the two accounts being accessed is not found
                    System.out.println("Thread " + Thread.currentThread().getName() + " : Error - One or both accounts not found for transfer");
                    return "Error: One or both accounts not found!";
                }
            }
        }
    }
}
