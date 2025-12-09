import java.net.http.*;
import java.net.*;
import java.io.*;
import java.time.Duration;

public class HttpLoginTest {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://127.0.0.1:8080/api/auth/login";
        String payload = args.length > 1 ? args[1] : "{\"username\":\"admin\",\"password\":\"wrongpassword\"}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        System.out.println("Sending POST to: " + url);
        System.out.println("Payload: " + payload);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("--- RESPONSE ---");
        System.out.println("Status: " + response.statusCode());
        System.out.println("Headers: " + response.headers().map());
        System.out.println("Body:\n" + response.body());
    }
}
