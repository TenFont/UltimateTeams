package dev.xf3d3.ultimateteams.api.events;

import dev.xf3d3.ultimateteams.config.Upgrades.UpgradeId;
import dev.xf3d3.ultimateteams.models.Team;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is about to buy the next level of a team upgrade, before the cost is taken from the player
 */
public class TeamUpgradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled = false;

    @Getter
    private final Player createdBy;
    @Getter
    private final Team playerTeam;
    @Getter
    private final UpgradeId upgradeId;
    @Getter
    private final int level;
    @Getter
    private final double cost;


    public TeamUpgradeEvent(@NotNull Player createdBy, @NotNull Team playerTeam, @NotNull UpgradeId upgradeId, int level, double cost) {
        this.createdBy = createdBy;
        this.playerTeam = playerTeam;
        this.upgradeId = upgradeId;
        this.level = level;
        this.cost = cost;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.isCancelled = b;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
