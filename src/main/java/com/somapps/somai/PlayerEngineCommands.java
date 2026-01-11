package com.somapps.somai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralized command strings for PlayerEngine's command manager.
 *
 * We keep these in one place so the rest of the mod (UI, whistle presets, etc.)
 * does not hardcode scattered literals.
 *
 * Notes:
 * - Exact command names/arguments are owned by PlayerEngine.
 * - If a command name differs for your PlayerEngine version, fix it here.
 */
public final class PlayerEngineCommands {
    private PlayerEngineCommands() {
    }

    public enum Category {
        ALL,
        CONTROL,
        MOVEMENT,
        INVENTORY,
        COMBAT,
        MISC
    }

    /**
     * A UI-friendly command template.
     *
     * @param id stable identifier for UI/state
     * @param category grouping for UI
     * @param label button label
     * @param template the string sent to PlayerEngine command manager (may include placeholders)
     * @param description short hint shown in UI tooltips
     */
    public record CommandSpec(String id, Category category, String label, String template, String description) {
        public CommandSpec {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id");
            }
            if (category == null) {
                throw new IllegalArgumentException("category");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("label");
            }
            if (template == null || template.isBlank()) {
                throw new IllegalArgumentException("template");
            }
            if (description == null) {
                description = "";
            }
        }
    }

    private static final List<CommandSpec> COMMAND_SPECS = buildSpecs();

    public static List<CommandSpec> allSpecs() {
        return COMMAND_SPECS;
    }

    public static List<CommandSpec> specs(Category category) {
        if (category == null || category == Category.ALL) {
            return COMMAND_SPECS;
        }

        List<CommandSpec> out = new ArrayList<>();
        for (CommandSpec spec : COMMAND_SPECS) {
            if (spec.category() == category) {
                out.add(spec);
            }
        }
        return out;
    }

    private static List<CommandSpec> buildSpecs() {
        List<CommandSpec> specs = new ArrayList<>();

        // Control
        specs.add(new CommandSpec("control.status", Category.CONTROL, "Status", Control.STATUS,
            "Prints current PlayerEngine state."));
        specs.add(new CommandSpec("control.list", Category.CONTROL, "List", Control.LIST,
            "Lists available PlayerEngine commands."));
        specs.add(new CommandSpec("control.stop", Category.CONTROL, "Stop", Control.STOP,
            "Stops current automation."));
        specs.add(new CommandSpec("control.pause", Category.CONTROL, "Pause", Control.PAUSE,
            "Pauses current automation."));
        specs.add(new CommandSpec("control.unpause", Category.CONTROL, "Unpause", Control.UNPAUSE,
            "Resumes after pause."));
        specs.add(new CommandSpec("control.idle", Category.CONTROL, "Idle", Control.IDLE,
            "Puts the automaton in idle mode."));
        specs.add(new CommandSpec("control.reload", Category.CONTROL, "Reload settings", Control.RELOAD_SETTINGS,
            "Reloads PlayerEngine settings."));
        specs.add(new CommandSpec("control.resetmem", Category.CONTROL, "Reset memory", Control.RESET_MEMORY,
            "Resets PlayerEngine memory/cache."));
        specs.add(new CommandSpec("control.aibridge", Category.CONTROL, "AI bridge", Control.SET_AI_BRIDGE_ENABLED + " <on|off>",
            "Toggles AI bridge integration (if supported)."));
        specs.add(new CommandSpec("control.hostile", Category.CONTROL, "Hostile attack", Control.SET_HOSTILE_ATTACK + " <on|off>",
            "Toggles hostile auto-attack behavior."));

        // Movement
        specs.add(new CommandSpec("move.goto", Category.MOVEMENT, "Goto", Movement.GOTO + " <x> <y> <z>",
            "Navigate to coordinates."));
        specs.add(new CommandSpec("move.follow", Category.MOVEMENT, "Follow", Movement.FOLLOW + " <player>",
            "Follow a player (usually the owner)."));
        specs.add(new CommandSpec("move.locate", Category.MOVEMENT, "Locate", Movement.LOCATE_STRUCTURE + " <structure>",
            "Locate a structure type."));

        // Inventory
        specs.add(new CommandSpec("inv.inventory", Category.INVENTORY, "Inventory", Inventory.INVENTORY,
            "Shows inventory overview (if supported)."));
        specs.add(new CommandSpec("inv.equip", Category.INVENTORY, "Equip", Inventory.EQUIP + " <slot> <item>",
            "Equips an item (arguments depend on PlayerEngine)."));
        specs.add(new CommandSpec("inv.pickupdrops", Category.INVENTORY, "Pickup drops", Inventory.PICKUP_DROPS,
            "Collects nearby dropped items."));
        specs.add(new CommandSpec("inv.stash", Category.INVENTORY, "Stash", Inventory.STASH,
            "Stores items in the configured stash."));
        specs.add(new CommandSpec("inv.deposit", Category.INVENTORY, "Deposit", Inventory.DEPOSIT,
            "Deposits items into nearby containers."));
        specs.add(new CommandSpec("inv.eatfood", Category.INVENTORY, "Eat food", Inventory.EAT_FOOD,
            "Eat something edible from inventory."));
        specs.add(new CommandSpec("inv.food", Category.INVENTORY, "Food", Inventory.FOOD,
            "Food-related helper (see PlayerEngine docs)."));
        specs.add(new CommandSpec("inv.meat", Category.INVENTORY, "Meat", Inventory.MEAT,
            "Meat-related helper (see PlayerEngine docs)."));
        specs.add(new CommandSpec("inv.get", Category.INVENTORY, "Get", Inventory.GET + " <item>",
            "Fetch an item (arguments depend on PlayerEngine)."));
        specs.add(new CommandSpec("inv.give", Category.INVENTORY, "Give", Inventory.GIVE + " <player> <item>",
            "Give an item to a player."));

        // Combat
        specs.add(new CommandSpec("combat.attack", Category.COMBAT, "Attack", Combat.ATTACK + " <target>",
            "Attacks a player or mob (see PlayerEngine args)."));

        // Misc
        specs.add(new CommandSpec("misc.body", Category.MISC, "Body lang", Misc.BODY_LANGUAGE,
            "Body language / emote style command."));
        specs.add(new CommandSpec("misc.hero", Category.MISC, "Hero", Misc.HERO,
            "Hero behavior preset."));
        specs.add(new CommandSpec("misc.gamer", Category.MISC, "Gamer", Misc.GAMER,
            "Gamer behavior preset."));
        specs.add(new CommandSpec("misc.farm", Category.MISC, "Farm", Misc.FARM + " <range>",
            "Farm around a point (range depends on PlayerEngine)."));
        specs.add(new CommandSpec("misc.fish", Category.MISC, "Fish", Misc.FISH,
            "Start fishing automation."));
        specs.add(new CommandSpec("misc.build", Category.MISC, "Build", Misc.BUILD_STRUCTURE + " <name>",
            "Build a named structure (see PlayerEngine)."));

        return Collections.unmodifiableList(specs);
    }

    /** High-level control / meta commands. */
    public static final class Control {
        private Control() {
        }

        public static final String LIST = "list";
        public static final String STATUS = "status";
        public static final String STOP = "stop";
        public static final String PAUSE = "pause";
        public static final String UNPAUSE = "unpause";
        public static final String IDLE = "idle";

        public static final String RELOAD_SETTINGS = "reloadsettings";
        public static final String RESET_MEMORY = "resetmemory";

        public static final String SET_AI_BRIDGE_ENABLED = "setaibridgeenabled";
        public static final String SET_HOSTILE_ATTACK = "sethostileattack";
    }

    /** Movement/navigation commands. */
    public static final class Movement {
        private Movement() {
        }

        public static final String GOTO = "goto";
        public static final String FOLLOW = "follow";
        public static final String LOCATE_STRUCTURE = "locatestructure";
    }

    /** Inventory & item interaction commands. */
    public static final class Inventory {
        private Inventory() {
        }

        public static final String INVENTORY = "inventory";
        public static final String EQUIP = "equip";

        public static final String PICKUP_DROPS = "pickupdrops";
        public static final String STASH = "stash";
        public static final String DEPOSIT = "deposit";

        public static final String EAT_FOOD = "eatfood";
        public static final String FOOD = "food";
        public static final String MEAT = "meat";

        public static final String GET = "get";
        public static final String GIVE = "give";
    }

    /** Combat-oriented commands. */
    public static final class Combat {
        private Combat() {
        }

        // Based on AttackPlayerOrMobCommand class.
        public static final String ATTACK = "attack";
    }

    /** Misc / cosmetic / roleplay commands. */
    public static final class Misc {
        private Misc() {
        }

        public static final String BODY_LANGUAGE = "bodylanguage";
        public static final String HERO = "hero";
        public static final String GAMER = "gamer";

        public static final String FARM = "farm";
        public static final String FISH = "fish";
        public static final String BUILD_STRUCTURE = "buildstructure";
    }
}
