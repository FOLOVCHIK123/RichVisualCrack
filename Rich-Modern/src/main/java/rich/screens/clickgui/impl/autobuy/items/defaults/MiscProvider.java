package rich.screens.clickgui.impl.autobuy.items.defaults;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.price.Defaultpricec;
import rich.screens.clickgui.impl.autobuy.util.KrushItem;

import java.util.ArrayList;
import java.util.List;

public class MiscProvider {
    public static List<AutoBuyableItem> getMisc() {
        List<AutoBuyableItem> misc = new ArrayList<>();
        misc.add(new KrushItem("Золотое яблоко", Items.GOLDEN_APPLE, new ItemStack(Items.GOLDEN_APPLE), Defaultpricec.getPrice("Золотое яблоко")));
        return misc;
    }
}