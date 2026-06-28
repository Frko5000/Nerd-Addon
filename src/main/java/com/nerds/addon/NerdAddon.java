package com.nerds.addon;

import com.nerds.addon.commands.CommandExample;
import com.nerds.addon.hud.HudExample;
import com.nerds.addon.modules.JoinLeaveNotify;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class NerdAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Nerd Addon");
    public static final HudGroup HUD_GROUP = new HudGroup("Nerd Addon");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Nerd Addon");

        // Modules
        Modules.get().add(new JoinLeaveNotify());

        // Commands
        Commands.add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.nerds.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Frko5000", "nerd-addon");
    }
}