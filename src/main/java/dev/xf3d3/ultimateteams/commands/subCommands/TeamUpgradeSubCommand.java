package dev.xf3d3.ultimateteams.commands.subCommands;

import de.themoep.minedown.adventure.MineDown;
import dev.xf3d3.ultimateteams.UltimateTeams;
import dev.xf3d3.ultimateteams.config.Upgrades.UpgradeId;
import dev.xf3d3.ultimateteams.config.Upgrades.UpgradePath;
import dev.xf3d3.ultimateteams.models.Team;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TeamUpgradeSubCommand {

    private static final String PLAYER_PLACEHOLDER = "%PLAYER%";
    private static final String UPGRADE_PLACEHOLDER = "%UPGRADE%";
    private static final String UPGRADES_PLACEHOLDER = "%UPGRADES%";
    private static final String LEVEL_PLACEHOLDER = "%LEVEL%";
    private static final String COST_PLACEHOLDER = "%COST%";
    private static final String CURRENCY_PLACEHOLDER = "%CURRENCY%";

    private final UltimateTeams plugin;

    public TeamUpgradeSubCommand(@NotNull UltimateTeams plugin) {
        this.plugin = plugin;
    }

    public void teamUpgradeSubCommand(CommandSender sender, @Nullable String name) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(MineDown.parse(plugin.getMessages().getGeneral().getPlayerOnlyCommand()));
            return;
        }

        if (plugin.getEconomyHook() == null) {
            player.sendMessage(MineDown.parse(plugin.getMessages().getGeneral().getFunctionDisabled()));

            return;
        }

        final String upgrades = String.join("|", plugin.getUpgrades().names());

        if (name == null) {
            player.sendMessage(MineDown.parse(plugin.getMessages().getTeam().getUpgrade().getIncorrectUsage().replace(UPGRADES_PLACEHOLDER, upgrades)));

            return;
        }

        final UpgradeId id = UpgradeId.parse(name);
        final Optional<UpgradePath<?>> optionalPath = plugin.getUpgrades().find(id);

        if (optionalPath.isEmpty()) {
            player.sendMessage(MineDown.parse(plugin.getMessages().getTeam().getUpgrade().getUnknownUpgrade()
                    .replace(UPGRADE_PLACEHOLDER, name)
                    .replace(UPGRADES_PLACEHOLDER, upgrades)
            ));

            return;
        }

        final UpgradePath<?> path = optionalPath.get();

        plugin.getTeamStorageUtil().findTeamByMember(player.getUniqueId()).ifPresentOrElse(
                team -> {
                    // Check permission
                    if (!(plugin.getTeamStorageUtil().isTeamOwner(player) || (plugin.getTeamStorageUtil().isTeamManager(player) && team.hasPermission(Team.Permission.UPGRADE)))) {
                        player.sendMessage(MineDown.parse(plugin.getMessages().getGeneral().getNoPermission()));

                        return;
                    }

                    // Find the next level the team can buy
                    final var nextLevel = path.nextLevel(team.getUpgradeLevel(id));

                    if (nextLevel.isEmpty()) {
                        player.sendMessage(MineDown.parse(plugin.getMessages().getTeam().getUpgrade().getMaxLevelReached().replace(UPGRADE_PLACEHOLDER, name)));

                        return;
                    }

                    final int level = nextLevel.get().getKey();
                    final double cost = nextLevel.get().getValue().getCost();
                    final String currencyName = cost == 1 ? plugin.getEconomyHook().getCurrencyNameSingular() : plugin.getEconomyHook().getCurrencyNamePlural();

                    // Take the cost from the player's balance
                    if (!plugin.getEconomyHook().takeMoney(player, cost)) {
                        player.sendMessage(MineDown.parse(plugin.getMessages().getTeam().getUpgrade().getNotEnoughFunds()
                                .replace(COST_PLACEHOLDER, String.valueOf(cost))
                                .replace(CURRENCY_PLACEHOLDER, currencyName)
                        ));

                        return;
                    }

                    team.setUpgradeLevel(id, level);
                    plugin.runAsync(task -> plugin.getTeamStorageUtil().updateTeamData(player, team));

                    team.sendTeamMessage(MineDown.parse(plugin.getMessages().getTeam().getUpgrade().getBroadcast()
                            .replace(PLAYER_PLACEHOLDER, player.getName())
                            .replace(UPGRADE_PLACEHOLDER, name)
                            .replace(LEVEL_PLACEHOLDER, String.valueOf(level))
                            .replace(COST_PLACEHOLDER, String.valueOf(cost))
                            .replace(CURRENCY_PLACEHOLDER, currencyName)
                    ));
                },
                () -> player.sendMessage(MineDown.parse(plugin.getMessages().getTeam().getInfo().getNotInTeam()))
        );
    }
}
