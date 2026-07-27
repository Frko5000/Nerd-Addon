package com.nerds.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Coords extends Command {

    private static final SuggestionProvider<CommandSource> PLAYER_SUGGESTIONS = (context, builder) -> {
        if (mc.world == null) return builder.buildFuture();

        String remaining = builder.getRemaining().toLowerCase();

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            String name = player.getGameProfile().name();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    };

    public Coords() {
        super("coords", "copies your (or another player's) coordinates to your clipboard.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.player == null) return SINGLE_SUCCESS;
            copyCoords(mc.player.getGameProfile().name(), mc.player.getX(), mc.player.getY(), mc.player.getZ());
            return SINGLE_SUCCESS;
        });

        builder.then(argument("player", StringArgumentType.word())
            .suggests(PLAYER_SUGGESTIONS)
            .executes(context -> {
                if (mc.world == null) return SINGLE_SUCCESS;

                String targetName = StringArgumentType.getString(context, "player");
                AbstractClientPlayerEntity target = null;

                for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                    if (player.getGameProfile().name().equalsIgnoreCase(targetName)) {
                        target = player;
                        break;
                    }
                }

                if (target == null) {
                    error("player \"" + targetName + "\" not found.");
                    return SINGLE_SUCCESS;
                }

                copyCoords(target.getGameProfile().name(), target.getX(), target.getY(), target.getZ());
                return SINGLE_SUCCESS;
            })
        );
    }

    private void copyCoords(String name, double x, double y, double z) {
        String text = (int) Math.floor(x) + " " + (int) Math.floor(y) + " " + (int) Math.floor(z);
        mc.keyboard.setClipboard(text);
        boolean isSelf = mc.player != null && name.equals(mc.player.getGameProfile().name());
        info("successfully copied " + (isSelf ? "your" : name + "'s") + " coordinates: \n" + text);
    }
}