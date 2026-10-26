package com.scientificcalculator;

import com.scientificcalculator.command.CalcCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/** Punto de entrada (solo cliente). Registra /calc y /calculator. */
public class ScientificCalculatorClient implements ClientModInitializer {
    public static final String MOD_ID = "scientificcalculator";

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> CalcCommand.register(dispatcher));
    }
}
