import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiTask {
    private static final String BASE = "https://jsonplaceholder.typicode.com";
    private static final Pattern OBJECT = Pattern.compile("\\{[^{}]*\\}");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    static String request(String method, String path, String json) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) URI.create(BASE + path).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        if (json != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (var output = connection.getOutputStream()) {
                output.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = connection.getResponseCode();
        if (method.equals("DELETE")) {
            connection.disconnect();
            return String.valueOf(status);
        }
        try (var input = status >= 400
                ? connection.getErrorStream()
                : connection.getInputStream()) {
            String response = input == null
                    ? ""
                    : new String(input.readAllBytes(), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + ": " + response);
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }
    // Завдання 1
    static String createUser(String json) throws IOException {
        return request("POST", "/users", json);
    }
    static String updateUser(int id, String json) throws IOException {
        return request("PUT", "/users/" + id, json);
    }
    static int deleteUser(int id) throws IOException {
        return Integer.parseInt(request("DELETE", "/users/" + id, null));
    }
    static String getUsers() throws IOException {
        return request("GET", "/users", null);
    }
    static String getUser(int id) throws IOException {
        return request("GET", "/users/" + id, null);
    }
    static String getUserByUsername(String username) throws IOException {
        String value = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return request("GET", "/users?username=" + value, null);
    }
    // Завдання 2
    static int maxId(String json) {
        Matcher matcher = ID.matcher(json);
        int max = 0;
        while (matcher.find()) {
            max = Math.max(max, Integer.parseInt(matcher.group(1)));
        }
        return max;
    }
    static void saveLastPostComments(int userId) throws IOException {
        String posts = request("GET", "/users/" + userId + "/posts", null);
        int postId = maxId(posts);
        if (postId == 0) {
            System.out.println("У користувача немає постів");
            return;
        }
        String comments = request("GET", "/posts/" + postId + "/comments", null);
        Path file = Path.of(
                "user-" + userId + "-post-" + postId + "-comments.json"
        );
        Files.writeString(file, comments, StandardCharsets.UTF_8);
        System.out.println(comments);
        System.out.println("Збережено: " + file);
    }
    // Завдання 3
    static String openTodos(String json) {

        Matcher matcher = OBJECT.matcher(json);
        StringBuilder result = new StringBuilder("[");
        Pattern notCompleted = Pattern.compile("\"completed\"\\s*:\\s*false\\b");
        while (matcher.find()) {
            String todo = matcher.group();
            if (notCompleted.matcher(todo).find()) {
                if (result.length() > 1) {
                    result.append(",");
                }
                result.append(todo);
            }
        }
        return result.append("]").toString();
    }
    static void printOpenTodos(int userId) throws IOException {
        String todos = request("GET", "/users/" + userId + "/todos", null);
        System.out.println(openTodos(todos));
    }
    public static void main(String[] args) throws IOException {
        String user = "{\"name\":\"Test User\",\"username\":\"test_user\","
                + "\"email\":\"test@example.com\"}";
        String updated = "{\"id\":1,\"name\":\"Updated User\","
                + "\"username\":\"Bret\",\"email\":\"test@example.com\"}";
        System.out.println("Створено: " + createUser(user));
        System.out.println("Оновлено: " + updateUser(1, updated));
        System.out.println("Видалено, статус: " + deleteUser(1));
        System.out.println("Всі користувачі: " + getUsers());
        System.out.println("Користувач за id: " + getUser(1));
        System.out.println("Користувач за username: " + getUserByUsername("Bret"));
        saveLastPostComments(1);
        System.out.println("Відкриті задачі:");
        printOpenTodos(1);
    }
}
