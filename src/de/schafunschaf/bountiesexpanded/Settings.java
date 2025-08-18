package de.schafunschaf.bountiesexpanded;

import de.schafunschaf.bountiesexpanded.helper.ModInitHelper;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

public class Settings implements LunaSettingsListener {
    private static final String modId = "bountiesexpanded";

    public static int baseReward = LunaSettings.getInt(modId, "baseReward");
    public static int rewardPerTier = LunaSettings.getInt(modId, "rewardPerTier");
    public static float chanceEasy = LunaSettings.getFloat(modId, "chanceEasy");
    public static float chanceMedium = LunaSettings.getFloat(modId, "chanceMedium");
    public static float chanceChallenging = LunaSettings.getFloat(modId, "chanceChallenging");
    public static float chanceHard = LunaSettings.getFloat(modId, "chanceHard");

    public static boolean skirmishActive = LunaSettings.getBoolean(modId, "skirmishActive");
    public static double skirmishSpawnChance = LunaSettings.getDouble(modId, "skirmishSpawnChance");
    public static int skirmishMinBounties = LunaSettings.getInt(modId, "skirmishMinBounties");
    public static int skirmishMaxBounties = LunaSettings.getInt(modId, "skirmishMaxBounties");
    public static int skirmishMinDuration = LunaSettings.getInt(modId, "skirmishMinDuration");
    public static int skirmishMaxDuration = LunaSettings.getInt(modId, "skirmishMaxDuration");
    public static int skirmishBaseShipBounty = LunaSettings.getInt(modId, "skirmishBaseShipBounty");

    public static boolean assassinationActive = LunaSettings.getBoolean(modId, "assassinationActive");
    public static double assassinationSpawnChance = LunaSettings.getDouble(modId, "assassinationSpawnChance");
    public static int assassinationMinBounties = LunaSettings.getInt(modId, "assassinationMinBounties");
    public static int assassinationMaxBounties = LunaSettings.getInt(modId, "assassinationMaxBounties");
    public static double assassinationMinTravelDistance = LunaSettings.getDouble(modId, "assassinationMinTravelDistance");
    public static double assassinationBaseRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBaseRewardMultiplier");
    public static double assassinationBonusRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBonusRewardMultiplier");
    public static int assassinationMinTier = LunaSettings.getInt(modId, "assassinationMinTier");

    public static boolean warCriminalActive = LunaSettings.getBoolean(modId, "warCriminalActive");
    public static double warCriminalSpawnChance = LunaSettings.getDouble(modId, "warCriminalSpawnChance");
    public static int warCriminalMinBounties = LunaSettings.getInt(modId, "warCriminalMinBounties");
    public static int warCriminalMaxBounties = LunaSettings.getInt(modId, "warCriminalMaxBounties");
    public static int warCriminalMinDuration = LunaSettings.getInt(modId, "warCriminalMinDuration");
    public static int warCriminalMaxDuration = LunaSettings.getInt(modId, "warCriminalMaxDuration");
    public static int warCriminalMinTier = LunaSettings.getInt(modId, "warCriminalMinTier");

    public static boolean pirateBountyActive = LunaSettings.getBoolean(modId, "pirateBountyActive");
    public static double pirateBountySpawnChance = LunaSettings.getDouble(modId, "pirateBountySpawnChance");
    public static int pirateBountyMinBounties = LunaSettings.getInt(modId, "pirateBountyMinBounties");
    public static int pirateBountyMaxBounties = LunaSettings.getInt(modId, "pirateBountyMaxBounties");
    public static int pirateBountyMinDuration = LunaSettings.getInt(modId, "pirateBountyMinDuration");
    public static int pirateBountyMaxDuration = LunaSettings.getInt(modId, "pirateBountyMaxDuration");
    public static int pirateMinTier = LunaSettings.getInt(modId, "pirateBountyMinTier");

    public static boolean deserterBountyActive = LunaSettings.getBoolean(modId, "deserterBountyActive");
    public static double deserterBountySpawnChance = LunaSettings.getDouble(modId, "deserterBountySpawnChance");
    public static int deserterBountyMinBounties = LunaSettings.getInt(modId, "deserterBountyMinBounties");
    public static int deserterBountyMaxBounties = LunaSettings.getInt(modId, "deserterBountyMaxBounties");
    public static int deserterBountyMinDuration = LunaSettings.getInt(modId, "deserterBountyMinDuration");
    public static int deserterBountyMaxDuration = LunaSettings.getInt(modId, "deserterBountyMaxDuration");
    public static int deserterMinTier = LunaSettings.getInt(modId, "deserterBountyMinTier");

    public static boolean bountyHunterActive = LunaSettings.getBoolean(modId, "bountyHunterActive");
    public static double bountyHunterSpawnChance = LunaSettings.getDouble(modId, "bountyHunterSpawnChance");
    public static int bountyHunterMinBounties = LunaSettings.getInt(modId, "bountyHunterMinBounties");
    public static int bountyHunterMaxBounties = LunaSettings.getInt(modId, "bountyHunterMaxBounties");
    public static int bountyHunterMinTier = LunaSettings.getInt(modId, "bountyHunterMinTier");

