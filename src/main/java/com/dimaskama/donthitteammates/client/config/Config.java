package com.dimaskama.donthitteammates.client.config;

import com.dimaskama.donthitteammates.client.DHTMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

public abstract class Config {
    private final transient String CONFPATH;

    public Config(String path) {
        CONFPATH = path;
    }

    public void loadOrCreate() {
        File file = new File(CONFPATH);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!(parent.exists() || parent.mkdirs()))
                DHTMod.LOGGER.error("Can't create config: " + parent.getAbsolutePath());
            try {
                saveJsonWithoutCatch();
            } catch (IOException e) {
                DHTMod.LOGGER.error("Exception occurred while writing new config. " + e);
            }
        } else {
            try (FileReader f = new FileReader(CONFPATH)) {
                Config c = new Gson().fromJson(f, getClass());
                for (Field field : getClass().getDeclaredFields()) field.set(this, field.get(c));
            } catch (IOException | IllegalAccessException e) {
                DHTMod.LOGGER.error("Exception occurred while reading config. " + e);
            }
        }
    }

    public void saveJson() {
        try {
            saveJsonWithoutCatch();
        } catch (IOException e) {
            DHTMod.LOGGER.error("Exception occurred while saving config. " + e);
        }
    }

    public void saveJsonWithoutCatch() throws IOException {
        try (FileWriter w = new FileWriter(CONFPATH)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, w);
            DHTMod.LOGGER.info("Config saved: " + CONFPATH);
        }
    }

    public void reset() {
        try {
            Config n = getClass().getConstructor(String.class).newInstance(CONFPATH);
            for (Field field : getClass().getDeclaredFields()) field.set(this, field.get(n));
        } catch (Exception e) {
            DHTMod.LOGGER.error("Exception occurred while resetting config. " + e);
        }
    }
}
