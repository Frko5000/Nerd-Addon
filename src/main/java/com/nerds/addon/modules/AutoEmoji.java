// skidded by ___hj from skid hack

package com.nerds.addon.modules;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AutoEmoji extends Module {
    private static final Map<String, String> EMOJIS = new HashMap<>();

    public AutoEmoji() {
        super(NerdAddon.CATEGORY, "auto-emoji", "adds discord emojis like :sob: and other ones to format in chat");
        loadEmojiMappings();
    }

    private void loadEmojiMappings() {
        EMOJIS.clear();


        EMOJIS.put(":)", "☺");
        EMOJIS.put(":(", "☹");
        EMOJIS.put("<3", "❤");
        EMOJIS.put(":D", "😀");
        EMOJIS.put(";)", "😉");
        EMOJIS.put("B)", "😎");
        EMOJIS.put(":/", "😕");
        EMOJIS.put(":O", "😮");
        EMOJIS.put("xD", "😆");


        try {

            InputStream stream = MinecraftClient.getInstance()
                .getResourceManager()
                .getResource(net.minecraft.util.Identifier.of("nerd-addon", "emojis.json"))
                .get()
                .getInputStream();

            if (stream != null) {
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

                Map<String, String> jsonMappings = new Gson().fromJson(
                    reader,
                    new TypeToken<Map<String, String>>(){}.getType()
                );

                if (jsonMappings != null) {
                    for (Map.Entry<String, String> entry : jsonMappings.entrySet()) {
                        String emojiValue = entry.getValue();

                        if (emojiValue != null) {
                            emojiValue = emojiValue.replace("\uFE0F", "").replace("\u200D", "");
                        }


                        EMOJIS.put(entry.getKey(), emojiValue);
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            warning("Failed to parse or sanitize your custom auto-emoji asset JSON: " + e.getMessage());
        }
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        String message = event.message;

        for (Map.Entry<String, String> entry : EMOJIS.entrySet()) {
            if (message.contains(entry.getKey())) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }


        if (message.contains("\uFE0F")) {
            message = message.replace("\uFE0F", "");
        }
        if (message.contains("\u200D")) {
            message = message.replace("\u200D", "");
        }


        if (!message.equals(event.message)) {
            event.message = message;
        }
    }
}
