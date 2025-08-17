package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import de.schafunschaf.bountiesexpanded.Blacklists;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.credits.CreditCalculator;
import de.schafunschaf.bountiesexpanded.helper.faction.BountyHunterFactionPicker;
import de.schafunschaf.bountiesexpanded.helper.faction.HostileFactionPicker;
import de.schafunschaf.bountiesexpanded.helper.faction.MiscFactionUtils;
import de.schafunschaf.bountiesexpanded.helper.faction.ParticipatingFactionPicker;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetGenerator;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetPointCalculator;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetUpgradeHelper;
import de.schafunschaf.bountiesexpanded.helper.level.LevelPicker;
import de.schafunschaf.bountiesexpanded.helper.location.CoreWorldPicker;
import de.schafunschaf.bountiesexpanded.helper.location.RemoteWorldPicker;
import de.schafunschaf.bountiesexpanded.helper.location.TagCollection;
import de.schafunschaf.bountiesexpanded.helper.market.MarketUtils;
import de.schafunschaf.bountiesexpanded.helper.person.BountyGiverGenerator;
import de.schafunschaf.bountiesexpanded.helper.person.OfficerGenerator;
import de.schafunschaf.bountiesexpanded.helper.ship.HullModUtils;
import de.schafunschaf.bountiesexpanded.helper.ship.ShipUtils;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.RareFlagshipManager;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.assassination.AssassinationBountyEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter.BountyHunterEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.deserter.DeserterBountyEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.pirate.PirateBountyEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.skirmish.SkirmishBountyEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.warcriminal.WarCriminalEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.warcriminal.WarCriminalManager;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.MissionHandler;
import lombok.extern.log4j.Log4j;
import org.lazywizard.lazylib.MathUtils;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.MissionHandler.MissionType;
import static de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.MissionHandler.createNewMissionGoal;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

/**
 * A convenience class for providing supported bounties
 */
@Log4j
public class EntityProvider {
    public static final String FLEET_IDENTIFIER_KEY = "$bountiesExpanded_fleetIdentifier";
    public static final String RECENTLY_USED_FOR_BOUNTY = "$bountiesExpanded_recentlyUsedForBounty";
    private static final String NO_TARGETED_FACTION = "BountiesExpanded: failed to pick valid targeted faction";
    private static final String NO_COMMANDER = "BountiesExpanded: failed to generate fleet commander for faction '%s'";
    private static final String NO_HIDEOUT = "BountiesExpanded: failed to pick hideout";
    private static final String NO_DESTINATION = "BountiesExpanded: failed to pick destination";
    private static final String NO_FLEET = "BountiesExpanded: failed to create bounty fleet";
    private static final String NOT_IN_RANGE = "BountiesExpanded: player fleet not in range to create bounty";
    private static final String IN_RANGE = "BountiesExpanded: player fleet in range of market to create bounty";

    public static float genBountyUseTimeout() {
        return 60f + 60f * (float) Math.random();
    }

    public static void markRecentlyUsedForBounty(StarSystemAPI system) {
        if (system != null && system.getCenter() != null) {
            system.getCenter().getMemoryWithoutUpdate().set(RECENTLY_USED_FOR_BOUNTY, true, genBountyUseTimeout());
        }
    }

