package de.schafunschaf.bountiesexpanded.scripts.console.commands;

import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.warcriminal.WarCriminalIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.warcriminal.WarCriminalManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

public class BountiesExpandedSpawnWarCriminalBounty implements BaseCommand {
     public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage("Error: This command is campaign-only.");
            return CommandResult.WRONG_CONTEXT;
        }

        WarCriminalManager manager = WarCriminalManager.getInstance();
        if (isNull(manager)) {
            Console.showMessage("the WarCriminalManager instance is missing!");
            return CommandResult.ERROR;
        }

        Console.showMessage("attempting to spawn WarCriminalBounty..");
        WarCriminalIntel warcriminalBountyEvent = manager.createWarCriminalBountyEvent();
        if (isNotNull(warcriminalBountyEvent)) {
            manager.addActive(warcriminalBountyEvent);
            Console.showMessage("it worked!");
            Console.showMessage("Spawned WarCriminalBounty at " + warcriminalBountyEvent.getSpawnLocation().getName());
            return CommandResult.SUCCESS;
        }

        Console.showMessage("it didn't work!");
        return CommandResult.ERROR;
    }
}
