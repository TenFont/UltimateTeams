package dev.xf3d3.ultimateteams.hooks;

import com.artillexstudios.axboosters.api.AxBoostersAPI;
import com.artillexstudios.axboosters.api.events.AxBoostersGetMultiplierEvent;
import com.artillexstudios.axboosters.api.events.AxBoostersLoadEvent;
import com.artillexstudios.axboosters.boosters.BoosterType;
import com.artillexstudios.axboosters.users.User;
import com.artillexstudios.axboosters.users.UserList;
import dev.xf3d3.ultimateteams.UltimateTeams;
import dev.xf3d3.ultimateteams.api.events.TeamDisbandEvent;
import dev.xf3d3.ultimateteams.api.events.TeamMemberJoinEvent;
import dev.xf3d3.ultimateteams.api.events.TeamMemberLeaveEvent;
import dev.xf3d3.ultimateteams.api.events.TeamUpgradeEvent;
import dev.xf3d3.ultimateteams.config.Upgrades.BoosterLevel;
import dev.xf3d3.ultimateteams.config.Upgrades.UpgradeId;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Applies the booster upgrades of a team to its members through AxBoosters
 */
public class AxBoostersHook implements Listener {

    private final UltimateTeams plugin;

    public AxBoostersHook(@NotNull UltimateTeams plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.sendConsole("-------------------------------------------");
        plugin.sendConsole("&6UltimateTeams: &3Hooked into AxBoosters");
        plugin.sendConsole("&6UltimateTeams: &3Team booster upgrades enabled!");
        plugin.sendConsole("-------------------------------------------");
    }

    /**
     * Adds the percent of the booster upgrades of the player's team to the booster multiplier of the player
     */
    @EventHandler
    public void onGetMultiplier(AxBoostersGetMultiplierEvent event) {
        final Key hookKey = event.getBoosterHook().getKey();

        // Check first which booster upgrades boost this hook, so that we don't look up the team for nothing
        final List<String> boosterTypes = plugin.getUpgrades().getBoosters().keySet().stream()
                .filter(boosterType -> boosts(boosterType, hookKey))
                .toList();

        if (boosterTypes.isEmpty()) {
            return;
        }

        plugin.getTeamStorageUtil().findTeamByMember(event.getPlayer().getUniqueId()).ifPresent(team -> {
            final double percent = boosterTypes.stream()
                    .mapToDouble(boosterType -> plugin.getUpgrades().getBoosters().get(boosterType)
                            .effectiveLevel(team.getUpgradeLevel(new UpgradeId.Booster(boosterType)))
                            .map(BoosterLevel::getPercent)
                            .orElse(0.0)
                    )
                    .sum();

            if (percent > 0) {
                event.setMultiplier(event.getMultiplier() + (float) percent);
            }
        });
    }

    /**
     * Warns about the booster upgrades that are not booster types of AxBoosters. This is checked once AxBoosters
     * has loaded its booster types, which happens after this plugin is enabled.
     */
    @EventHandler
    public void onLoad(AxBoostersLoadEvent event) {
        plugin.getUpgrades().getBoosters().keySet().stream()
                .filter(boosterType -> !exists(boosterType))
                .forEach(boosterType -> plugin.log(Level.WARNING, "The booster upgrade '" + boosterType + "' in upgrades.yml is not a booster type of AxBoosters, it will be ignored"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUpgrade(TeamUpgradeEvent event) {
        // Only booster upgrades change the boosters of the members
        if (event.getUpgradeId() instanceof UpgradeId.Booster) {
            refresh(event.getPlayerTeam().getMembers().keySet());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMemberJoin(TeamMemberJoinEvent event) {
        refresh(List.of(event.getNewMember().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMemberLeave(TeamMemberLeaveEvent event) {
        refresh(List.of(event.getOldMember()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(TeamDisbandEvent event) {
        refresh(event.getTeam().getMembers().keySet());
    }

    private boolean exists(@NotNull String boosterTypeName) {
        try {
            return AxBoostersAPI.getBoosterType(boosterTypeName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean boosts(@NotNull String boosterTypeName, @NotNull Key hookKey) {
        try {
            final BoosterType boosterType = AxBoostersAPI.getBoosterType(boosterTypeName);

            return boosterType != null && boosterType.getBoosted().stream().anyMatch(hook -> hook.getKey().equals(hookKey));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Makes AxBoosters evaluate again the boosts of the given players, one tick later.
     * The events that call this are called before the change they announce is made.
     */
    private void refresh(@NotNull Collection<UUID> members) {
        for (final UUID uuid : List.copyOf(members)) {
            final Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                plugin.getScheduler().runAtEntityLater(player, () -> update(player), 1L);
            }
        }
    }

    private void update(@NotNull Player player) {
        try {
            final User user = UserList.getUser(player);

            if (user != null) {
                user.update();
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to update the AxBoosters boosts of " + player.getName(), e);
        }
    }
}
