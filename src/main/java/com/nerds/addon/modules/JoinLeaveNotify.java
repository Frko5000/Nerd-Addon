package com.nerds.addon.modules;

import com.mojang.authlib.GameProfile;
import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

public class JoinLeaveNotify extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<NotifyMode> mode = sgGeneral.add(new EnumSetting.Builder<NotifyMode>()
        .name("notify-mode")
        .description("Which notification style to use.")
        .defaultValue(NotifyMode.Classic)
        .build()
    );

    private final Setting<List<String>> players = sgGeneral.add(new StringListSetting.Builder()
        .name("ignore-list")
        .description("Players to ignore (name contains).")
        .defaultValue(List.of())
        .build()
    );

    private boolean skipJoinListPacket = true;

    public JoinLeaveNotify() {
        super(NerdAddon.CATEGORY, "join-leave-notify", "Notifies you when players join or leave the game.");
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        skipJoinListPacket = true;
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        skipJoinListPacket = true;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        switch (event.packet) {
            case PlayerListS2CPacket packet -> {
                if (!packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) return;
                if (skipJoinListPacket) {
                    skipJoinListPacket = false;
                    return;
                }

                for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
                    if (entry.profile() == null) continue;

                    String name = entry.profile().name();
                    if (shouldIgnore(name)) continue;
                    notify(name, true);
                }
            }
            case PlayerRemoveS2CPacket packet -> {
                if (mc.getNetworkHandler() == null) return;

                for (UUID id : packet.profileIds()) {
                    PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(id);
                    if (entry == null || entry.getProfile() == null) continue;

                    String name = entry.getProfile().name();
                    if (shouldIgnore(name)) continue;
                    notify(name, false);
                }
            }
            default -> {}
        }
    }

    private boolean shouldIgnore(String name) {
        if (name == null || name.isBlank()) return true;

        for (String ignored : players.get()) {
            if (name.contains(ignored)) return true;
        }

        return false;
    }

    private void notify(String name, boolean joined) {
        switch (mode.get()) {
            case Classic -> notifyClassic(name, joined);
            case Modern -> notifyModern(name, joined);
        }
    }

    private void notifyClassic(String name, boolean joined) {
        Text text = Text.literal("\u00a7e" + name + (joined ? " joined the game." : " left the game."));

        mc.execute(() -> {
            if (mc.player != null) mc.player.sendMessage(text, false);
        });
    }

    private void notifyModern(String name, boolean joined) {
        MutableText text = Text.literal("[")
            .formatted(Formatting.GRAY)
            .append(Text.literal(joined ? "+" : "-").formatted(joined ? Formatting.GREEN : Formatting.RED))
            .append(Text.literal("] " + name).formatted(Formatting.GRAY));

        mc.execute(() -> {
            if (mc.player != null) mc.player.sendMessage(text, false);
        });
    }

    public enum NotifyMode {
        Classic,
        Modern
    }
}