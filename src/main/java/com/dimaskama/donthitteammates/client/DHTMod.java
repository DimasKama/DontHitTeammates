package com.dimaskama.donthitteammates.client;

import com.dimaskama.donthitteammates.client.config.DHTConfig;
import com.dimaskama.donthitteammates.client.screen.DHTScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class DHTMod implements ClientModInitializer {
    public static final String MOD_ID = "donthitteammates";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.donthitteammates.toggle",
            GLFW.GLFW_KEY_KP_2,
            MOD_ID
    ));
    public static final KeyBinding MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.donthitteammates.menu",
            GLFW.GLFW_KEY_KP_3,
            MOD_ID
    ));
    public static final DHTConfig CONFIG = new DHTConfig("config/donthitteammates.json");

    @Override
    public void onInitializeClient() {
        CONFIG.loadOrCreate();
        if (!CONFIG.save_enabled_state) CONFIG.enabled = false;

        ClientCommandRegistrationCallback.EVENT.register(new ModCommand());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.wasPressed()) {
                toggle(client);
            }
            while (MENU_KEY.wasPressed()) {
                Screen screen = client.currentScreen;
                if (!(screen instanceof DHTScreen)) {
                    client.setScreen(new DHTScreen(screen));
                }
            }
        });
    }

    public static void toggle(MinecraftClient client) {
        CONFIG.enabled = !CONFIG.enabled;
        Text message = CONFIG.enabled ? Text.translatable("message.donthitteammates.enabled") : Text.empty();
        client.inGameHud.setOverlayMessage(message, false);
    }

    public static boolean shouldProtect(OtherClientPlayerEntity player) {
        return CONFIG.teammates.stream().anyMatch(teammate -> teammate.name.equalsIgnoreCase(player.getGameProfile().getName()) && teammate.enabled);
    }

    private static final class ModCommand implements ClientCommandRegistrationCallback {
        @Override
        public void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
            dispatcher.register(literal(MOD_ID)
                    .executes(ModCommand::toggle)
                    .then(literal("toggle")
                            .executes(ModCommand::toggle))
                    .then(literal("config")
                            .then(literal("save_enabled_state")
                                    .then(argument("value", BoolArgumentType.bool())
                                            .executes(context -> {
                                                CONFIG.save_enabled_state = BoolArgumentType.getBool(context, "value");
                                                context.getSource().sendFeedback(Text.translatable(
                                                        "message.donthitteammates.set_config_value",
                                                        "save_enabled_state",
                                                        Boolean.toString(CONFIG.enabled)
                                                ));
                                                CONFIG.saveJson();
                                                return 0;
                                            })))
                            .then(literal("save_teammates_list")
                                    .then(argument("value", BoolArgumentType.bool())
                                            .executes(context -> {
                                                CONFIG.save_teammates_list = BoolArgumentType.getBool(context, "value");
                                                context.getSource().sendFeedback(Text.translatable(
                                                        "message.donthitteammates.set_config_value",
                                                        "save_teammates_list",
                                                        Boolean.toString(CONFIG.save_teammates_list)
                                                ));
                                                CONFIG.saveJson();
                                                return 0;
                                            })))));
        }

        private static int toggle(CommandContext<FabricClientCommandSource> context) {
            DHTMod.toggle(MinecraftClient.getInstance());
            return 0;
        }
    }
}
