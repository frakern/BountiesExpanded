package de.schafunschaf.bountiesexpanded.scripts.console.commands;

import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter.BountyHunterIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter.BountyHunterManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

public class BountiesExpandedSpawnBountyHunter implements BaseCommand {
     public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage("Error: This command is campaign-only.");
            return CommandResult.WRONG_CONTEXT;
        }

        BountyHunterManager manager = BountyHunterManager.getInstance();
        if (isNull(manager)) {
            Console.showMessage("the BountyHunterManager instance is missing!");
            return CommandResult.ERROR;
        }

        Console.showMessage("attempting to spawn BountyHunter...");
        BountyHunterIntel BountyHunterEvent = manager.createBountyHunterEvent();
        if (isNotNull(BountyHunterEvent)) {
            manager.addActive(BountyHunterEvent);
            Console.showMessage("it worked!");
            Console.showMessage("Spawned BountyHunter at " + BountyHunterEvent.getSpawnLocation().getName());
            return CommandResult.SUCCESS;
        }

        Console.showMessage("it didn't work!");
        return CommandResult.ERROR;
    }
}
