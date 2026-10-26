package com.scientificcalculator.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.scientificcalculator.gui.CalculatorScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

/** Comandos de cliente: se ejecutan localmente y nunca se envían al servidor. */
public final class CalcCommand {
    private CalcCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        Command<FabricClientCommandSource> open = ctx -> {
            MinecraftClient mc = ctx.getSource().getClient();
            // Se abre en el siguiente tick: el chat cierra su propia pantalla justo después de ejecutar el comando.
            mc.send(() -> mc.setScreen(new CalculatorScreen()));
            return 1;
        };
        dispatcher.register(ClientCommandManager.literal("calc").executes(open));
        dispatcher.register(ClientCommandManager.literal("calculator").executes(open));
    }
}