    public static SkirmishBountyEntity skirmishBountyEntity() {
        Difficulty difficulty = Difficulty.randomDifficulty();
        int level = Math.max(LevelPicker.pickLevel(0) + difficulty.getModifier(), Settings.skirmishMinTier);
        float fractionToKill = (50 - new Random().nextInt(26)) / 100f;
        float fp = FleetPointCalculator.vanillaCalculation(level);
        int bountyCredits = CreditCalculator.vanillaCalculation(level, difficulty.getMultiplier());

        FactionAPI offeringFaction = ParticipatingFactionPicker.pickFaction();
        if (!MiscFactionUtils.canFactionOfferBounties(offeringFaction)) return null;

        FactionAPI targetedFaction = HostileFactionPicker.pickParticipatingFaction(offeringFaction, Blacklists.getSkirmishBountyBlacklist(), true);
        if (isNull(targetedFaction)) {
            log.warn(NO_TARGETED_FACTION);
            return null;
        }

        PersonAPI fleetCommander = OfficerGenerator.generateOfficer(targetedFaction, level);
        if (isNull(fleetCommander)) {
            log.warn(String.format(NO_COMMANDER, targetedFaction.getDisplayName()));
            return null;
        }

        fleetCommander.setPersonality(Personalities.AGGRESSIVE);

        SectorEntityToken hideout = CoreWorldPicker.pickFactionHideout(targetedFaction);
        if (isNull(hideout)) {
            log.warn(NO_HIDEOUT);
            return null;
        }

        MarketAPI homeMarket = MarketUtils.getBestMarketForQuality(targetedFaction);
        if (isNull(homeMarket))
            homeMarket = MarketUtils.createFakeMarket(targetedFaction);
        float fleetQuality = Math.max(homeMarket.getShipQualityFactor() - 0.1f + difficulty.getMultiplier(), 0.2f);

        CampaignFleetAPI bountyFleet = FleetGenerator.createBountyFleetV2(fp, fleetQuality, homeMarket, hideout, fleetCommander);
        if (isNull(bountyFleet)) {
            log.warn(NO_FLEET);
            return null;
        }

        return new SkirmishBountyEntity(bountyCredits, offeringFaction, targetedFaction, bountyFleet, fleetCommander, hideout, fractionToKill, difficulty, level, fleetQuality);
    }

    public static AssassinationBountyEntity assassinationBountyEntity() {
        // @todo add more mission types. freighter fleet. luddic pilgrims.
        MissionHandler missionHandler = createNewMissionGoal(MissionType.ASSASSINATION);
        Difficulty difficulty = Difficulty.randomDifficulty();
        int level = Math.max(LevelPicker.pickLevel(0) + difficulty.getModifier(), Settings.assassinationMinTier);
        float fp = FleetPointCalculator.vanillaCalculation(level);
        int bountyCredits = CreditCalculator.vanillaCalculation(level, difficulty.getMultiplier());
        float rareFlagshipChance = Math.max(0f, (difficulty.getMultiplier()) * 0.5f);

        FactionAPI targetedFaction = ParticipatingFactionPicker.pickFaction(Blacklists.getSkirmishBountyBlacklist());
        if (isNull(targetedFaction)) {
            log.warn(NO_TARGETED_FACTION);
            return null;
        }

        MarketAPI homeMarket = MarketUtils.getBestMarketForQuality(targetedFaction);
        if (isNull(homeMarket))
            homeMarket = MarketUtils.createFakeMarket(targetedFaction);
        float fleetQuality = Math.max(homeMarket.getShipQualityFactor() - 0.1f + difficulty.getMultiplier(), 0.2f);

        PersonAPI fleetCommander = OfficerGenerator.generateOfficer(targetedFaction, level);
        if (isNull(fleetCommander)) {
            log.warn(String.format(NO_COMMANDER, targetedFaction.getDisplayName()));
            return null;
        }

        MarketAPI spawnLocation = CoreWorldPicker.pickSafeHideout(targetedFaction).getMarket();
        if (isNull(spawnLocation)) {
            log.warn(NO_HIDEOUT);
            return null;
        }

        MarketAPI travelDestination = CoreWorldPicker.pickSafeHideout(targetedFaction, CoreWorldPicker.getDistantMarkets((float) Settings.assassinationMinTravelDistance, spawnLocation.getPrimaryEntity())).getMarket();
        if (isNull(travelDestination)) {
            log.warn(NO_DESTINATION);
            return null;
        }

        CampaignFleetAPI bountyFleet = FleetGenerator.createBountyFleetV2(fp, fleetQuality, homeMarket, spawnLocation.getPrimaryEntity(), fleetCommander);
        if (isNull(bountyFleet)) {
            log.warn(NO_FLEET);
            return null;
        }

        if (Math.random() < rareFlagshipChance) { // 0/5/10/15 % chance to spawn (by difficulty level)
            boolean rareFlagshipAdded = RareFlagshipManager.replaceFlagship(bountyFleet);
            if (rareFlagshipAdded) {
                FleetMemberAPI flagship = bountyFleet.getFlagship();
                bountyFleet.getMemoryWithoutUpdate().set(RareFlagshipManager.RARE_FLAGSHIP_KEY, flagship);
                int flagshipFP = flagship.getFleetPointCost();
                bountyCredits += (int) (CreditCalculator.getRewardByFP(flagshipFP, difficulty.getMultiplier()) * Misc.getSizeNum(flagship.getHullSpec().getHullSize()));
                log.info(String.format("BountiesExpanded: Fleet got lucky! Added '%s' as rare flagship", flagship.getHullSpec().getHullName()));
            }
        }

        return new AssassinationBountyEntity(bountyCredits, targetedFaction, bountyFleet, fleetCommander, spawnLocation.getPrimaryEntity(), travelDestination.getPrimaryEntity(), missionHandler, difficulty, level, fleetQuality);
    }

