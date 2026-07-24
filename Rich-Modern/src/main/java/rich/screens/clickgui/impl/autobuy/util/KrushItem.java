package rich.screens.clickgui.impl.autobuy.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuyItemSettings;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;

public class KrushItem implements AutoBuyableItem {
    private final String displayName;
    private final Item material;
    private final ItemStack displayStack;
    private final int defaultPrice;
    private final AutoBuyItemSettings settings;
    private final boolean isKrushItem;
    private boolean enabled;

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice) {
        this(displayName, material, displayStack, defaultPrice, true, false);
    }

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice, boolean canHaveQuantity) {
        this(displayName, material, displayStack, defaultPrice, canHaveQuantity, false);
    }

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice, boolean canHaveQuantity, boolean isKrushItem) {
        this.displayName = displayName;
        this.material = material;
        this.displayStack = displayStack;
        this.defaultPrice = defaultPrice;
        this.isKrushItem = isKrushItem;
        this.settings = new AutoBuyItemSettings(defaultPrice, material, displayName, canHaveQuantity);
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        if (config.hasItemConfig(displayName)) {
            this.enabled = config.isItemEnabled(displayName);
        } else {
            this.enabled = true;
            config.loadItemSettings(displayName, defaultPrice);
        }
    }

    private boolean shouldHaveGlint() {
        if (!isKrushItem) {
            return false;
        }
        return material == Items.TOTEM_OF_UNDYING;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack copy = displayStack.copy();
        if (shouldHaveGlint()) {
            copy.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return copy;
    }

    @Override
    public int getPrice() {
        return settings.getBuyBelow();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public AutoBuyItemSettings getSettings() {
        return settings;
    }
}