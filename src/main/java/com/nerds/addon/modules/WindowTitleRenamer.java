package com.nerds.addon.modules;

import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerInfo;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class WindowTitleRenamer extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> prefixSetting = sgGeneral.add(new StringSetting.Builder()
        .name("prefix")
        .description("The custom prefix for the window title.")
        .defaultValue("Nerd Addon")
        .onChanged(s -> updateTitle())
        .build()
    );

    private final Setting<Boolean> customIconSetting = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-icon")
        .description("enable nerd icon")
        .defaultValue(true)
        .onChanged(enabled -> {
            if (enabled && isActive()) {
                setCustomIcon("assets/nerd-addon/icon.png");
            }
        })
        .build()
    );

    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder()
        .name("update-delay")
        .description("How often to refresh the window title (in milliseconds).")
        .defaultValue(100)
        .min(50)
        .sliderMax(1000)
        .onChanged(v -> restartTimer())
        .build()
    );

    private Timer updateTimer;

    public WindowTitleRenamer() {
        super(NerdAddon.CATEGORY, "window-title-renamer", "renames window and icon");
    }

    @Override
    public void onActivate() {
        if (customIconSetting.get()) {
            setCustomIcon("assets/nerd-addon/icon.png");
        }
        updateTitle();
        restartTimer();
    }

    @Override
    public void onDeactivate() {
        stopTimer();
        if (mc.getWindow() != null) {
            GLFW.glfwSetWindowTitle(mc.getWindow().getHandle(), "Minecraft");
        }
    }

    private void restartTimer() {
        stopTimer();
        if (!isActive()) return;
        updateTimer = new Timer("WindowTitleRenamer-Timer", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mc.execute(() -> updateTitle());
            }
        }, 0, delaySetting.get());
    }

    private void stopTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        updateTitle();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        updateTitle();
    }

    private void updateTitle() {
        if (!isActive()) return;
        String context = getContext();
        String title = context.isEmpty() ? prefixSetting.get() : prefixSetting.get() + " | " + context;
        applyTitleNow(title);
    }

    private void applyTitleNow(String title) {
        if (mc.getWindow() != null) {
            GLFW.glfwSetWindowTitle(mc.getWindow().getHandle(), title);
        }
    }

    private String getContext() {
        if (mc.player == null) {
            Screen screen = mc.currentScreen;
            if (screen instanceof TitleScreen) return "Main Menu";
            if (screen instanceof SelectWorldScreen) return "Selecting World";
            if (screen instanceof CreateWorldScreen) return "Creating World";
            if (screen instanceof EditWorldScreen) return "Editing World";
            if (screen instanceof MultiplayerScreen) return "Multiplayer";
            if (screen != null) {
                String screenTitle = screen.getTitle().getString();
                return screenTitle.isEmpty() ? "Menu" : screenTitle;
            }
            return "Main Menu";
        }

        if (mc.getServer() != null) {
            return "Playing " + mc.getServer().getSaveProperties().getLevelName();
        } else {
            ServerInfo server = mc.getCurrentServerEntry();
            return server != null ? server.name : "Multiplayer";
        }
    }

    private void setCustomIcon(String resourcePath) {
        if (mc.getWindow() == null) return;
        long window = mc.getWindow().getHandle();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer imageBuffer = null;
            try {
                InputStream stream = WindowTitleRenamer.class.getClassLoader().getResourceAsStream(resourcePath);
                if (stream == null) {
                    MeteorClient.LOG.error("Window icon not found: {}", resourcePath);
                    return;
                }

                byte[] bytes;
                try {
                    bytes = stream.readAllBytes();
                } catch (IOException e) {
                    MeteorClient.LOG.error("Failed to read icon file: {}", resourcePath, e);
                    return;
                }

                ByteBuffer raw = MemoryUtil.memAlloc(bytes.length);
                raw.put(bytes).flip();

                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                imageBuffer = STBImage.stbi_load_from_memory(raw, w, h, comp, 4);
                MemoryUtil.memFree(raw);
                if (imageBuffer == null) {
                    MeteorClient.LOG.error("counld tload ts icon: {}", STBImage.stbi_failure_reason());
                    return;
                }

                GLFWImage.Buffer gimg = GLFWImage.malloc(1);
                gimg.position(0).width(w.get(0)).height(h.get(0)).pixels(imageBuffer);
                GLFW.glfwSetWindowIcon(window, gimg);
                gimg.free();
            } finally {
                if (imageBuffer != null) MemoryUtil.memFree(imageBuffer);
            }
        }
    }
}