    public static WarCriminalEntity warCriminalEntity() {
        MissionHandler missionHandler = createNewMissionGoal();
        Difficulty difficulty = Difficulty.randomDifficulty();
        int level = Math.max(LevelPicker.pickLevel(0) + difficulty.getModifier(), Settings.warCriminalMinTier);
        float fp = FleetPointCalculator.vanillaCalculation(level);
        float payoutMult = switch (missionHandler.getMissionType()) {
            case RETRIEVAL -> 0f;
            case ASSASSINATION, DESTRUCTION -> 0.1f;
            case OBLITERATION -> .3f;
        };

        int bountyCredits = CreditCalculator.vanillaCalculation(level, difficulty.getMultiplier() + payoutMult);
        float rareFlagshipChance = Math.max(0f, (difficulty.getMultiplier()) * 0.5f);

        FactionAPI offeringFaction = ParticipatingFactionPicker.pickFaction(Blacklists.getDefaultBlacklist());
        if (!MiscFactionUtils.canFactionOfferBounties(offeringFaction)) return null;

        FactionAPI targetedFaction = HostileFactionPicker.pickParticipatingFaction(offeringFaction, Blacklists.getDefaultBlacklist(), true);
        if (isNull(targetedFaction)) {
            log.warn(NO_TARGETED_FACTION);
            return null;
        }

        if (targetedFaction == offeringFaction)
            return null;

        SectorEntityToken spawnLocation = CoreWorldPicker.pickFactionHideout(targetedFaction);
        if (isNull(spawnLocation)) {
            log.warn(NO_HIDEOUT);
            return null;
        }

        // For Retrieval missions
        MarketAPI dropOffLocation = MarketUtils.getRandomFactionMarket(offeringFaction);
        if (isNull(dropOffLocation)) {
            log.warn(NO_DESTINATION);
            return null;
        }

        PersonAPI fleetCommander = OfficerGenerator.generateOfficer(targetedFaction, level);
        if (isNull(fleetCommander)) {
            log.warn(String.format(NO_COMMANDER, targetedFaction.getDisplayName()));
            return null;
        }

        MarketAPI homeMarket = MarketUtils.getBestMarketForQuality(targetedFaction);
        if (isNull(homeMarket))
            homeMarket = MarketUtils.createFakeMarket(targetedFaction);
        float fleetQuality = Math.max(homeMarket.getShipQualityFactor() - 0.1f + difficulty.getMultiplier(), 0.2f);

        CampaignFleetAPI bountyFleet = FleetGenerator.createBountyFleetV2(fp, fleetQuality, homeMarket, spawnLocation, fleetCommander);
        if (isNull(bountyFleet)) {
            log.warn(NO_FLEET);
            return null;
        }

        if (Math.random() < rareFlagshipChance) { // 0/5/10/15 % chance to spawn (by difficulty level)
            boolean rareFlagshipAdded = RareFlagshipManager.replaceFlagship(bountyFleet);
            if (rareFlagshipAdded) {
                FleetMemberAPI flagship = bountyFleet.getFlagship();
                bountyFleet.getMemoryWithoutUpdate().set(RareFlagshipManager.RARE_FLAGSHIP_KEY, flagship);
                int flagshipFP = flagship.getFleetPointCost();
                bountyCredits += (int) (CreditCalculator.getRewardByFP(flagshipFP, difficulty.getMultiplier()) * Misc.getSizeNum(flagship.getHullSpec().getHullSize()));
                log.info(String.format("BountiesExpanded: Fleet got lucky! Added '%s' as rare flagship", flagship.getHullSpec().getHullName()));
            }
        }

        Random random = new Random(bountyFleet.getId().hashCode() * 1337L);

        boolean isRareShip = bountyFleet.getMemoryWithoutUpdate().contains(RareFlagshipManager.RARE_FLAGSHIP_KEY);
        int numSMods = isRareShip ? 3 : 2;

        FleetMemberAPI flagship = bountyFleet.getFlagship();
        if (!isNull(flagship)) {
            if (missionHandler.getMissionType().equals(MissionHandler.MissionType.RETRIEVAL))
                HullModUtils.addRandomSMods(flagship, numSMods, random);

            if (flagship.getVariant().getSMods().isEmpty())
                ShipUtils.upgradeShip(flagship, numSMods, random);

            ShipUtils.addMinorUpgrades(flagship, random);

            flagship.updateStats();
        }

        numSMods = Math.max(0, difficulty.getModifier());
        FleetUpgradeHelper.upgradeRandomShips(bountyFleet, numSMods, difficulty.getMultiplier(), true, random);

        return new WarCriminalEntity(bountyCredits, level, fleetQuality, difficulty, targetedFaction, offeringFaction, bountyFleet, fleetCommander, spawnLocation, dropOffLocation.getPrimaryEntity(), missionHandler);
    }