    public static boolean debug = LunaSettings.getBoolean(modId, "debug");
    public static boolean disableVanillaBounties = LunaSettings.getBoolean(modId, "disableVanillaBounties");
    public static boolean retrievalEventActive = LunaSettings.getBoolean(modId, "retrievalEventActive");
    public static int retrievalEventDuration = LunaSettings.getInt(modId, "retrievalEventDuration");
    public static boolean triggeredEventsActive = LunaSettings.getBoolean(modId, "retrievalEventActive");
    public static boolean onlyRecoverWithSP = false;
    public static boolean prepareUpdate = false;
    public static boolean ignorePlayerMarkets = true;

    @Override
    public void settingsChanged(String modID) {
        baseReward = LunaSettings.getInt(modId, "baseReward");
        rewardPerTier = LunaSettings.getInt(modId, "rewardPerTier");
        chanceEasy = LunaSettings.getFloat(modId, "chanceEasy");
        chanceMedium = LunaSettings.getFloat(modId, "chanceMedium");
        chanceChallenging = LunaSettings.getFloat(modId, "chanceChallenging");
        chanceHard = LunaSettings.getFloat(modId, "chanceHard");

        skirmishActive = LunaSettings.getBoolean(modId, "skirmishActive");
        skirmishSpawnChance = LunaSettings.getDouble(modId, "skirmishSpawnChance");
        skirmishMinBounties = LunaSettings.getInt(modId, "skirmishMinBounties");
        skirmishMaxBounties = LunaSettings.getInt(modId, "skirmishMaxBounties");
        skirmishMinDuration = LunaSettings.getInt(modId, "skirmishMinDuration");
        skirmishMaxDuration = LunaSettings.getInt(modId, "skirmishMaxDuration");
        skirmishBaseShipBounty = LunaSettings.getInt(modId, "skirmishBaseShipBounty");

        assassinationActive = LunaSettings.getBoolean(modId, "assassinationActive");
        assassinationSpawnChance = LunaSettings.getDouble(modId, "assassinationSpawnChance");
        assassinationMinBounties = LunaSettings.getInt(modId, "assassinationMinBounties");
        assassinationMaxBounties = LunaSettings.getInt(modId, "assassinationMaxBounties");
        assassinationMinTravelDistance = LunaSettings.getDouble(modId, "assassinationMinTravelDistance");
        assassinationBaseRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBaseRewardMultiplier");
        assassinationBonusRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBonusRewardMultiplier");
        assassinationMinTier = LunaSettings.getInt(modId, "assassinationMinTier");

        warCriminalActive = LunaSettings.getBoolean(modId, "warCriminalActive");
        warCriminalSpawnChance = LunaSettings.getDouble(modId, "warCriminalSpawnChance");
        warCriminalMinBounties = LunaSettings.getInt(modId, "warCriminalMinBounties");
        warCriminalMaxBounties = LunaSettings.getInt(modId, "warCriminalMaxBounties");
        warCriminalMinDuration = LunaSettings.getInt(modId, "warCriminalMinDuration");
        warCriminalMaxDuration = LunaSettings.getInt(modId, "warCriminalMaxDuration");
        warCriminalMinTier = LunaSettings.getInt(modId, "warCriminalMinTier");

        pirateBountyActive = LunaSettings.getBoolean(modId, "pirateBountyActive");
        pirateBountySpawnChance = LunaSettings.getDouble(modId, "pirateBountySpawnChance");
        pirateBountyMinBounties = LunaSettings.getInt(modId, "pirateBountyMinBounties");
        pirateBountyMaxBounties = LunaSettings.getInt(modId, "pirateBountyMaxBounties");
        pirateBountyMinDuration = LunaSettings.getInt(modId, "pirateBountyMinDuration");
        pirateBountyMaxDuration = LunaSettings.getInt(modId, "pirateBountyMaxDuration");
        pirateMinTier = LunaSettings.getInt(modId, "pirateBountyMinTier");

        deserterBountyActive = LunaSettings.getBoolean(modId, "deserterBountyActive");
        deserterBountySpawnChance = LunaSettings.getDouble(modId, "deserterBountySpawnChance");
        deserterBountyMinBounties = LunaSettings.getInt(modId, "deserterBountyMinBounties");
        deserterBountyMaxBounties = LunaSettings.getInt(modId, "deserterBountyMaxBounties");
        deserterBountyMinDuration = LunaSettings.getInt(modId, "deserterBountyMinDuration");
        deserterBountyMaxDuration = LunaSettings.getInt(modId, "deserterBountyMaxDuration");
        deserterMinTier = LunaSettings.getInt(modId, "deserterMinTier");

        bountyHunterActive = LunaSettings.getBoolean(modId, "bountyHunterActive");
        bountyHunterSpawnChance = LunaSettings.getDouble(modId, "bountyHunterSpawnChance");
        bountyHunterMinBounties = LunaSettings.getInt(modId, "bountyHunterMinBounties");
        bountyHunterMaxBounties = LunaSettings.getInt(modId, "bountyHunterMaxBounties");
        bountyHunterMinTier = LunaSettings.getInt(modId, "bountyHunterMinTier");

        debug = LunaSettings.getBoolean(modId, "debug");
        disableVanillaBounties = LunaSettings.getBoolean(modId, "disableVanillaBounties");
        retrievalEventActive = LunaSettings.getBoolean(modId, "retrievalEventActive");
        retrievalEventDuration = LunaSettings.getInt(modId, "retrievalEventDuration");
        triggeredEventsActive = LunaSettings.getBoolean(modId, "retrievalEventActive");
    }

    public static boolean isDebugActive() {
        return LunaSettings.getBoolean(modId, "debug");
    }
}
