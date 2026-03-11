package de.pr77pr77.rei.nopartialrecipes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipes.MOD_ID;

public class ServerManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path filePath;
    public ServerSettings data;

    public static class ServerSettings {
        ArrayList<ServerSetting> servers = new ArrayList<>();
    }

    public static class ServerSetting {
        String serverAddress;
        ArrayList<String> recipeDataIDs;

        ServerSetting() {
            ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();
            if (server != null) {
                serverAddress = server.address;
            } else {
                serverAddress = "none";
            }
            recipeDataIDs = new ArrayList<>();
            recipeDataIDs.add("minecraft");
        }
    }

    public ServerManager() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        this.filePath = configDir.resolve(MOD_ID + ".json");
        load();
    }

    public void addCurrentServer() {
        if (getCurrentServerRecipeDataIDs() == null) {
            data.servers.add(new ServerSetting());
            saveAsync();
        }
    }

    public ArrayList<String> getCurrentServerRecipeDataIDs() {
        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();
        if (server == null) {
            return new ArrayList<>();
        }
        ServerSetting serverSetting = data.servers.stream()
                .filter(s -> server.address.equals(s.serverAddress))
                .findFirst()
                .orElse(null);
        if (serverSetting == null) {
            return new ArrayList<>();
        }
        return serverSetting.recipeDataIDs;
    }

    private void load() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                this.data = GSON.fromJson(json, ServerSettings.class);
                if (this.data == null) this.data = new ServerSettings();
            } else {
                this.data = new ServerSettings();
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.data = new ServerSettings();
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(filePath.getParent());
            String json = GSON.toJson(this.data);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void saveAsync() {
        ServerSettings snapshot;
        synchronized (this) {
            snapshot = new ServerSettings();
            snapshot.servers = new ArrayList<>(this.data.servers);
        }
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(filePath.getParent());
                String json = GSON.toJson(snapshot);
                Files.writeString(filePath, json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}