    public static PirateBountyEntity pirateBountyEntity() {
        MissionHandler missionHandler = createNewMissionGoal(MissionType.ASSASSINATION);
        Difficulty difficulty = Difficulty.randomDifficulty();
        int level = Math.max(LevelPicker.pickLevel(0) + difficulty.getModifier(), Settings.pirateMinTier);
        float fp = FleetPointCalculator.vanillaCalculation(level);
        int bountyCredits = CreditCalculator.vanillaCalculation(level, difficulty.getMultiplier());
        float rareFlagshipChance = Math.max(0f, (difficulty.getMultiplier()) * 0.5f);
        float fleetQuality = difficulty.getMultiplier() + 0.1f;

        Set<String> defaultBlacklist = Blacklists.getDefaultBlacklist();
        defaultBlacklist.add(Factions.PIRATES);

        FactionAPI offeringFaction = ParticipatingFactionPicker.pickFaction(defaultBlacklist);
        if (!MiscFactionUtils.canFactionOfferBounties(offeringFaction)) return null;

        FactionAPI targetedFaction = Global.getSector().getFaction(Factions.PIRATES);
        if (isNull(targetedFaction)) {
            log.warn(NO_TARGETED_FACTION);
            return null;
        }

        SectorEntityToken spawnLocation;
        if (fp > 180) {
            spawnLocation = RemoteWorldPicker.pickRandomHideout(TagCollection.VANILLA_BOUNTY_SYSTEM_TAGS, true);
        }
        else if (fp > 120) {
            spawnLocation = RemoteWorldPicker.pickRandomHideout(TagCollection.VANILLA_BOUNTY_SYSTEM_TAGS, true, 24);
        }
        else if (fp > 60) {
            spawnLocation = RemoteWorldPicker.pickRandomHideout(TagCollection.VANILLA_BOUNTY_SYSTEM_TAGS, true, 18);
        }
        else {
            spawnLocation = RemoteWorldPicker.pickRandomHideout(TagCollection.VANILLA_BOUNTY_SYSTEM_TAGS, true, 13);
        }

        if (isNull(spawnLocation)) {
            log.warn(NO_HIDEOUT);
            return null;
        }

        PersonAPI fleetCommander = OfficerGenerator.generateOfficer(targetedFaction, level);
        if (isNull(fleetCommander)) {
            log.warn(String.format(NO_COMMANDER, targetedFaction.getDisplayName()));
            return null;
        }
        fleetCommander.setRankId(Ranks.SPACE_CAPTAIN);

        CampaignFleetAPI bountyFleet = FleetGenerator.createBountyFleetV2(fp, fleetQuality, null, spawnLocation, fleetCommander);
        if (isNull(bountyFleet)) {
            log.warn(NO_FLEET);
            return null;
        }

        if (Math.random() < rareFlagshipChance) { // 0/5/10/15 % chance to spawn (by difficulty level)
            boolean rareFlagshipAdded = RareFlagshipManager.replaceFlagship(bountyFleet);
            if (rareFlagshipAdded) {
                FleetMemberAPI flagship = bountyFleet.getFlagship();
                bountyFleet.getMemoryWithoutUpdate().set(RareFlagshipManager.RARE_FLAGSHIP_KEY, flagship);
                int flagshipFP = flagship.getFleetPointCost();
                bountyCredits += (int) (CreditCalculator.getRewardByFP(flagshipFP, difficulty.getMultiplier()) * Misc.getSizeNum(flagship.getHullSpec().getHullSize()));
                log.info(String.format("BountiesExpanded: Fleet got lucky! Added '%s' as rare flagship", flagship.getHullSpec().getHullName()));
            }
        }

        return new PirateBountyEntity(bountyCredits, level, fleetQuality, difficulty, offeringFaction, bountyFleet, fleetCommander, spawnLocation, missionHandler);
    }

