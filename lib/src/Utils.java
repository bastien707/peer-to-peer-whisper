import java.util.HashMap;

public class  Utils {
    public static String hashMapToString(HashMap<String, Integer> nodes) {
        StringBuilder message = new StringBuilder();
        for (String name : nodes.keySet()) {
            message.append(name).append("=").append(nodes.get(name)).append(",");
        }
        return message.toString();
    }
}
