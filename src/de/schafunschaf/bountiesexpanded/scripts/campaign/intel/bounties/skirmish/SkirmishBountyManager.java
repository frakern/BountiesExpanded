package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.skirmish;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetGenerator;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetUpgradeHelper;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter.BountyHunterEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter.BountyHunterManager;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity.EntityProvider;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import lombok.extern.log4j.Log4j;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

@Log4j
public class SkirmishBountyManager extends BaseEventManager {
    public static final String KEY = "$bountiesExpanded_skirmishBountyManager";
    private final Set<String> bountiesActiveForFaction = new HashSet<>();
    private final Set<String> bountiesActiveAtEntity = new HashSet<>();

    public SkirmishBountyManager() {
        super();
        Global.getSector().getMemoryWithoutUpdate().set(KEY, this);
    }

    public static SkirmishBountyManager getInstance() {
        Object instance = Global.getSector().getMemoryWithoutUpdate().get(KEY);
        return (SkirmishBountyManager) instance;
    }

    @Override
    protected int getMinConcurrent() {
        if (Settings.isDebugActive())
            return 5;

        return Settings.skirmishMinBounties;
    }

    @Override
    protected int getMaxConcurrent() {
        if (Settings.isDebugActive())
            return 5;

        return Settings.skirmishMaxBounties;
    }

    @Override
    protected EveryFrameScript createEvent() {
        if (Settings.skirmishActive) {
            if (Settings.isDebugActive())
                return createSkirmishBountyEvent();
            if (new Random().nextFloat() <= Settings.skirmishSpawnChance) {
                return createSkirmishBountyEvent();
            }
        }

        return null;
    }

    public SkirmishBountyIntel createSkirmishBountyEvent() {
        SkirmishBountyEntity skirmishBountyEntity = EntityProvider.skirmishBountyEntity();

        if (isNull(skirmishBountyEntity))
            return null;
        if (hasActiveBounty(skirmishBountyEntity))
            return null;

        MarketAPI targetMarket = skirmishBountyEntity.getTargetMarket();

        final SkirmishBountyIntel skirmishBountyIntel = new SkirmishBountyIntel(skirmishBountyEntity, targetMarket);

        log.info("BountiesExpanded - Spawning Skirmish Bounty: By "
                + skirmishBountyEntity.getOfferingFaction().getDisplayName() + " | Against "
                + skirmishBountyEntity.getTargetedFaction().getDisplayName() + " | At "
                + targetMarket.getStarSystem().getName());

        registerBounty(skirmishBountyEntity);

        return skirmishBountyIntel;
    }

    public void registerBounty(SkirmishBountyEntity bountyEntity) {
        bountiesActiveForFaction.add(bountyEntity.getTargetedFaction().getId());
        bountiesActiveAtEntity.add(bountyEntity.getTargetMarket().getId());
    }

    public void unregisterBounty(SkirmishBountyEntity bountyEntity) {
        bountiesActiveForFaction.remove(bountyEntity.getTargetedFaction().getId());
        bountiesActiveAtEntity.remove(bountyEntity.getTargetMarket().getId());
    }

    public boolean hasActiveBounty(SkirmishBountyEntity bountyEntity) {
        return bountiesActiveForFaction.contains(bountyEntity.getTargetedFaction().getId())
                || bountiesActiveAtEntity.contains(bountyEntity.getTargetMarket().getId());
    }
}