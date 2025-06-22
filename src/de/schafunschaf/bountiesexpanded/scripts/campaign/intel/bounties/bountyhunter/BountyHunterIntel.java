package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.EncounterOption;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetGenerator;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BaseBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyResult;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyResultType;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyType;
import lombok.Getter;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Set;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

@Getter
public class BountyHunterIntel extends BaseBountyIntel {

    //Based on VengeanceFleetIntel from Nexerelin

    public static final FleetAssignment TRAIL_ASSIGNMENT = FleetAssignment.DELIVER_CREW;

    private final BountyHunterEntity bountyHunterEntity;
    protected String factionId;
    protected MarketAPI market;
    protected boolean assembling = true;
    protected float daysToLaunch;
    protected final float daysToLaunchFixed;
    protected float daysLeft;
    protected boolean foundPlayerYet = false;
    protected final IntervalUtil interval = new IntervalUtil(0.4f, 0.6f);
    protected final IntervalUtil interval2 = new IntervalUtil(1f, 2f);
    protected final IntervalUtil locationTokenInterval = new IntervalUtil(5f, 15f);
    protected float timeSpentLooking = 0f;
    protected boolean trackingMode = false;
    protected SectorEntityToken locationToken;

    public BountyHunterIntel(BountyHunterEntity bountyHunterEntity, CampaignFleetAPI campaignFleetAPI, PersonAPI personAPI, SectorEntityToken spawnLocation, SectorEntityToken travelDestination) {
        super(BountyType.BOUNTY_HUNTER, bountyHunterEntity, bountyHunterEntity.getMissionHandler(), campaignFleetAPI, personAPI, spawnLocation, travelDestination);
        Misc.makeImportant(fleet, "pbe");
        this.bountyHunterEntity = bountyHunterEntity;
        bountyHunterEntity.setIntel(this);
        this.factionId = bountyHunterEntity.getOfferingFaction().getId();
        this.market = bountyHunterEntity.getSpawnLocation().getMarket();
        this.setImportant(true);

        if (fleet.getFleetPoints() >= 0 && fleet.getFleetPoints() <= 50) {
            daysToLaunch = 7f;
        } else if (fleet.getFleetPoints() >= 51 && fleet.getFleetPoints() <= 80) {
            daysToLaunch = 15f;
        } else if (fleet.getFleetPoints() >= 81 && fleet.getFleetPoints() <= 120) {
            daysToLaunch = 20f;
        } else if (fleet.getFleetPoints() >= 121 && fleet.getFleetPoints() <= 180) {
            daysToLaunch = 35f;
        } else {
            daysToLaunch = 50f;
        }

        this.daysToLaunch = Math.round(daysToLaunch * MathUtils.getRandomNumberInRange(0.9f, 1.1f));
        this.daysToLaunchFixed = daysToLaunch;

        float distance = Misc.getDistanceToPlayerLY(spawnLocation);
        float distBonus = 30 + distance*1.5f;	// don't crank it up too much, I don't think this is the important component

        if (fleet.getFleetPoints() >= 0 && fleet.getFleetPoints() <= 80) {
            duration = Math.max(60, Math.min(90,
                            Math.round(distBonus * MathUtils.getRandomNumberInRange(0.75f, 1f))));

        } else if (fleet.getFleetPoints() >= 81 && fleet.getFleetPoints() <= 180) {
            duration = Math.max(90, Math.min(120,
                    Math.round(distBonus * MathUtils.getRandomNumberInRange(1.25f, 1.75f))));

        } else {
            duration = Math.max(120, Math.min(150,
                    Math.round(distBonus * MathUtils.getRandomNumberInRange(2f, 2.5f))));
        }
        // need to add days to launch to duration so bounty isn't over as soon as it launches.
        this.duration += daysToLaunchFixed;

        this.daysLeft = duration;
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        boolean isDone = isDone() || isNotNull(result);
        boolean isNotInvolved = !battle.isPlayerInvolved() || !battle.isInvolved(fleet) || battle.onPlayerSide(fleet);
        boolean isFlagshipAlive = isNotNull(fleet.getFlagship()) && fleet.getFlagship().getCaptain() == person;

        if (isDone || isNotInvolved || isFlagshipAlive) {
            return;
        }

        if (battle.isInvolved(fleet) && !battle.isPlayerInvolved()) {
            if (isNull(fleet.getFlagship()) || fleet.getFlagship().getCaptain() != person) {
                fleet.setCommander(fleet.getFaction().createRandomPerson());
                result = new BountyResult(BountyResultType.END_PLAYER_NO_REWARD, 0, 0, 0);
                cleanUp(false);
                return;
            }
        }

        result = new BountyResult(BountyResultType.END_PLAYER_NO_REWARD, 0, 0, 0);
        SharedData.getData().getPersonBountyEventData().reportSuccess();
        cleanUp(false);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> intelTags = super.getIntelTags(map);
        intelTags.add(factionId);
        return intelTags;
    }