    public static DeserterBountyEntity deserterBountyEntity() {
        MissionHandler missionHandler = createNewMissionGoal(MissionType.ASSASSINATION);
        Difficulty difficulty = Difficulty.randomDifficulty();
        int level = Math.max(LevelPicker.pickLevel(0) + difficulty.getModifier(), Settings.deserterMinTier);
        float fp = FleetPointCalculator.vanillaCalculation(level);
        int bountyCredits = CreditCalculator.vanillaCalculation(level, difficulty.getMultiplier());
        float rareFlagshipChance = Math.max(0f, (difficulty.getMultiplier()) * 0.5f);

        FactionAPI offeringFaction = ParticipatingFactionPicker.pickFaction(Blacklists.getDefaultBlacklist());
        if (!MiscFactionUtils.canFactionOfferBounties(offeringFaction))
            return null;

        SectorEntityToken spawnLocation = CoreWorldPicker.pickFactionHideout(offeringFaction);
        if (isNull(spawnLocation)) {
            log.warn(NO_HIDEOUT);
            return null;
        }

        MarketAPI homeMarket = MarketUtils.getBestMarketForQuality(offeringFaction);
        if (isNull(homeMarket))
            homeMarket = MarketUtils.createFakeMarket(offeringFaction);
        float fleetQuality = Math.max(homeMarket.getShipQualityFactor() - 0.1f + difficulty.getMultiplier(), 0.2f);

        SectorEntityToken travelDestination;
        if (fp > 140) {
            travelDestination = RemoteWorldPicker.pickRandomHideout(TagCollection.VANILLA_BOUNTY_SYSTEM_TAGS, false);
        }
        else if (fp > 80) {
            travelDestination = RemoteWorldPicker.pickRandomHideout(TagCollection.VANILLA_BOUNTY_SYSTEM_TAGS, false, 20);
        }
        else {
            travelDestination = RemoteWorldPicker.pickRandomHideout(TagCollection.VANILLA_BOUNTY_SYSTEM_TAGS, false, 16);
        }
        if (isNull(travelDestination)) {
            log.warn(NO_DESTINATION);
            return null;
        }

        PersonAPI fleetCommander = OfficerGenerator.generateOfficer(offeringFaction, level);
        if (isNull(fleetCommander)) {
            log.warn(String.format(NO_COMMANDER, offeringFaction.getDisplayName()));
            return null;
        }

        CampaignFleetAPI bountyFleet = FleetGenerator.createBountyFleetV2(fp, fleetQuality, null, spawnLocation, fleetCommander, offeringFaction, true);
        if (isNull(bountyFleet)) {
            log.warn(NO_FLEET);
            return null;
        }

        if (Math.random() < rareFlagshipChance) { // 0/5/10/15 % chance to spawn (by difficulty level)
            boolean rareFlagshipAdded = RareFlagshipManager.replaceFlagship(bountyFleet);
            if (rareFlagshipAdded) {
                FleetMemberAPI flagship = bountyFleet.getFlagship();
                bountyFleet.getMemoryWithoutUpdate().set(RareFlagshipManager.RARE_FLAGSHIP_KEY, flagship);
                int flagshipFP = flagship.getFleetPointCost();
                bountyCredits += (int) (CreditCalculator.getRewardByFP(flagshipFP, difficulty.getMultiplier()) * Misc.getSizeNum(flagship.getHullSpec().getHullSize()));
                log.info(String.format("BountiesExpanded: Fleet got lucky! Added '%s' as rare flagship", flagship.getHullSpec().getHullName()));
            }
        }

        return new DeserterBountyEntity(bountyCredits, level, fleetQuality, difficulty, offeringFaction, bountyFleet, fleetCommander, spawnLocation, travelDestination, missionHandler);
    }

