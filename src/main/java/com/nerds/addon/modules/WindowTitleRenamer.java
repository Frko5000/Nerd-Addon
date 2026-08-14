package com.nerds.addon.modules;

import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.Icons;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class WindowTitleRenamer extends Module {
    private static final String ICON = "assets/nerd-addon/icon.png";

    private final Setting<String> prefix = settings.getDefaultGroup().add(new StringSetting.Builder()
        .name("prefix").description("Window title prefix.").defaultValue("Nerd Addon")
        .onChanged(value -> updateTitle()).build()
    );

    private final Setting<Boolean> customIcon = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("custom-icon").description("Use the Nerd Addon window icon.").defaultValue(true)
        .onChanged(value -> { if (isActive()) mc.execute(() -> setIcon(value)); }).build()
    );

    public WindowTitleRenamer() {
        super(NerdAddon.CATEGORY, "window-title-renamer", "Customizes your minecraft window title and icon ");
    }

    @Override public void onActivate() {
        setIcon(customIcon.get());
        updateTitle();
    }

    @Override public void onDeactivate() {
        setIcon(false);
        mc.updateWindowTitle();
    }

    private void updateTitle() {
        if (isActive()) mc.updateWindowTitle();
    }

    public String getTitle() {
        String name = prefix.get().strip();
        if (name.isEmpty()) name = "Minecraft";
        String context = getContext();
        return context.isEmpty() ? name : name + " | " + context;
    }

    private String getContext() {
        if (mc.player != null) {
            if (mc.getServer() != null) return "Playing " + mc.getServer().getSaveProperties().getLevelName();
            ServerInfo server = mc.getCurrentServerEntry();
            return server == null || server.name.isBlank() ? "Multiplayer" : server.name;
        }

        Screen screen = mc.currentScreen;
        if (screen == null || screen instanceof TitleScreen) return "Main Menu";
        if (screen instanceof SelectWorldScreen) return "Selecting World";
        if (screen instanceof CreateWorldScreen) return "Creating World";
        if (screen instanceof EditWorldScreen) return "Editing World";
        if (screen instanceof MultiplayerScreen) return "Multiplayer";
        String title = screen.getTitle().getString();
        return title.isEmpty() ? "Menu" : title;
    }

    private void setIcon(boolean custom) {
        if (mc.getWindow() == null) return;
        if (!custom) {
            try {
                mc.getWindow().setIcon(mc.getDefaultResourcePack(), SharedConstants.isDevelopment ? Icons.SNAPSHOT : Icons.RELEASE);
            } catch (IOException e) {
                MeteorClient.LOG.warn("Could not restore the vanilla window icon", e);
            }
            return;
        }

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(ICON); MemoryStack stack = MemoryStack.stackPush()) {
            if (stream == null) throw new IOException("Icon not found: " + ICON);
            byte[] bytes = stream.readAllBytes();
            ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
            ByteBuffer pixels;
            IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1), channels = stack.mallocInt(1);
            try {
                encoded.put(bytes).flip();
                pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
            } finally {
                MemoryUtil.memFree(encoded);
            }
            if (pixels == null) throw new IOException(STBImage.stbi_failure_reason());

            GLFWImage.Buffer image = GLFWImage.malloc(1);
            try {
                image.width(width.get(0)).height(height.get(0)).pixels(pixels);
                GLFW.glfwSetWindowIcon(mc.getWindow().getHandle(), image);
            } finally {
                image.free();
                STBImage.stbi_image_free(pixels);
            }
        } catch (IOException e) {
            MeteorClient.LOG.warn("Could not set the custom window icon", e);
        }
    }
}
