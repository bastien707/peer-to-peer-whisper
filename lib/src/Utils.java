import java.util.Arrays;
import java.util.HashMap;

public class  Utils {
    public static String hashMapToString(HashMap<String, Integer> nodes) {
        StringBuilder message = new StringBuilder();
        for (String name : nodes.keySet()) {
            message.append(name).append("=").append(nodes.get(name)).append(",");
        }
        return message.toString();
    }

    public static HashMap<String, Integer> stringToHashMap(String message) {
        if (message.length() != 0) {
            String substring = message.substring(1, message.length() - 1); // remove { and }
            HashMap<String, Integer> deps = new HashMap<>();
            String[] parts = substring.split(", ");
            for (String part : parts) {
                String[] keyValue = part.split("=");
                deps.put(keyValue[0], Integer.parseInt(keyValue[1]));
            }
            return deps;
        } else {
            return new HashMap<>();
        }
    }
}
