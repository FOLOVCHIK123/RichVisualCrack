package rich.screens.clickgui.impl.autobuy.items.price;

import java.util.HashMap;
import java.util.Map;

public class Defaultpricec {
    private static final Map<String, Integer> defaultPrices = new HashMap<>();

    static {
    }

    public static int getPrice(String displayName) {
        return defaultPrices.getOrDefault(displayName, 1000);
    }
}