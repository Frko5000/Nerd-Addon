package com.nerds.addon.modules;

import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

public class WindowTitleRenamer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> title = sgGeneral.add(new StringSetting.Builder()
        .name("title")
        .description("The custom window title to display.")
        .defaultValue("Nerd Addon")
        .build()
    );

    public WindowTitleRenamer() {
        super(NerdAddon.CATEGORY, "window-title-renamer", "Lets you set a custom window title.");
    }

    @Override
    public void onActivate() {
        applyTitle();
    }

    @Override
    public void onDeactivate() {
        GLFW.glfwSetWindowTitle(mc.getWindow().getHandle(), "Minecraft");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        applyTitle();
    }

    private void applyTitle() {
        GLFW.glfwSetWindowTitle(mc.getWindow().getHandle(), title.get());
    }
}