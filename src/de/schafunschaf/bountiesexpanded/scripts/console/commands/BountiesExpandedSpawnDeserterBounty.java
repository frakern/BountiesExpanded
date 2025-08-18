package de.schafunschaf.bountiesexpanded.scripts.console.commands;

import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.deserter.DeserterBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.deserter.DeserterBountyManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

public class BountiesExpandedSpawnDeserterBounty implements BaseCommand {
     public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage("Error: This command is campaign-only.");
            return CommandResult.WRONG_CONTEXT;
        }

        DeserterBountyManager manager = DeserterBountyManager.getInstance();
        if (isNull(manager)) {
            Console.showMessage("the DeserterBountyManager instance is missing!");
            return CommandResult.ERROR;
        }

        Console.showMessage("attempting to spawn DeserterBounty...");
        DeserterBountyIntel DeserterBountyEvent = manager.createDeserterBountyEvent();
        if (isNotNull(DeserterBountyEvent)) {
            manager.addActive(DeserterBountyEvent);
            Console.showMessage("it worked!");
            Console.showMessage("Spawned DeserterBounty at " + DeserterBountyEvent.getSpawnLocation().getName());
            return CommandResult.SUCCESS;
        }

        Console.showMessage("it didn't work!");
        return CommandResult.ERROR;
    }
}
