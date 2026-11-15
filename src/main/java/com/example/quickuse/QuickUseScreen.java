package com.example.quickuse;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class QuickUseScreen extends Screen {
    private static final int ROW_H = 24;
    private static final int W_ITEM = 124;
    private static final int W_HAND = 40;
    private static final int W_KEY = 80;
    private static final int W_NAME = 100;
    private static final int W_DEL = 20;
    private static final int GAP = 4;
    private static final int TOTAL_W = W_ITEM + W_HAND + W_KEY + W_NAME + W_DEL + GAP * 4;
    private static final int TOP = 48;

    private int scroll = 0;
    private int visibleRows = 1;
    private UseEntry listening = null;

    public QuickUseScreen() {
        super(Text.literal("QuickUse"));
    }

    @Override
    protected void init() {
        visibleRows = Math.max(1, (height - TOP - 40) / ROW_H);
        int maxScroll = Math.max(0, Config.entries.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int left = (width - TOTAL_W) / 2;

        for (int r = 0; r < visibleRows; r++) {
            int idx = scroll + r;
            if (idx >= Config.entries.size()) break;
            UseEntry e = Config.entries.get(idx);
            int y = TOP + r * ROW_H;
            int x = left;

            // Предмет
            TextFieldWidget itemField = new TextFieldWidget(textRenderer, x, y, W_ITEM, 20, Text.literal("Предмет"));
            itemField.setMaxLength(100);
            itemField.setPlaceholder(Text.literal("minecraft:ender_pearl").formatted(Formatting.DARK_GRAY));
            itemField.setText(e.item);
            itemField.setEditableColor(colorFor(e));
            itemField.setChangedListener(s -> {
                e.item = s;
                itemField.setEditableColor(colorFor(e));
            });
            addDrawableChild(itemField);
            x += W_ITEM + GAP;

            // Взять из руки
            addDrawableChild(ButtonWidget.builder(Text.literal("Рука"), b -> {
                if (client != null && client.player != null) {
                    ItemStack st = client.player.getMainHandStack();
                    if (!st.isEmpty()) {
                        e.item = Registries.ITEM.getId(st.getItem()).toString();
                        clearAndInit();
                    }
                }
            }).dimensions(x, y, W_HAND, 20)
                    .tooltip(Tooltip.of(Text.literal("Подставить предмет из главной руки")))
                    .build());
            x += W_HAND + GAP;

            // Клавиша
            Text keyLabel = (listening == e)
                    ? Text.literal("> нажмите <").formatted(Formatting.YELLOW)
                    : e.keyText();
            addDrawableChild(ButtonWidget.builder(keyLabel, b -> {
                listening = e;
                clearAndInit();
            }).dimensions(x, y, W_KEY, 20)
                    .tooltip(Tooltip.of(Text.literal("Нажмите, затем клавишу или кнопку мыши. ESC — убрать.")))
                    .build());
            x += W_KEY + GAP;

            // Своё название
            TextFieldWidget nameField = new TextFieldWidget(textRenderer, x, y, W_NAME, 20, Text.literal("Название"));
            nameField.setMaxLength(48);
            nameField.setPlaceholder(Text.literal("Своё название").formatted(Formatting.DARK_GRAY));
            nameField.setText(e.name);
            nameField.setChangedListener(s -> e.name = s);
            addDrawableChild(nameField);
            x += W_NAME + GAP;

            // Удалить
            addDrawableChild(ButtonWidget.builder(Text.literal("X").formatted(Formatting.RED), b -> {
                Config.entries.remove(e);
                if (listening == e) listening = null;
                clearAndInit();
            }).dimensions(x, y, W_DEL, 20).build());
        }

        int by = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Добавить"), b -> {
            Config.entries.add(new UseEntry());
            scroll = Integer.MAX_VALUE; // прокрутить вниз, init() ограничит
            clearAndInit();
        }).dimensions(width / 2 - 104, by, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(width / 2 + 4, by, 100, 20).build());
    }

    private static int colorFor(UseEntry e) {
        boolean ok = e.item == null || e.item.isBlank() || e.resolveItem() != null;
        return ok ? 0xE0E0E0 : 0xFF5555;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("QuickUse — быстрое использование предметов"), width / 2, 8, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Колёсико — прокрутка, ESC — закрыть и сохранить").formatted(Formatting.GRAY), width / 2, 20, 0xAAAAAA);

        int left = (width - TOTAL_W) / 2;
        int hy = TOP - 11;
        ctx.drawTextWithShadow(textRenderer, "Предмет (id)", left, hy, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Клавиша", left + W_ITEM + GAP + W_HAND + GAP, hy, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Название в чате", left + W_ITEM + W_HAND + W_KEY + GAP * 3, hy, 0xAAAAAA);

        if (Config.entries.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Список пуст — нажмите «+ Добавить»").formatted(Formatting.GRAY), width / 2, TOP + 20, 0xAAAAAA);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening.setKey(InputUtil.UNKNOWN_KEY);
            } else {
                listening.setKey(InputUtil.fromKeyCode(keyCode, scanCode));
            }
            listening = null;
            clearAndInit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listening != null) {
            listening.setKey(InputUtil.Type.MOUSE.createFromCode(button));
            listening = null;
            clearAndInit();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, Config.entries.size() - visibleRows);
        int ns = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(verticalAmount)));
        if (ns != scroll) {
            scroll = ns;
            clearAndInit();
        }
        return true;
    }

    @Override
    public void removed() {
        Config.save();
    }
}
