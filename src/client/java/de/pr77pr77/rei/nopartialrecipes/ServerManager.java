package de.pr77pr77.rei.nopartialrecipes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

        ServerSetting(String serverAddress) {
            this.serverAddress = Objects.requireNonNullElse(serverAddress, "none");
            recipeDataIDs = new ArrayList<>();
            recipeDataIDs.add("minecraft");
        }
    }

    public ServerManager() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        this.filePath = configDir.resolve(MOD_ID + ".json");
        load();
        cleanup();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> cleanup());
    }

    public void addCurrentServer() {
        if (getCurrentServerRecipeDataIDs().isEmpty()) {
            data.servers.add(new ServerSetting());
            saveAsync();
        }
    }

    public void setServer(String serverAddress, boolean enable) {
        if (enable && getServerRecipeDataIDs(serverAddress).isEmpty()) {
            data.servers.add(new ServerSetting(serverAddress));
            saveAsync();
            REIPlugin.instance.registerDisplays(DisplayRegistry.getInstance());
        } else if (!enable && getServerRecipeDataIDs(serverAddress).contains("minecraft")) {
            data.servers.removeIf(s -> s.serverAddress.equals(serverAddress));
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

    public ArrayList<String> getServerRecipeDataIDs(String serverAddress) {
        if (serverAddress == null) {
            return new ArrayList<>();
        }
        ServerSetting serverSetting = data.servers.stream()
                .filter(s -> serverAddress.equals(s.serverAddress))
                .findFirst()
                .orElse(null);
        if (serverSetting == null) {
            return new ArrayList<>();
        }
        return serverSetting.recipeDataIDs;
    }

    public void cleanup() {
        ServerList list = new ServerList(MinecraftClient.getInstance());
        list.loadFile();

        List<ServerInfo> infos = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            infos.add(list.get(i));
        }

        boolean deleted = false;
        List<ServerSetting> tempServerSettingList = new ArrayList<>(data.servers); // Needed, because looped list can't be modified.
        for (ServerSetting serverSetting : tempServerSettingList) {
            if (infos.stream()
                    .noneMatch(s -> s.address.equals(serverSetting.serverAddress))) {
                data.servers.remove(serverSetting);
                deleted = true;
            }
        }

        if (deleted) {
            saveAsync();
        }
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