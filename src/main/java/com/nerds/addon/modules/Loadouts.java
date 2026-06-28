// skidded by ___hj from skid hack
// credits to startdust addon https://github.com/0xTas/stardust


package com.nerds.addon.modules;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import oshi.util.tuples.Pair;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Loadouts extends Module {
    public Loadouts() {
        super(NerdAddon.CATEGORY, "Loadouts", "add button in inv to move item where you saved mastersigma");
    }

    public static final String LOADOUTS_FILE = "meteor-client/loadouts.json";

    public final Setting<Boolean> quickLoadout = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("quick-loadout-buttons")
            .description("Adds quicksave loadout buttons to the inventory screen.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> tickRateSetting = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("speed")
            .range(1, 50)
            .sliderRange(1, 20)
            .defaultValue(3)
            .build()
    );

    private int ticks = 0;
    public boolean isSorted = true;
    private boolean doubleTap = false;
    private String activeLoadoutKey = "quicksave";
    private final ArrayDeque<Pair<Integer, Integer>> jobs = new ArrayDeque<>();
    private final HashMap<String, HashMap<Integer, Item>> loadouts = new HashMap<>();

    private boolean checkOrCreateFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }
            return true;
        } catch (IOException e) {
            System.err.println("[Loadouts] Failed to create file " + path + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onActivate() {
        loadLoadoutsFromFile();
    }

    @Override
    public void onDeactivate() {
        ticks = 0;
        jobs.clear();
        isSorted = true;
        doubleTap = false;
        saveLoadoutsToFile();
        activeLoadoutKey = "quicksave";
    }

    public void clearLoadouts() {
        loadouts.clear();
        saveLoadoutsToFile();
    }

    public void deleteLoadout(String name) {
        loadouts.remove(name);
        saveLoadoutsToFile();
    }

    public boolean noLoadout(String name) {
        return !loadouts.containsKey(name);
    }

    private void loadLoadoutsFromFile() {
        if (!checkOrCreateFile(LOADOUTS_FILE)) {
            System.err.println("[Loadouts] Could not access loadouts file for loading.");
            return;
        }

        Gson gson = new Gson();
        try (Reader reader = new FileReader(LOADOUTS_FILE)) {
            java.lang.reflect.Type type = new TypeToken<HashMap<String, HashMap<Integer, String>>>() {}.getType();
            HashMap<String, HashMap<Integer, String>> loaded = gson.fromJson(reader, type);

            loadouts.clear();
            if (loaded == null) return;

            for (Map.Entry<String, HashMap<Integer, String>> entry : loaded.entrySet()) {
                HashMap<Integer, Item> itemMap = new HashMap<>();
                for (Map.Entry<Integer, String> itemId : entry.getValue().entrySet()) {
                    itemMap.put(itemId.getKey(), Registries.ITEM.get(Identifier.of(itemId.getValue())));
                }
                loadouts.put(entry.getKey(), itemMap);
            }
        } catch (Exception err) {
            System.err.println("[Loadouts] Error loading loadouts: " + err.getMessage());
        }
    }

    private void saveLoadoutsToFile() {
        if (!checkOrCreateFile(LOADOUTS_FILE)) {
            System.err.println("[Loadouts] Could not access loadouts file for saving.");
            return;
        }

        Gson gson = new Gson();
        try (Writer writer = new FileWriter(LOADOUTS_FILE)) {
            HashMap<String, HashMap<Integer, String>> itemNameMap = new HashMap<>();
            for (Map.Entry<String, HashMap<Integer, Item>> entry : loadouts.entrySet()) {
                HashMap<Integer, String> nameMap = new HashMap<>();
                for (Map.Entry<Integer, Item> itemEntry : entry.getValue().entrySet()) {
                    nameMap.put(itemEntry.getKey(), Registries.ITEM.getId(itemEntry.getValue()).toString());
                }
                itemNameMap.put(entry.getKey(), nameMap);
            }
            gson.toJson(itemNameMap, writer);
        } catch (Exception err) {
            System.err.println("[Loadouts] Error saving loadouts: " + err.getMessage());
        }
    }

    private boolean isLoaded(String loadoutKey) {
        if (loadouts.isEmpty()) return true;
        if (mc.player == null) return true;
        if (!loadouts.containsKey(loadoutKey)) return true;
        if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler handler)) return true;

        HashMap<Integer, Item> loadout = loadouts.get(loadoutKey);
        for (int n = PlayerScreenHandler.EQUIPMENT_START; n < handler.slots.size(); n++) {
            if (!loadout.containsKey(n)) continue;
            ItemStack stack = handler.getSlot(n).getStack();
            if (!stack.isOf(loadout.get(n))) return false;
        }
        return true;
    }

    public void saveLoadout(String name) {
        if (mc.player == null) return;
        if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler handler)) return;

        HashMap<Integer, Item> loadout = new HashMap<>();
        for (int n = PlayerScreenHandler.EQUIPMENT_START; n < handler.slots.size(); n++) {
            ItemStack stack = handler.getSlot(n).getStack();
            if (!stack.isEmpty() && !stack.isOf(Items.AIR)) {
                loadout.put(n, stack.getItem());
            }
        }
        loadouts.put(name, loadout);
        saveLoadoutsToFile();
    }

    public void loadLoadout(String name) {
        if (mc.player == null) return;
        if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler handler)) return;

        if (loadouts.isEmpty() || !loadouts.containsKey(name) || loadouts.get(name).isEmpty()) {
            return;
        }

        jobs.clear();
        activeLoadoutKey = name;
        ArrayList<Integer> sorted = new ArrayList<>();
        HashMap<Integer, Item> loadout = loadouts.get(name);
        HashMap<Integer, ItemStack> changedSlots = new HashMap<>();

        for (int to = PlayerScreenHandler.EQUIPMENT_START; to < handler.slots.size(); to++) {
            Item assigned = loadout.get(to);
            if (assigned == null) continue;

            ItemStack current = handler.getSlot(to).getStack();
            if (current.isOf(assigned)) {
                sorted.add(to);
                continue;
            }

            for (int from = PlayerScreenHandler.EQUIPMENT_START; from < handler.slots.size(); from++) {
                if (to == from || sorted.contains(from)) continue;
                ItemStack occupiedBy = changedSlots.containsKey(from)
                    ? changedSlots.get(from)
                    : handler.getSlot(from).getStack();

                if (occupiedBy.isOf(assigned)) {
                    if (loadout.get(from) != null && occupiedBy.isOf(loadout.get(from))) {
                        sorted.add(from);
                        continue;
                    }

                    if (!current.isEmpty()) {
                        sorted.add(to);
                        changedSlots.put(from, current);
                        jobs.addLast(new Pair<>(from, to));
                    } else {
                        sorted.add(to);
                        sorted.add(from);
                        changedSlots.remove(from);
                        jobs.addLast(new Pair<>(from, to));
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler)) return;

        ++ticks;
        if (ticks >= tickRateSetting.get()) {
            ticks = 0;
            if (!jobs.isEmpty()) {
                isSorted = false;
                Pair<Integer, Integer> entry = jobs.removeFirst();
                InvUtils.move().fromId(entry.getA()).toId(entry.getB());
            }
            if (jobs.isEmpty() && !isSorted) {
                if (!doubleTap && !isLoaded(activeLoadoutKey)) {
                    doubleTap = true;
                    loadLoadout(activeLoadoutKey);
                } else {
                    isSorted = true;
                    doubleTap = false;
                }
            }
        }
    }
}
