package com.imagesorter.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.imagesorter.model.ConfigSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Service for managing application configuration
 * Handles loading and saving configuration settings to JSON file
 */
public class ConfigService {

    private static final String CONFIG_DIR = System.getProperty("user.home") +
            File.separator + ".imagesorter";
    private static final String CONFIG_FILE = "config.json";
    private static final String CONFIG_PATH = CONFIG_DIR + File.separator + CONFIG_FILE;

    private static ConfigService instance;
    private ConfigSettings config;
    private ObjectMapper objectMapper;

    private ConfigService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.config = new ConfigSettings();

        // Ensure config directory exists
        createConfigDirectory();
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
        File configFile = new File(CONFIG_PATH);

        if (configFile.exists()) {
            try {
                config = objectMapper.readValue(configFile, ConfigSettings.class);
                System.out.println("Configuration loaded from: " + CONFIG_PATH);
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
     * Saves configuration to file
     */
    public void saveConfig() {
        try {
            File configFile = new File(CONFIG_PATH);
            objectMapper.writeValue(configFile, config);
            System.out.println("Configuration saved to: " + CONFIG_PATH);
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
        return CONFIG_PATH;
    }

    /**
     * Checks if configuration file exists
     */
    public boolean configFileExists() {
        return new File(CONFIG_PATH).exists();
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
     * Backs up current configuration file
     */
    public boolean backupConfig() {
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            return false;
        }

        String backupPath = CONFIG_PATH + ".backup." + System.currentTimeMillis();
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

        // Check if at least one folder is configured
        for (int i = 1; i <= 9; i++) {
            String folderPath = config.getFolderPath(i);
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