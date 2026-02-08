package com.example.possessor.game;

import net.minecraftforge.fml.loading.FMLPaths;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config-possessor.yml";
    private static boolean debug = false;

    public static void load() {
        File configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE).toFile();
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
                debug = Boolean.parseBoolean(props.getProperty("debug", "false"));
            } catch (IOException e) {
                System.err.println("[PossessorMod] Failed to load config: " + e.getMessage());
            }
        } else {
            // Create default
            debug = false;
            save();
        }
    }

    public static void save() {
        File configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE).toFile();
        Properties props = new Properties();
        props.setProperty("debug", String.valueOf(debug));

        try (FileWriter writer = new FileWriter(configFile)) {
            // Write a simple YML-like format (Properties is key=value but works for simple bools)
            writer.write("# Possessor Mod Configuration\n");
            writer.write("debug: " + debug + "\n");
        } catch (IOException e) {
            System.err.println("[PossessorMod] Failed to save config: " + e.getMessage());
        }
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean value) {
        debug = value;
        save();
    }
}