    public static BountyHunterEntity bountyHunterEntity() {
        MissionHandler missionHandler = createNewMissionGoal(MissionType.DESTRUCTION);
        Difficulty difficulty = Difficulty.randomDifficulty();
        int level = Math.max(LevelPicker.pickLevel(1) + difficulty.getModifier(), Settings.bountyHunterMinTier);
        float fp = FleetPointCalculator.vanillaCalculation(level);
        float rareFlagshipChance = Math.max(0f, (difficulty.getMultiplier()) * 0.5f);
        float fleetQuality = difficulty.getModifier() + 0.7f;

        // Check if player is within 16 light years of a market.
        float rangeToShowBounties = 16f;
        boolean withinRange = false;
        if (!Global.getSector().getPlayerFleet().getContainingLocation().hasTag(Tags.THEME_HIDDEN)) {
            List<StarSystemAPI> nearbyStarSystems = Misc.getNearbyStarSystems(Global.getSector().getPlayerFleet(), rangeToShowBounties);
            for (StarSystemAPI system : nearbyStarSystems) {
                List<MarketAPI> markets = Misc.getMarketsInLocation(system);
                for (MarketAPI market : markets) {
                    if (!market.isHidden() && market.getSize() > 3) {
                        withinRange = true;
                        log.info(IN_RANGE);
                        break;
                    }
                }
                if (withinRange) {
                    break;
                }
            }
        }

        if (!withinRange) {
            log.info(NOT_IN_RANGE);
            return null;
        }

        Set<String> defaultBlacklist = Blacklists.getDefaultBlacklist();

        FactionAPI offeringFaction = BountyHunterFactionPicker.pickFaction(defaultBlacklist);
        if (isNull(offeringFaction)) {
            log.warn(NO_TARGETED_FACTION);
            return null;
        }
        if (!MiscFactionUtils.canFactionOfferBounties(offeringFaction)) {
            log.info(String.format("BountiesExpanded: offering faction %s cannot create bounties.", offeringFaction.getId()));
            return null;
        }

        SectorEntityToken spawnLocation = CoreWorldPicker.pickFactionHideout(offeringFaction);

        if (isNull(spawnLocation)) {
            log.warn(NO_HIDEOUT);
            return null;
        }

        PersonAPI fleetCommander;
        PersonAPI offeringPerson = null;
        if (offeringFaction.getRelToPlayer().isHostile()) {
            offeringPerson = BountyGiverGenerator.generateBountyGiver(spawnLocation.getMarket());
            fleetCommander = OfficerGenerator.generateOfficer(offeringFaction, level);
        }
        else {
            if (MathUtils.getRandomNumberInRange(0, 100) <= 20) {
                fleetCommander = OfficerGenerator.generateOfficer(offeringFaction, level);
            } else {
                fleetCommander = OfficerGenerator.generateOfficer(Global.getSector().getFaction(Factions.MERCENARY), level);
                fleetCommander.setRankId(Ranks.SPACE_CAPTAIN);
            }
        }

        if (isNull(fleetCommander)) {
            log.warn(NO_COMMANDER);
            return null;
        }

        CampaignFleetAPI bountyFleet = FleetGenerator.createBountyFleetV2(fp, fleetQuality, null, spawnLocation, fleetCommander, fleetCommander.getFaction(), FleetTypes.MERC_BOUNTY_HUNTER);
        if (isNull(bountyFleet)) {
            log.warn(NO_FLEET);
            return null;
        }
        if (offeringFaction.getRelToPlayer().isHostile()) {
            bountyFleet.setName(String.format("%s Bounty Hunter", offeringFaction.getDisplayName()));
        }
        else {
            bountyFleet.setName("Bounty Hunter");
        }

        if (Math.random() < rareFlagshipChance) { // 0/5/10/15 % chance to spawn (by difficulty level)
            boolean rareFlagshipAdded = RareFlagshipManager.replaceFlagship(bountyFleet);
            if (rareFlagshipAdded) {
                FleetMemberAPI flagship = bountyFleet.getFlagship();
                bountyFleet.getMemoryWithoutUpdate().set(RareFlagshipManager.RARE_FLAGSHIP_KEY, flagship);
                log.info(String.format("BountiesExpanded: Fleet got lucky! Added '%s' as rare flagship", flagship.getHullSpec().getHullName()));
            }
        }

        log.info("BountiesExpanded: got to end");

        return new BountyHunterEntity(offeringFaction, bountyFleet, offeringPerson, spawnLocation, difficulty, level, fleetQuality, missionHandler);

    }

}