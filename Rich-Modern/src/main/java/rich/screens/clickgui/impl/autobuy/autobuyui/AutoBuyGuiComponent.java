package rich.screens.clickgui.impl.autobuy.autobuyui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.ItemRegistry;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;
import rich.util.render.item.ItemRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AutoBuyGuiComponent implements IMinecraft {
    private static final int FORCED_GUI_SCALE = 2;

    private float x, y, width, height;
    private float targetScroll = 0f;
    private float smoothScroll = 0f;

    private float slideOffsetX = 0f;
    private float targetSlideOffsetX = 0f;
    private boolean slidingOut = false;
    private static final float SLIDE_SPEED = 20f;

    private final Map<AutoBuyableItem, Float> toggleAnimations = new HashMap<>();
    private final Map<AutoBuyableItem, Float> hoverAnimations = new HashMap<>();
    private final Map<AutoBuyableItem, Float> enabledAnimations = new HashMap<>();

    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    private AutoBuyableItem hoveredItem = null;
    private AutoBuyableItem editingItem = null;
    private EditField editingField = EditField.NONE;
    private String inputText = "";
    private float cursorBlink = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private float panelAlpha = 1f;
    private float currentScale = 1f;

    private static final float ITEM_HEIGHT = 22f;
    private static final float ITEM_SPACING = 3f;
    private static final float CATEGORY_HEIGHT = 18f;
    private static final float ANIM_SPEED = 11f;

    private static final String PRICE_LABEL = "Цена покупки: ";
    private static final String QUANTITY_LABEL = "Покупать от: ";

    private final List<PendingIcon> pendingIcons = new ArrayList<>();
    private final List<PendingContextIcon> pendingContextIcons = new ArrayList<>();

    private static class PendingIcon {
        ItemStack stack;
        float x, y;

        PendingIcon(ItemStack stack, float x, float y) {
            this.stack = stack;
            this.x = x;
            this.y = y;
        }
    }

    private static class PendingContextIcon {
        ItemStack stack;
        float x, y;
        float scale;

        PendingContextIcon(ItemStack stack, float x, float y, float scale) {
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }

    public enum EditField {
        NONE, PRICE, QUANTITY
    }

    public AutoBuyGuiComponent() {
    }

    private int getCurrentGuiScale() {
        int scale = mc.options.getGuiScale().getValue();
        if (scale == 0) {
            scale = mc.getWindow().calculateScaleFactor(0, mc.forcesUnicodeFont());
        }
        return scale;
    }

    private float getScaleFactor() {
        return (float) getCurrentGuiScale() / (float) FORCED_GUI_SCALE;
    }

    public void position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void size(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setAlpha(float alpha) {
        this.panelAlpha = alpha;
    }

    public void startSlideOut() {
        slidingOut = true;
        targetSlideOffsetX = -(width + 100);
    }

    public void startSlideIn() {
        slidingOut = false;
        targetSlideOffsetX = 0f;
    }

    public boolean isSlideComplete() {
        return Math.abs(slideOffsetX - targetSlideOffsetX) < 5f;
    }

    public boolean isSlidOut() {
        return slidingOut && isSlideComplete();
    }

    public void resetSlide() {
        slideOffsetX = 0f;
        targetSlideOffsetX = 0f;
        slidingOut = false;
    }

    public void setSlideInstant(float offset) {
        slideOffsetX = offset;
        targetSlideOffsetX = offset;
    }

    public boolean isEditing() {
        return editingItem != null && editingField != EditField.NONE;
    }

    private boolean isHovered(double mx, double my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    private int clampAlpha(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
    }

    private float easeInOutCubic(float x) {
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2 * x + 2, 3) / 2f;
    }

    private float calculateSlideAlpha() {
        if (slideOffsetX >= 0) return 1f;
        float maxOffset = width + 100;
        float progress = Math.abs(slideOffsetX) / maxOffset;
        progress = Math.max(0f, Math.min(1f, progress));
        return 1f - easeOutCubic(progress);
    }

    public void render(DrawContext context, float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        this.panelAlpha = alphaMultiplier;
        this.currentScale = 1f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        float slideDiff = targetSlideOffsetX - slideOffsetX;
        if (Math.abs(slideDiff) > 0.5f) {
            float progress = 1f - (Math.abs(slideDiff) / (width + 100));
            float easedSpeed = SLIDE_SPEED * (0.5f + 0.5f * easeOutCubic(progress));
            slideOffsetX += slideDiff * easedSpeed * deltaTime;
        } else {
            slideOffsetX = targetSlideOffsetX;
        }

        float slideAlpha = calculateSlideAlpha();

        updateAnimations(deltaTime);

        cursorBlink += deltaTime * 2f;
        if (cursorBlink > 1f) cursorBlink -= 1f;

        hoveredItem = null;
        pendingIcons.clear();
        pendingContextIcons.clear();

        float contentHeight = calculateContentHeight();
        float maxScroll = Math.max(0, contentHeight - height + 10f);
        targetScroll = clamp(targetScroll, -maxScroll, 0);

        float diff = targetScroll - smoothScroll;
        smoothScroll += diff * 0.3f;
        if (Math.abs(diff) < 0.1f) {
            smoothScroll = targetScroll;
        }

        renderPanelBackground(alphaMultiplier, slideAlpha);

        float clipX = x + 3;
        float clipY = y + 1;
        float clipW = width - 6;
        float clipH = height - 3;

        Scissor.enable(clipX, clipY, clipW, clipH, FORCED_GUI_SCALE);

        float contentOffsetX = slideOffsetX;
        float contentAlpha = alphaMultiplier * slideAlpha;

        // ===== ПУСТАЯ ПАНЕЛЬ =====
        // Здесь ничего не рисуется, все предметы скрыты
        // ==========================

        for (PendingIcon icon : pendingIcons) {
            ItemRender.drawItem(icon.stack, icon.x, icon.y, 1.0f, 1.0f);
        }
        pendingIcons.clear();

        Scissor.disable();

        float scaleFactor = getScaleFactor();

        int scissorX1 = (int) (clipX * scaleFactor);
        int scissorY1 = (int) (clipY * scaleFactor);
        int scissorX2 = (int) ((clipX + clipW) * scaleFactor);
        int scissorY2 = (int) ((clipY + clipH) * scaleFactor);

        context.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        for (PendingContextIcon icon : pendingContextIcons) {
            drawItemWithScaleCompensation(context, icon.stack, icon.x, icon.y, icon.scale, 1.0f, scaleFactor);
        }
        pendingContextIcons.clear();

        context.disableScissor();
    }

    private void drawItemWithScaleCompensation(DrawContext context, ItemStack stack, float x, float y, float scale, float alpha, float scaleFactor) {
        if (stack.isEmpty() || alpha <= 0.01f) return;

        float size = 16 * scale;
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();

        matrices.translate(centerX, centerY);
        matrices.scale(scale * scaleFactor, scale * scaleFactor);
        matrices.translate(-8, -8);

        context.drawItem(stack, 0, 0);

        matrices.popMatrix();
    }

    private boolean isInView(float itemY, float itemHeight, float clipY, float clipH) {
        float itemBottom = itemY + itemHeight;
        float clipBottom = clipY + clipH;
        return itemBottom > clipY && itemY < clipBottom;
    }

    private void updateAnimations(float deltaTime) {
        for (AutoBuyableItem item : ItemRegistry.getAllItems()) {
            float targetToggle = item.isEnabled() ? 1f : 0f;
            float currentToggle = toggleAnimations.getOrDefault(item, item.isEnabled() ? 1f : 0f);
            float newToggle = smoothLerp(currentToggle, targetToggle, ANIM_SPEED * deltaTime);
            toggleAnimations.put(item, newToggle);

            float targetEnabled = item.isEnabled() ? 1f : 0f;
            float currentEnabled = enabledAnimations.getOrDefault(item, item.isEnabled() ? 1f : 0f);
            float newEnabled = smoothLerp(currentEnabled, targetEnabled, ANIM_SPEED * deltaTime);
            enabledAnimations.put(item, newEnabled);

            boolean isHov = item == hoveredItem;
            float targetHover = isHov ? 1f : 0f;
            float currentHover = hoverAnimations.getOrDefault(item, 0f);
            hoverAnimations.put(item, smoothLerp(currentHover, targetHover, ANIM_SPEED * deltaTime));
        }
    }

    private float smoothLerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * clamp(speed, 0f, 1f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float calculateContentHeight() {
        // Возвращаем минимальную высоту, так как контент пустой
        return 50f;
    }

    private void renderPanelBackground(float alphaMultiplier, float slideAlpha) {
        float bgSlideAlpha = slidingOut ? slideAlpha : 1f;
        int bgAlpha = clampAlpha((int) (15 * alphaMultiplier * bgSlideAlpha));
        int outlineAlpha = clampAlpha((int) (215 * alphaMultiplier * bgSlideAlpha));

        if (bgAlpha > 0) {
            Render2D.rect(x, y, width, height, new Color(64, 64, 64, bgAlpha).getRGB(), 7f);
            Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 7f);
        }
    }

    private void renderCategoryHeader(float catX, float catY, float catWidth, String name, float contentAlpha) {
        // Метод оставлен пустым, так как категории не отображаются
    }

    private void renderItem(DrawContext context, AutoBuyableItem item, float itemX, float itemY, float itemW,
                            float mouseX, float mouseY, float alphaMultiplier, float slideAlpha) {
        // Метод оставлен пустым, так как предметы не отображаются
    }

    private void queueItemIcon(AutoBuyableItem item, float iconX, float iconY, float iconSize) {
        // Метод оставлен пустым, так как иконки не отображаются
    }

    private void renderToggle(float tx, float ty, float tw, float th, float anim, float enabledAnim, float contentAlpha) {
        // Метод оставлен пустым, так как переключатели не отображаются
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY, float panelW, float panelH) {
        if (slidingOut) return false;

        if (!isHovered(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            if (isEditing()) applyEdit();
            return false;
        }

        if (button != 0) {
            if (isEditing()) applyEdit();
            return true;
        }

        // Вся логика кликов по предметам удалена, так как их нет
        if (isEditing()) applyEdit();
        return true;
    }

    private void startEditing(AutoBuyableItem item, EditField field) {
        editingItem = item;
        editingField = field;
        cursorBlink = 0f;

        if (field == EditField.PRICE) {
            inputText = String.valueOf(item.getSettings().getBuyBelow());
        } else if (field == EditField.QUANTITY) {
            inputText = String.valueOf(item.getSettings().getMinQuantity());
        }
    }

    private void applyEdit() {
        if (editingItem == null || editingField == EditField.NONE) return;

        try {
            int value = Integer.parseInt(inputText);

            if (editingField == EditField.PRICE) {
                editingItem.getSettings().setBuyBelow(Math.max(1, value));
            } else if (editingField == EditField.QUANTITY) {
                editingItem.getSettings().setMinQuantity(Math.max(1, Math.min(64, value)));
            }

            ItemRegistry.saveItemSettings(editingItem);

        } catch (NumberFormatException ignored) {}

        editingItem = null;
        editingField = EditField.NONE;
        inputText = "";
    }

    private void cancelEdit() {
        editingItem = null;
        editingField = EditField.NONE;
        inputText = "";
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEditing()) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            applyEdit();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelEdit();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !inputText.isEmpty()) {
            inputText = inputText.substring(0, inputText.length() - 1);
            return true;
        }

        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!isEditing()) return false;

        if (Character.isDigit(chr)) {
            int maxLen = editingField == EditField.PRICE ? 9 : 2;
            if (inputText.length() < maxLen) {
                inputText += chr;
            }
            return true;
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float panelX, float panelY, float panelW, float panelH) {
        if (slidingOut) return false;

        if (isHovered(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            targetScroll += (float) amount * 25f;
            return true;
        }
        return false;
    }

    public void resetHover() {
        hoveredItem = null;
    }

    public void resetPositions() {
        smoothScroll = targetScroll;
    }

    private List<CategoryItems> getCategorizedItems() {
        // Возвращаем пустой список, так как предметы не отображаются
        return new ArrayList<>();
    }

    private static class CategoryItems {
        String name;
        List<AutoBuyableItem> items;

        CategoryItems(String name, List<AutoBuyableItem> items) {
            this.name = name;
            this.items = items != null ? items : new ArrayList<>();
        }
    }
}