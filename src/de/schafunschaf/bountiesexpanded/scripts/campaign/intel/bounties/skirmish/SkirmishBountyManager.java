package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.skirmish;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import de.schafunschaf.bountiesexpanded.Blacklists;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.faction.HostileFactionPicker;
import de.schafunschaf.bountiesexpanded.helper.faction.MiscFactionUtils;
import de.schafunschaf.bountiesexpanded.helper.faction.ParticipatingFactionPicker;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetGenerator;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetUpgradeHelper;
import de.schafunschaf.bountiesexpanded.helper.location.CoreWorldPicker;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter.BountyHunterEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter.BountyHunterManager;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity.EntityProvider;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import exerelin.campaign.fleets.InvasionFleetManager;
import exerelin.campaign.intel.colony.ColonyExpeditionIntel;
import exerelin.campaign.intel.defensefleet.DefenseFleetIntel;
import exerelin.campaign.intel.fleets.OffensiveFleetIntel;
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

        Set<String> blacklist = Blacklists.getSkirmishBountyBlacklist();
        FactionAPI offeringFaction = null;
        FactionAPI targetedFaction = null;
        MarketAPI market = null;
        boolean nex = false;

        // If Nexerelin is installed, give priority to systems with current invasion.
        if (Global.getSettings().getModManager().isModEnabled("nexerelin")) {
            List<IntelInfoPlugin> all_intel = Global.getSector().getIntelManager().getIntel(OffensiveFleetIntel.class);
            for (IntelInfoPlugin intel : all_intel)
            {
                if (intel instanceof OffensiveFleetIntel off) {
                    if (off instanceof DefenseFleetIntel || off instanceof ColonyExpeditionIntel)
                        continue;

                    if (off.getTarget() == null) continue;

                    if (!MiscFactionUtils.canFactionOfferBounties(off.getFaction())) continue;

                    if (blacklist.contains(off.getTargetFaction().getId())) continue;

                    // Check if there's already a bounty offered by this faction in this system.
                    boolean bountyExists = bountiesActiveForFaction.contains(off.getFaction().getId())
                            && bountiesActiveAtEntity.contains(off.getTarget().getContainingLocation().getId());

                    if (!bountyExists) {
                        offeringFaction = off.getFaction();
                        market = off.getTarget();
                        targetedFaction = off.getTargetFaction();
                        nex = true;;
                        break;
                    }
                }
            }
        }

        if (isNull(offeringFaction) && isNull(market) && isNull(targetedFaction)) {
            offeringFaction = ParticipatingFactionPicker.pickFaction();
            if (!MiscFactionUtils.canFactionOfferBounties(offeringFaction)) return null;

            targetedFaction = HostileFactionPicker.pickParticipatingFaction(offeringFaction, blacklist, true);
            if (isNull(targetedFaction)) {
                log.warn(EntityProvider.NO_TARGETED_FACTION);
                return null;
            }

            market = CoreWorldPicker.pickFactionHideout(targetedFaction).getMarket();
            if (isNull(market)) {
                log.warn(EntityProvider.NO_HIDEOUT);
                return null;
            }
        }

        SkirmishBountyEntity skirmishBountyEntity = new SkirmishBountyEntity(offeringFaction, targetedFaction, market, nex);

        if (isNull(skirmishBountyEntity)) {
            return null;
        }

        if (hasActiveBounty(skirmishBountyEntity)) {
            log.warn("BountiesExpanded: Skirmish bounty already exists at target");
            return null;
        }

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
        bountiesActiveForFaction.add(bountyEntity.getOfferingFaction().getId());
        bountiesActiveAtEntity.add(bountyEntity.getTargetMarket().getContainingLocation().getId());
    }

    public void unregisterBounty(SkirmishBountyEntity bountyEntity) {
        bountiesActiveForFaction.remove(bountyEntity.getOfferingFaction().getId());
        bountiesActiveAtEntity.remove(bountyEntity.getTargetMarket().getContainingLocation().getId());
    }

    public boolean hasActiveBounty(SkirmishBountyEntity bountyEntity) {
        return bountiesActiveForFaction.contains(bountyEntity.getOfferingFaction().getId())
                && bountiesActiveAtEntity.contains(bountyEntity.getTargetMarket().getContainingLocation().getId());
    }
}