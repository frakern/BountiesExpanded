package de.schafunschaf.bountiesexpanded.helper.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import de.schafunschaf.bountiesexpanded.helper.ship.HullModUtils;
import de.schafunschaf.bountiesexpanded.helper.ship.ShipUtils;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.RareFlagshipManager;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import de.schafunschaf.bountiesexpanded.util.ComparisonTools;

import java.util.Random;

public class FleetUpgradeHelper {
    public static void upgradeRandomShips(CampaignFleetAPI fleet, int numSMods, float probability, boolean excludeFlagship, Random random) {
        fleet.inflateIfNeeded();

        if (ComparisonTools.isNull(random))
            random = new Random();

        FleetDataAPI fleetData = fleet.getFleetData();
        for (FleetMemberAPI fleetMember : fleetData.getMembersListCopy()) {
            if (excludeFlagship && fleetMember.isFlagship())
                continue;

            ShipUtils.upgradeShip(fleetMember, numSMods, random, probability);
        }
    }
}
