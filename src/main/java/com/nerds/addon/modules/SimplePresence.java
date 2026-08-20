package com.nerds.addon.modules;

import com.nerds.addon.NerdAddon;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class SimplePresence extends Module {

    private static final String APP_ID = "1539644928261623860";
    private final RichPresence presence = new RichPresence();

    public SimplePresence() {

        super(NerdAddon.CATEGORY, "simple-presence", "Custom Numbani Discord RPC.");
    }

    @Override
    public void onActivate() {
        // 1. Initialize Meteor's built-in Discord IPC
        DiscordIPC.start(Long.parseLong(APP_ID), null);

        // 2. Set up your exact Rich Presence variables
        presence.setState("Playing minecraft with meteor client and nerd addon");
        presence.setLargeImage("icon_1_", "Numbani");



        presence.setStart(System.currentTimeMillis() / 1000);

        // 3. Send the updates to Discord
        DiscordIPC.setActivity(presence);
    }

    @Override
    public void onDeactivate() {

        DiscordIPC.stop();
    }
}
// uh no this is not vibecoded -LaggyZaggy
