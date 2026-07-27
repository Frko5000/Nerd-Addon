package com.nerds.addon.hud;

import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class StatsHud extends HudElement {
    public static final HudElementInfo<StatsHud> INFO = new HudElementInfo<>(NerdAddon.HUD_GROUP, "stats", "Displays account and vanilla stat values.", StatsHud::new);

    public enum Mode {
        Account,
        PlayerKills,
        MobKills,
        Deaths,
        Playtime,
        DamageDealt,
        DamageTaken
    }

    static {
        INFO.addPreset("Account", hud -> hud.mode.set(Mode.Account));
        INFO.addPreset("Player Kills", hud -> hud.mode.set(Mode.PlayerKills));
        INFO.addPreset("Mob Kills", hud -> hud.mode.set(Mode.MobKills));
        INFO.addPreset("Deaths", hud -> hud.mode.set(Mode.Deaths));
        INFO.addPreset("Playtime", hud -> hud.mode.set(Mode.Playtime));
        INFO.addPreset("Damage Dealt", hud -> hud.mode.set(Mode.DamageDealt));
        INFO.addPreset("Damage Taken", hud -> hud.mode.set(Mode.DamageTaken));
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which account/stat value to display.")
        .defaultValue(Mode.Account)
        .build()
    );

    private final Setting<Boolean> customLabel = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-label")
        .description("Uses a custom label before the value.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> label = sgGeneral.add(new StringSetting.Builder()
        .name("label")
        .description("Custom label.")
        .defaultValue("")
        .visible(customLabel::get)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> syncStats = sgGeneral.add(new BoolSetting.Builder()
        .name("sync-stats")
        .description("Requests fresh vanilla stats from the server.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.Account)
        .build()
    );

    private final Setting<SettingColor> labelColor = sgColors.add(new ColorSetting.Builder()
        .name("label-color")
        .description("Color of the label.")
        .defaultValue(new SettingColor(175, 175, 175))
        .build()
    );

    private final Setting<SettingColor> valueColor = sgColors.add(new ColorSetting.Builder()
        .name("value-color")
        .description("Color of the stat value.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    public StatsHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        if (syncStats.get() && mode.get() != Mode.Account) requestStats();
    }

    @Override
    public void render(HudRenderer renderer) {
        String left = getLabel();
        String right = getValue();
        boolean shadow = this.shadow.get();
        double leftWidth = renderer.textWidth(left, shadow);
        double rightWidth = renderer.textWidth(right, shadow);

        setSize(leftWidth + rightWidth, renderer.textHeight(shadow));

        double x = this.x;
        x = renderer.text(left, x, y, labelColor.get(), shadow);
        renderer.text(right, x, y, valueColor.get(), shadow);
    }

    private String getLabel() {
        if (customLabel.get() && !label.get().isBlank()) return label.get() + ": ";

        return switch (mode.get()) {
            case Account -> "Account: ";
            case PlayerKills -> "Kills: ";
            case MobKills -> "Mob Kills: ";
            case Deaths -> "Deaths: ";
            case Playtime -> "Playtime: ";
            case DamageDealt -> "Damage Dealt: ";
            case DamageTaken -> "Damage Taken: ";
        };
    }

    private String getValue() {
        if (mc.player == null) return mode.get() == Mode.Account ? "N/A" : "0";

        return switch (mode.get()) {
            case Account -> mc.getSession().getUsername();
            case PlayerKills -> Integer.toString(getCustomStat(Stats.PLAYER_KILLS));
            case MobKills -> Integer.toString(getCustomStat(Stats.MOB_KILLS));
            case Deaths -> Integer.toString(getCustomStat(Stats.DEATHS));
            case Playtime -> formatPlaytime(getCustomStat(Stats.PLAY_TIME));
            case DamageDealt -> formatDamage(getCustomStat(Stats.DAMAGE_DEALT));
            case DamageTaken -> formatDamage(getCustomStat(Stats.DAMAGE_TAKEN));
        };
    }

    private int getCustomStat(Identifier id) {
        return mc.player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(id));
    }

    private void requestStats() {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
    }

    private String formatDamage(int raw) {
        return String.format("%.1f", raw / 10.0);
    }

    private String formatPlaytime(int ticks) {
        long seconds = ticks / 20L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;

        if (hours > 0) return String.format("%dh %02dm", hours, minutes);
        if (minutes > 0) return String.format("%dm %02ds", minutes, secs);
        return secs + "s";
    }
}