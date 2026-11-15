package com.example.quickuse;

import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Одна запись: предмет + клавиша + своё название. */
public class UseEntry {
    public String item = "";
    public String key = "key.keyboard.unknown";
    public String name = "";

    public transient boolean wasDown = false;

    /** Находит предмет по id (можно писать без "minecraft:"). */
    public Item resolveItem() {
        String s = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        if (!s.contains(":")) s = "minecraft:" + s;
        Identifier id = Identifier.tryParse(s);
        if (id == null) return null;
        for (Item i : Registries.ITEM) {
            if (Registries.ITEM.getId(i).equals(id)) {
                return i == Items.AIR ? null : i;
            }
        }
        return null;
    }

    public InputUtil.Key getKey() {
        try {
            return InputUtil.fromTranslationKey(key);
        } catch (Exception e) {
            return InputUtil.UNKNOWN_KEY;
        }
    }

    public void setKey(InputUtil.Key k) {
        this.key = k.getTranslationKey();
    }

    public Text keyText() {
        InputUtil.Key k = getKey();
        if (k.getCode() == -1) return Text.literal("Не задана");
        return k.getLocalizedText();
    }

    public boolean isDown(long window) {
        InputUtil.Key k = getKey();
        if (k.getCode() == -1) return false;
        if (k.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, k.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(window, k.getCode());
    }

    /** Своё название, если задано, иначе обычное название предмета. */
    public String displayName(Item it) {
        if (name != null && !name.isBlank()) return name.trim();
        return it.getName().getString();
    }
}