    @Override
    protected float getBaseDaysAfterEnd() {
        return 7f;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (assembling && getSpawnLocation().isVisibleToPlayerFleet()) return market.getPrimaryEntity();
        return null;
    }

    protected void handleFleetAssignment(CampaignFleetAPI playerFleet) {
        // commented here due to call to updateLocationToken in advanceImpl?
        //updateLocationToken();

        boolean playerVisible = false;
        boolean fleetVisible = false;
        if (fleet.getContainingLocation().equals(playerFleet.getContainingLocation())) {
            playerVisible = playerFleet.isVisibleToSensorsOf(fleet);
            fleetVisible = fleet.isVisibleToSensorsOf(playerFleet);
        }
        if (playerVisible && fleetVisible) {
            foundPlayerYet = true;
        }

        // player and enemy fleet are close to each other, deactivate tracking mode
        if (trackingMode && fleet.getContainingLocation().equals(playerFleet.getContainingLocation())) {
            var detectRange = (
                    1.5f * Math.max(
                            fleet.getMaxSensorRangeToDetect(playerFleet),
                            playerFleet.getMaxSensorRangeToDetect(fleet)
                    ));

            if (Misc.getDistance(fleet.getLocation(), playerFleet.getLocation()) <= 1000f + detectRange) {
                trackingMode = false;
            }
        }

        // added by rat.
        if (fleet.getContainingLocation().equals(playerFleet.getContainingLocation())) {
            //2f, originaly 1.5f
            var detectRange = (
                    2f * Math.max(
                            fleet.getMaxSensorRangeToDetect(playerFleet),
                            playerFleet.getMaxSensorRangeToDetect(fleet)
                    ));

            if (Misc.getDistance(fleet.getLocation(), playerFleet.getLocation()) <= 1000f + detectRange) {
                foundPlayerYet = true; //Set to true to cause the fleet to not home on to the player.
            }
        }

        // my understanding of tracking mode:
        // it activates when player [has been encountered at least once or is currently visible], and [is no longer in same system]
        // while tracking mode is active, fleet can travel to player without an actual sensor lock
        // tracking mode is deactivated once the two fleets are in same location again and get sufficiently close

        EncounterOption option = fleet.getAI().pickEncounterOption(null, playerFleet);
        if (option == EncounterOption.ENGAGE || option == EncounterOption.HOLD_VS_STRONGER) {
            // can see player or has encountered player at least once
            if (playerVisible || foundPlayerYet) {
                // in same system but not currently in tracking mode, look around the system?
                // this means that if fleet has been shaken once,
                // as long as player doesn't leave the system, player is safe
                if (fleet.getContainingLocation().equals(playerFleet.getContainingLocation()) && !trackingMode) {
                    if (fleet.getAI().getCurrentAssignmentType() != FleetAssignment.PATROL_SYSTEM) {
                        fleet.clearAssignments();
                        fleet.addAssignment(FleetAssignment.PATROL_SYSTEM,
                                locationToken,
                                1000f,
                                "hunting your fleet");
                        fleet.getAbility(Abilities.EMERGENCY_BURN).activate();
                        ((ModularFleetAIAPI) fleet.getAI()).getTacticalModule().setPriorityTarget(playerFleet, 1000,
                                false);
                    }
                    // not in same system, or currently tracking; activate tracking mode
                } else {
                    trackingMode = true;
                    if (fleet.getContainingLocation().equals(playerFleet.getContainingLocation())) {
                        // tracking in same system, intercept
                        // 0.95: intercept only works if we can see player
                        // don't think it's possible to have sight on the player without tracking mode turning off
                        if (playerVisible) {
                            if (fleet.getAI().getCurrentAssignmentType() != FleetAssignment.INTERCEPT) {
                                fleet.clearAssignments();
                                fleet.addAssignment(FleetAssignment.INTERCEPT, playerFleet, 1000,
                                        "intercepting your fleet");
                            }
                        } else {
                            if (fleet.getAI().getCurrentAssignmentType() != FleetAssignment.ATTACK_LOCATION) {
                                fleet.clearAssignments();
                                fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, locationToken, 1000,
                                        "hunting your fleet");
                                ((ModularFleetAIAPI) fleet.getAI()).getTacticalModule().setPriorityTarget(playerFleet, 1000,
                                        false);
                            }
                        }

                    } else {
                        // player not in same system, maphack our way to player
                        if (fleet.getAI().getCurrentAssignmentType() != TRAIL_ASSIGNMENT) {
                            fleet.clearAssignments();
                            fleet.addAssignment(TRAIL_ASSIGNMENT, locationToken, 1000,
                                    "trailing your fleet");
                        }
                    }
                }
            }
            // can't see player now and haven't seen player at least once; long-distance maphack
            else {
                if (fleet.getAI().getCurrentAssignmentType() != TRAIL_ASSIGNMENT) {
                    fleet.clearAssignments();
                    fleet.addAssignment(TRAIL_ASSIGNMENT, locationToken, 1000,
                            "trailing your fleet");
                }
            }
        } else {
            result = new BountyResult(BountyResultType.END_PLAYER_NO_REWARD, 0, 0, 0);
            cleanUp(false);
            return;
        }

