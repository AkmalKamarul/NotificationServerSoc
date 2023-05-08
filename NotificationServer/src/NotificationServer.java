import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NotificationServer {
    private static List<Socket> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(8080)) {
            System.out.println("Server started");

            while (true) {
                Socket client = server.accept();
                clients.add(client);
                System.out.println("CLIENT CONNECTED");
                Thread t = new Thread(new ClientHandler(client));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket client;
        private BufferedReader in;

        public ClientHandler(Socket socket) throws IOException {
            this.client = socket;
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        }

        @Override
        public void run() {
            try (Socket socket = client; BufferedReader reader = in) {
                String inputLine;

                while ((inputLine = reader.readLine()) != null) {
                    System.out.println("Received message: " + inputLine);
                    sendToClient(client, inputLine);

                    if (inputLine.contains("New User Registered") && inputLine.contains(":")) {
                        // extract the user name from the message
                        String name = inputLine.split(":")[1].trim();

                        // send push notification using cURL to the client who sent the message
                        String token = "ExponentPushToken[sA_2YyCb2ef62eP-T36OZm]";

                        // send push notification with the title and message
                        sendPushNotification(token, "New User Registered",
                                "A new user, " + name + ", has registered on our platform.");
                    } // UPDATE EVENT
                    else if (inputLine.contains("User Transaction Complete") && inputLine.contains(":")) {
                        // extract the user name from the message
                        String name = inputLine.split(":")[1].trim();

                        // send push notification using cURL to the client who sent the message
                        String token = "ExponentPushToken[sA_2YyCb2ef62eP-T36OZm]";

                        // send push notification with the title and message
                        sendPushNotification(token, "Update Payment", name + " payment has been completed.");
                    }

                }

            } catch (IOException e) {
                System.out.println("Client disconnected");
            } finally {
                try {
                    client.close(); // close the connection explicitly
                } catch (IOException e) {
                    // ignore error on close
                }
                clients.remove(client);
            }
        }

        private void sendToClient(Socket client, String message) throws IOException {
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            out.println(message);
        }

        private static void sendPushNotification(String token, String title, String message) throws IOException {
            // prepare the push notification message with the title and message
            String json = String.format(
                    "{\"to\": \"%s\", \"sound\": \"default\", \"title\": \"%s\", \"body\": \"%s\"}",
                    token, title, message);

            URL url = new URL("https://exp.host/--/api/v2/push/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code : " + responseCode);
        }

    }

}
