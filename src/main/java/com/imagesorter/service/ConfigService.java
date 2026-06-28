package com.imagesorter.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.imagesorter.model.ConfigSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing application configuration
 * Handles loading and saving configuration settings to JSON file
 */
public class ConfigService {

    private static final String OLD_CONFIG_DIR = System.getProperty("user.dir");
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".imagesorterpro";
    private static final String DEFAULT_CONFIG_FILE = "image_sort_config.json";
    private static final String POINTER_FILE = "config_pointer.txt";
    private static final String DEFAULT_CONFIG_PATH = CONFIG_DIR + File.separator + DEFAULT_CONFIG_FILE;
    private static final String POINTER_PATH = CONFIG_DIR + File.separator + POINTER_FILE;

    private static ConfigService instance;
    private ConfigSettings config;
    private ObjectMapper objectMapper;
    private String configPath;

    private ConfigService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Migrate configuration from old working directory if it exists
        migrateOldConfig();

        resolveConfigPath();

        this.config = new ConfigSettings();

        // Ensure new config directory exists
        createConfigDirectory();
    }

    private void resolveConfigPath() {
        File pointerFile = new File(POINTER_PATH);
        if (pointerFile.exists()) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(pointerFile.toPath());
                String customPath = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!customPath.isEmpty()) {
                    configPath = customPath;
                    return;
                }
            } catch (IOException e) {
                System.err.println("Failed to read config pointer: " + e.getMessage());
            }
        }
        configPath = DEFAULT_CONFIG_PATH;
    }

    public void setConfigPath(String newPath) {
        if (newPath == null || newPath.trim().isEmpty()) {
            return;
        }
        
        File newFile = new File(newPath);
        File parent = newFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        File pointerFile = new File(POINTER_PATH);
        try {
            java.nio.file.Files.write(pointerFile.toPath(), newPath.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to write config pointer: " + e.getMessage());
        }
        
        this.configPath = newPath;
        saveConfig();
    }

    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    /**
     * Loads configuration from file
     */
    public void loadConfig() {
        File configFile = new File(configPath);
        System.out.println("Config Path: "+configPath);
        if (configFile.exists()) {
            try {
                config = objectMapper.readValue(configFile, ConfigSettings.class);
                System.out.println("Configuration loaded from: " + configPath);
            } catch (IOException e) {
                System.err.println("Failed to load configuration: " + e.getMessage());
                System.err.println("Using default configuration.");
                config = new ConfigSettings();
            }
        } else {
            System.out.println("Configuration file not found. Using defaults.");
            config = new ConfigSettings();
        }
    }

    /**
     * Loads a configuration from a specific path without setting it as active
     */
    public ConfigSettings loadConfigFromPath(String path) {
        File configFile = new File(path);
        if (configFile.exists()) {
            try {
                return objectMapper.readValue(configFile, ConfigSettings.class);
            } catch (IOException e) {
                System.err.println("Failed to load configuration from path: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Saves configuration to file
     */
    public void saveConfig() {
        try {
            File configFile = new File(configPath);
            objectMapper.writeValue(configFile, config);
            System.out.println("Configuration saved to: " + configPath);
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * Gets the current configuration
     */
    public ConfigSettings getConfig() {
        return config;
    }

    /**
     * Sets a new configuration (typically used for testing)
     */
    public void setConfig(ConfigSettings config) {
        this.config = config != null ? config : new ConfigSettings();
    }

    /**
     * Resets configuration to defaults
     */
    public void resetToDefaults() {
        config = new ConfigSettings();
        saveConfig();
    }

    /**
     * Gets the configuration file path
     */
    public String getConfigPath() {
        return configPath;
    }

    /**
     * Checks if configuration file exists
     */
    public boolean configFileExists() {
        return new File(configPath).exists();
    }

    /**
     * Creates the configuration directory if it doesn't exist
     */
    private void createConfigDirectory() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            if (configDir.mkdirs()) {
                System.out.println("Created configuration directory: " + CONFIG_DIR);
            } else {
                System.err.println("Failed to create configuration directory: " + CONFIG_DIR);
            }
        }
    }

    /**
     * Migrates configuration file and pointer file from the old user.dir location
     * to the new user.home/.imagesorterpro location.
     */
    private void migrateOldConfig() {
        File oldConfig = new File(OLD_CONFIG_DIR + File.separator + DEFAULT_CONFIG_FILE);
        File oldPointer = new File(OLD_CONFIG_DIR + File.separator + POINTER_FILE);
        File newDir = new File(CONFIG_DIR);

        // Migrate pointer file first if old exists but new doesn't
        if (oldPointer.exists()) {
            File newPointer = new File(POINTER_PATH);
            if (!newPointer.exists()) {
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }
                try {
                    java.nio.file.Files.copy(oldPointer.toPath(), newPointer.toPath());
                    System.out.println("Migrated configuration pointer from " + oldPointer.getAbsolutePath() + " to " + newPointer.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Failed to migrate configuration pointer: " + e.getMessage());
                }
            }
        }

        // Migrate main configuration file if old exists but new doesn't
        if (oldConfig.exists()) {
            File newConfig = new File(DEFAULT_CONFIG_PATH);
            if (!newConfig.exists()) {
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }
                try {
                    java.nio.file.Files.copy(oldConfig.toPath(), newConfig.toPath());
                    System.out.println("Migrated configuration file from " + oldConfig.getAbsolutePath() + " to " + newConfig.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Failed to migrate configuration file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Backs up current configuration file
     */
    public boolean backupConfig() {
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            return false;
        }

        String backupPath = configPath + ".backup." + System.currentTimeMillis();
        File backupFile = new File(backupPath);

        try {
            java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath());
            System.out.println("Configuration backed up to: " + backupPath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to backup configuration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates the current configuration
     */
    public boolean validateConfig() {
        if (config == null) {
            return false;
        }

        List<String> hotkeys = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            hotkeys.add(String.valueOf(i));
        }
        for (char c = 'a'; c <= 'z'; c++) {
            hotkeys.add(String.valueOf(c));
        }

        // Check if at least one folder is configured
        for (String hotkey : hotkeys) {
            String folderPath = config.getFolderPath(hotkey);
            if (folderPath != null && !folderPath.trim().isEmpty()) {
                File folder = new File(folderPath);
                if (!folder.exists() || !folder.isDirectory()) {
                    System.err.println("Configured folder does not exist: " + folderPath);
                    return false;
                }
            }
        }

        return true;
    }


}
