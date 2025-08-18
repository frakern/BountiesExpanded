package de.schafunschaf.bountiesexpanded.scripts.console.commands;

import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.pirate.PirateBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.pirate.PirateBountyManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

public class BountiesExpandedSpawnPirateBounty implements BaseCommand {
     public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage("Error: This command is campaign-only.");
            return CommandResult.WRONG_CONTEXT;
        }

        PirateBountyManager manager = PirateBountyManager.getInstance();
        if (isNull(manager)) {
            Console.showMessage("the PirateBountyManager instance is missing!");
            return CommandResult.ERROR;
        }

        Console.showMessage("attempting to spawn PirateBounty...");
        PirateBountyIntel PirateBountyEvent = manager.createPirateBountyEvent();
        if (isNotNull(PirateBountyEvent)) {
            manager.addActive(PirateBountyEvent);
            Console.showMessage("it worked!");
            Console.showMessage("Spawned PirateBounty at " + PirateBountyEvent.getSpawnLocation().getName());
            return CommandResult.SUCCESS;
        }

        Console.showMessage("it didn't work!");
        return CommandResult.ERROR;
    }
}