        if (!fleetVisible || !playerVisible) {
            if (daysLeft <= 0f) {
                result = new BountyResult(BountyResultType.END_TIME, 0, 0, 0);
                cleanUp(false);
            }
        }
    }

    @Override
    public void advanceImpl(float amount) {
        if (isEnded()) {
            return;
        }

//        if (getFaction().isAtWorst(Factions.PLAYER, RepLevel.INHOSPITABLE)) {
//            endEvent(EndReason.NO_LONGER_HOSTILE);
//            return;
//        }

        if (assembling) {
            if (!market.getFactionId().equals(factionId)) {
                result = new BountyResult(BountyResultType.END_OTHER, 0, 0, 0);
                cleanUp(false);
                return;
            }

            if (!hasWorkingSpaceport(market)) {
                result = new BountyResult(BountyResultType.END_OTHER, 0, 0, 0);
                cleanUp(false);
                return;
            }

            daysToLaunch -= Global.getSector().getClock().convertToDays(amount);
            if (daysToLaunch < 0) {
                assembling = false;
                activateFleet();
            }
            return;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return;
        }

        if (!fleet.isAlive()) {
            result = new BountyResult(BountyResultType.END_PLAYER_NO_REWARD, 0, 0, 0);
            cleanUp(false);
            return;
        }

        // fleet took too many losses, quit
        if (fleet.getMemoryWithoutUpdate().contains("$startingFP") && fleet.getFleetPoints() < 0.4 * fleet.getMemoryWithoutUpdate().getFloat("$startingFP")) {
            result = new BountyResult(BountyResultType.END_PLAYER_NO_REWARD, 0, 0, 0);
            cleanUp(false);
            return;
        }

        /* Advance faster and faster if they lost you */
        float days = Global.getSector().getClock().convertToDays(amount);
        if (foundPlayerYet) {
            timeSpentLooking += days;
            daysLeft -= days * (2f + (timeSpentLooking / duration));
        } else {
            daysLeft -= days;
        }
        interval.advance(days);
        interval2.advance(days);

        if (interval2.intervalElapsed()) {
            if (fleet.getAI().getCurrentAssignmentType() == FleetAssignment.PATROL_SYSTEM &&
                    ((ModularFleetAIAPI) fleet.getAI()).getTacticalModule().getTarget() != playerFleet) {
                ((ModularFleetAIAPI) fleet.getAI()).getTacticalModule().setPriorityTarget(playerFleet, 1000, false);
                ((ModularFleetAIAPI) fleet.getAI()).getTacticalModule().setTarget(playerFleet);
            }
        }

        locationTokenInterval.advance(amount);
        if (locationTokenInterval.intervalElapsed()) {
            updateLocationToken();
        }

        if (!interval.intervalElapsed()) {
            return;
        }

        handleFleetAssignment(playerFleet);
    }

    public void activateFleet() {
        FleetGenerator.spawnFleet(fleet, market.getPrimaryEntity());
        sendUpdateIfPlayerHasIntel(null, false);
        fleet.getAI().clearAssignments();

        final MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();

        fleetMemory.set("$startingFP", fleet.getFleetPoints());
        fleetMemory.set("$clearCommands_no_remove", true);

        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE,
                market.getPrimaryEntity(),
                2f + (float) Math.random() * 2f,
                "orbiting " + market.getName());

        //fleet.getStats().getFleetwideMaxBurnMod().modifyFlat("bounty_fleet", -1f);

        fleetMemory.set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
        //fleetMemory.set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);

        if (!fleet.getFaction().getRelToPlayer().isHostile()) {
            fleetMemory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
            fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        }
    }

    /**
     * Updates the token used to track player fleet location when player is out of sight.
     * Normally (since 0.95) fleets don't know the player's containing location, unless
     * player was seen jumping.
     */
    public void updateLocationToken() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getContainingLocation() == null)
            return;

        Vector2f loc = player.getLocation();
        if (locationToken == null) {
            locationToken = player.getContainingLocation().createToken(loc);
        }
        if (locationToken.getContainingLocation() != player.getContainingLocation()) {
            // since we can't change its system, just rebuild it
            locationToken = player.getContainingLocation().createToken(loc);
            if (fleet != null) fleet.clearAssignments();
        }
        locationToken.setLocation(loc.x, loc.y);
    }

    public static boolean hasWorkingSpaceport(MarketAPI market) {
        for (Industry ind : market.getIndustries())
        {
            if (!ind.getSpec().hasTag(Industries.TAG_SPACEPORT))
                continue;
            if (ind.isDisrupted()) continue;
            return true;
        }
        return false;
    }

    public Object getListInfo() {
        return getListInfoParam();
    }
}
