package dev.xf3d3.ultimateteams.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Upgrades {

    public static final String UPGRADES_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃     UltimateTeams Config     ┃
            ┃      Developed by xF3d3      ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┃
            ┗╸ Information: https://modrinth.com/plugin/ultimate-teams""";

    @Comment({
            "The team slots upgrade path. Each level sets the maximum amount of members a team can have."
    })
    private SlotsUpgrade slots = new SlotsUpgrade();

    @Comment({
            "Booster upgrade paths, one per AxBoosters booster type.",
            "Each level sets the multiplier every team member gets for that booster type.",
            "Example:",
            "  money:",
            "    levels:",
            "      1:",
            "        cost: 2000.0",
            "        multiplier: 1.5",
            "      2:",
            "        cost: 5000.0",
            "        multiplier: 2.0",
    })
    private Map<String, BoosterUpgrade> boosters = new LinkedHashMap<>();

    /**
     * The names of all the configured upgrade paths, as typed in the upgrade command
     *
     * @return the names of the slots path and of every booster path
     */
    public Set<String> names() {
        final Set<String> names = new LinkedHashSet<>();
        names.add("slots");
        names.addAll(boosters.keySet());

        return names;
    }

    /**
     * Finds the configured upgrade path an {@link UpgradeId} refers to
     *
     * @param id the id of the upgrade path
     * @return the path, or empty if no such path is configured
     */
    public Optional<UpgradePath<?>> find(@NotNull UpgradeId id) {
        return switch (id) {
            case UpgradeId.Slots s -> Optional.of(slots);
            case UpgradeId.Booster b -> Optional.ofNullable(boosters.get(b.type()));
        };
    }

    /**
     * Identifies an upgrade path: either the (single) slots path, or a booster path of a given booster type
     */
    public sealed interface UpgradeId {
        /**
         * The string this id is stored under in a team's upgrade levels
         */
        String key();

        /**
         * Parses the name of an upgrade as typed in the upgrade command
         *
         * @param name the name of the upgrade: "slots", or a booster type
         * @return the id of the upgrade, which may not be configured
         */
        static UpgradeId parse(@NotNull String name) {
            return name.equalsIgnoreCase("slots") ? new Slots() : new Booster(name);
        }

        record Slots() implements UpgradeId {
            @Override
            public String key() {
                return "slots";
            }
        }

        record Booster(String type) implements UpgradeId {
            @Override
            public String key() {
                return "booster-" + type;
            }
        }
    }

    /**
     * A single level of an upgrade path
     */
    public interface UpgradeLevel {
        double getCost();
    }

    /**
     * An upgrade path: a set of numbered levels a team can progress through
     *
     * @param <L> the type of the levels of this path
     */
    public interface UpgradePath<L extends UpgradeLevel> {
        Map<Integer, L> getLevels();

        /**
         * The level that applies to a team that reached the given level number: the highest configured
         * level that is lower or equal to it. If the exact level was removed from the config, this falls
         * back to the closest lower level that still exists.
         *
         * @param reached the level number the team reached
         * @return the level that applies, or empty if no configured level is lower or equal to it
         */
        default Optional<L> effectiveLevel(int reached) {
            return Optional.ofNullable(new TreeMap<>(getLevels()).floorEntry(reached)).map(Map.Entry::getValue);
        }

        /**
         * The next level a team that reached the given level number can buy: the lowest configured level
         * that is higher than it.
         *
         * @param reached the level number the team reached
         * @return the level number and the level, or empty if the team is already at the last level
         */
        default Optional<Map.Entry<Integer, L>> nextLevel(int reached) {
            return Optional.ofNullable(new TreeMap<>(getLevels()).higherEntry(reached));
        }
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SlotsUpgrade implements UpgradePath<SlotsLevel> {
        @Comment("Level number -> cost (taken from the balance of the player buying it) and the max amount of members at that level")
        private Map<Integer, SlotsLevel> levels = new TreeMap<>(Map.of(
                1, new SlotsLevel(1000.0, 10),
                2, new SlotsLevel(2500.0, 12),
                3, new SlotsLevel(5000.0, 15)
        ));
    }

    @Getter
    @Configuration
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SlotsLevel implements UpgradeLevel {
        @Comment("How much this level costs (taken from the balance of the player buying it)")
        private double cost = 0;

        @Comment("The maximum amount of members a team can have at this level")
        private int slots = 0;
    }

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BoosterUpgrade implements UpgradePath<BoosterLevel> {
        @Comment("Level number -> cost (taken from the balance of the player buying it) and the booster multiplier at that level")
        private Map<Integer, BoosterLevel> levels = new TreeMap<>();
    }

    @Getter
    @Configuration
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BoosterLevel implements UpgradeLevel {
        @Comment("How much this level costs (taken from the balance of the player buying it)")
        private double cost = 0;

        @Comment("The booster multiplier every team member gets at this level")
        private double multiplier = 1;
    }
}
