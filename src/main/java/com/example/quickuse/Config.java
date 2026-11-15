package com.example.quickuse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static List<UseEntry> entries = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class Data {
        List<UseEntry> entries = new ArrayList<>();
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("quickuse.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Data d = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Data.class);
                if (d != null && d.entries != null) entries = d.entries;
            }
        } catch (Exception e) {
            System.err.println("[QuickUse] Не удалось загрузить конфиг: " + e);
        }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.entries = entries;
            Files.writeString(path(), GSON.toJson(d), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[QuickUse] Не удалось сохранить конфиг: " + e);
        }
    }
}
