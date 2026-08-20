package com.nerds.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class seeinv extends Command {
    public seeinv() {
        super("seeinv", "Sees inv of someone");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerArgumentType.create())
            .executes(context -> {
                PlayerEntity player = PlayerArgumentType.get(context);

                if (player != null) {
                    ItemStack mainhand = player.getMainHandStack();
                    ItemStack offhand = player.getOffHandStack();

                    info("Mainhand: %s", mainhand.getName().getString());
                    info("Offhand: %s", offhand.getName().getString());
                }

                return SINGLE_SUCCESS;
            })
        );
    }
}
