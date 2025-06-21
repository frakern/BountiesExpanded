package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetUpgradeHelper;
import de.schafunschaf.bountiesexpanded.helper.ship.ShipUtils;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity.EntityProvider;
import lombok.extern.log4j.Log4j;

import java.util.Random;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

@Log4j
public class BountyHunterManager extends BaseEventManager {
    public static final String KEY = "$bountiesExpanded_bountyHunterManager";
    public static final String BOUNTY_HUNTER_FLEET_KEY = "$bountiesExpanded_bountyHunterFleet";

    public BountyHunterManager() {
        super();
        Global.getSector().getMemoryWithoutUpdate().set(KEY, this);
    }

    public static BountyHunterManager getInstance() {
        Object test = Global.getSector().getMemoryWithoutUpdate().get(KEY);
        return (BountyHunterManager) test;
    }

    @Override
    protected int getMinConcurrent() {
        if (Settings.isDebugActive())
            return 5;

        return Settings.bountyHunterMinBounties;
    }

    @Override
    protected int getMaxConcurrent() {
        if (Settings.isDebugActive())
            return 5;

        return Settings.bountyHunterMaxBounties;
    }

    @Override
    protected EveryFrameScript createEvent() {
        if (Settings.bountyHunterActive) {
            if (Settings.isDebugActive())
                return createBountyHunterEvent();
            if (new Random().nextFloat() <= Settings.bountyHunterSpawnChance)
                return createBountyHunterEvent();
        }

        return null;
    }

    public BountyHunterIntel createBountyHunterEvent() {
        log.info("BountiesExpanded: creating new Bounty Hunter");
        final BountyHunterEntity bountyHunterEntity = EntityProvider.bountyHunterEntity();
        if (isNull(bountyHunterEntity)) {
            log.warn("BountiesExpanded: failed to create Bounty Hunter");
            return null;
        }
        final CampaignFleetAPI bountyFleet = bountyHunterEntity.getFleet();
        //bountyFleet.setName((String) CollectionUtils.getRandomEntry(NameStringCollection.suspiciousNames));
        // TODO Don't spawn fleet yet?
        //FleetGenerator.spawnFleet(bountyFleet, bountyHunterEntity.getSpawnLocation());

        final MemoryAPI fleetMemory = bountyFleet.getMemoryWithoutUpdate();

        fleetMemory.set(EntityProvider.FLEET_IDENTIFIER_KEY, BOUNTY_HUNTER_FLEET_KEY);
        fleetMemory.set(BOUNTY_HUNTER_FLEET_KEY, bountyHunterEntity);

        log.info("BountiesExpanded - Spawning Bounty Hunter: By "
                + bountyHunterEntity.getOfferingFaction().getDisplayName() + " | At "
                + bountyHunterEntity.getSpawnLocation().getName());
        log.info("Player-FP at creation: " + Global.getSector().getPlayerFleet().getFleetPoints());
        log.info("Enemy-FP at creation: " + bountyHunterEntity.getFleet().getFleetPoints());
        log.info("Difficulty: " + bountyHunterEntity.getDifficulty().getShortDescription());

        upgradeShips(bountyFleet);

        BountyHunterIntel intel = new BountyHunterIntel(bountyHunterEntity, bountyHunterEntity.getFleet(), bountyHunterEntity.getFleet().getCommander(), bountyHunterEntity.getSpawnLocation(), null);
        intel.updateLocationToken();

        return intel;
    }

    public void upgradeShips(CampaignFleetAPI bountyFleet) {
        if (isNull(bountyFleet))
            return;

        Random random = new Random(bountyFleet.getId().hashCode() * 1337L);
        int modValue = ((BountyHunterEntity) bountyFleet.getMemoryWithoutUpdate().get(BountyHunterManager.BOUNTY_HUNTER_FLEET_KEY)).getDifficulty().getFlatModifier();
        FleetMemberAPI flagship = bountyFleet.getFlagship();
        if (isNull(flagship))
            return;

        if (flagship.getVariant().getSMods().isEmpty()) {
            ShipUtils.upgradeShip(flagship, 2, random);
            ShipUtils.addMinorUpgrades(flagship, random);
        }

        FleetUpgradeHelper.upgradeRandomShips(bountyFleet, modValue, modValue * 0.1f, true, random);
    }
}
