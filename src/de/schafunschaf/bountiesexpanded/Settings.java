package de.schafunschaf.bountiesexpanded;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

public class Settings implements LunaSettingsListener {
    private static final String modId = "bountiesexpanded";

    public static int baseRewardPerFP = LunaSettings.getInt(modId, "baseRewardPerFP");
    public static float chanceEasy = LunaSettings.getFloat(modId, "chanceEasy");
    public static float chanceMedium = LunaSettings.getFloat(modId, "chanceMedium");
    public static float chanceChallenging = LunaSettings.getFloat(modId, "chanceChallenging");
    public static float chanceHard = LunaSettings.getFloat(modId, "chanceHard");
    public static float multiplierEasy = LunaSettings.getFloat(modId, "multiplierEasy");
    public static float multiplierMedium = LunaSettings.getFloat(modId, "multiplierMedium");
    public static float multiplierChallenging = LunaSettings.getFloat(modId, "multiplierChallenging");
    public static float multiplierHard = LunaSettings.getFloat(modId, "multiplierHard");

    public static boolean skirmishActive = LunaSettings.getBoolean(modId, "skirmishActive");
    public static double skirmishSpawnChance = LunaSettings.getDouble(modId, "skirmishSpawnChance");
    public static int skirmishMinBounties = LunaSettings.getInt(modId, "skirmishMinBounties");
    public static int skirmishMaxBounties = LunaSettings.getInt(modId, "skirmishMaxBounties");
    public static int skirmishMinDuration = LunaSettings.getInt(modId, "skirmishMinDuration");
    public static int skirmishMaxDuration = LunaSettings.getInt(modId, "skirmishMaxDuration");
    public static int skirmishBaseShipBounty = LunaSettings.getInt(modId, "skirmishBaseShipBounty");
    public static float skirmishMinFP = LunaSettings.getFloat(modId, "skirmishMinFP");

    public static boolean assassinationActive = LunaSettings.getBoolean(modId, "assassinationActive");
    public static double assassinationSpawnChance = LunaSettings.getDouble(modId, "assassinationSpawnChance");
    public static int assassinationMinBounties = LunaSettings.getInt(modId, "assassinationMinBounties");
    public static int assassinationMaxBounties = LunaSettings.getInt(modId, "assassinationMaxBounties");
    public static double assassinationMinTravelDistance = LunaSettings.getDouble(modId, "assassinationMinTravelDistance");
    public static double assassinationBaseRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBaseRewardMultiplier");
    public static double assassinationBonusRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBonusRewardMultiplier");
    public static float assassinationMinFP = LunaSettings.getFloat(modId, "assassinationMinFP");

    public static boolean warCriminalActive = LunaSettings.getBoolean(modId, "warCriminalActive");
    public static double warCriminalSpawnChance = LunaSettings.getDouble(modId, "warCriminalSpawnChance");
    public static int warCriminalMinBounties = LunaSettings.getInt(modId, "warCriminalMinBounties");
    public static int warCriminalMaxBounties = LunaSettings.getInt(modId, "warCriminalMaxBounties");
    public static int warCriminalMinDuration = LunaSettings.getInt(modId, "warCriminalMinDuration");
    public static int warCriminalMaxDuration = LunaSettings.getInt(modId, "warCriminalMaxDuration");
    public static float warCriminalMinFP = LunaSettings.getFloat(modId, "warCriminalMinFP");

    public static boolean pirateBountyActive = LunaSettings.getBoolean(modId, "pirateBountyActive");
    public static double pirateBountySpawnChance = LunaSettings.getDouble(modId, "pirateBountySpawnChance");
    public static int pirateBountyMinBounties = LunaSettings.getInt(modId, "pirateBountyMinBounties");
    public static int pirateBountyMaxBounties = LunaSettings.getInt(modId, "pirateBountyMaxBounties");
    public static int pirateBountyMinDuration = LunaSettings.getInt(modId, "pirateBountyMinDuration");
    public static int pirateBountyMaxDuration = LunaSettings.getInt(modId, "pirateBountyMaxDuration");
    public static float pirateMinFP = LunaSettings.getFloat(modId, "pirateBountyMinFP");

    public static boolean deserterBountyActive = LunaSettings.getBoolean(modId, "deserterBountyActive");
    public static double deserterBountySpawnChance = LunaSettings.getDouble(modId, "deserterBountySpawnChance");
    public static int deserterBountyMinBounties = LunaSettings.getInt(modId, "deserterBountyMinBounties");
    public static int deserterBountyMaxBounties = LunaSettings.getInt(modId, "deserterBountyMaxBounties");
    public static int deserterBountyMinDuration = LunaSettings.getInt(modId, "deserterBountyMinDuration");
    public static int deserterBountyMaxDuration = LunaSettings.getInt(modId, "deserterBountyMaxDuration");
    public static float deserterMinFP = LunaSettings.getFloat(modId, "deserterBountyMinFP");

    public static boolean bountyHunterActive = LunaSettings.getBoolean(modId, "bountyHunterActive");
    public static double bountyHunterSpawnChance = LunaSettings.getDouble(modId, "bountyHunterSpawnChance");
    public static int bountyHunterMinBounties = LunaSettings.getInt(modId, "bountyHunterMinBounties");
    public static int bountyHunterMaxBounties = LunaSettings.getInt(modId, "bountyHunterMaxBounties");
    public static float bountyHunterMinFP = LunaSettings.getFloat(modId, "bountyHunterMinFP");

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
        baseRewardPerFP = LunaSettings.getInt(modId, "baseRewardPerFP");
        chanceEasy = LunaSettings.getFloat(modId, "chanceEasy");
        chanceMedium = LunaSettings.getFloat(modId, "chanceMedium");
        chanceChallenging = LunaSettings.getFloat(modId, "chanceChallenging");
        chanceHard = LunaSettings.getFloat(modId, "chanceHard");
        multiplierEasy = LunaSettings.getFloat(modId, "multiplierEasy");
        multiplierMedium = LunaSettings.getFloat(modId, "multiplierMedium");
        multiplierChallenging = LunaSettings.getFloat(modId, "multiplierChallenging");
        multiplierHard = LunaSettings.getFloat(modId, "multiplierHard");

        skirmishActive = LunaSettings.getBoolean(modId, "skirmishActive");
        skirmishSpawnChance = LunaSettings.getDouble(modId, "skirmishSpawnChance");
        skirmishMinBounties = LunaSettings.getInt(modId, "skirmishMinBounties");
        skirmishMaxBounties = LunaSettings.getInt(modId, "skirmishMaxBounties");
        skirmishMinDuration = LunaSettings.getInt(modId, "skirmishMinDuration");
        skirmishMaxDuration = LunaSettings.getInt(modId, "skirmishMaxDuration");
        skirmishBaseShipBounty = LunaSettings.getInt(modId, "skirmishBaseShipBounty");
        skirmishMinFP = LunaSettings.getFloat(modId, "skirmishMinFP");

        assassinationActive = LunaSettings.getBoolean(modId, "assassinationActive");
        assassinationSpawnChance = LunaSettings.getDouble(modId, "assassinationSpawnChance");
        assassinationMinBounties = LunaSettings.getInt(modId, "assassinationMinBounties");
        assassinationMaxBounties = LunaSettings.getInt(modId, "assassinationMaxBounties");
        assassinationMinTravelDistance = LunaSettings.getDouble(modId, "assassinationMinTravelDistance");
        assassinationBaseRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBaseRewardMultiplier");
        assassinationBonusRewardMultiplier = LunaSettings.getDouble(modId, "assassinationBonusRewardMultiplier");
        assassinationMinFP = LunaSettings.getFloat(modId, "assassinationMinFP");

        warCriminalActive = LunaSettings.getBoolean(modId, "warCriminalActive");
        warCriminalSpawnChance = LunaSettings.getDouble(modId, "warCriminalSpawnChance");
        warCriminalMinBounties = LunaSettings.getInt(modId, "warCriminalMinBounties");
        warCriminalMaxBounties = LunaSettings.getInt(modId, "warCriminalMaxBounties");
        warCriminalMinDuration = LunaSettings.getInt(modId, "warCriminalMinDuration");
        warCriminalMaxDuration = LunaSettings.getInt(modId, "warCriminalMaxDuration");
        warCriminalMinFP = LunaSettings.getFloat(modId, "warCriminalMinFP");

        pirateBountyActive = LunaSettings.getBoolean(modId, "pirateBountyActive");
        pirateBountySpawnChance = LunaSettings.getDouble(modId, "pirateBountySpawnChance");
        pirateBountyMinBounties = LunaSettings.getInt(modId, "pirateBountyMinBounties");
        pirateBountyMaxBounties = LunaSettings.getInt(modId, "pirateBountyMaxBounties");
        pirateBountyMinDuration = LunaSettings.getInt(modId, "pirateBountyMinDuration");
        pirateBountyMaxDuration = LunaSettings.getInt(modId, "pirateBountyMaxDuration");
        pirateMinFP = LunaSettings.getFloat(modId, "pirateMinFP");

        deserterBountyActive = LunaSettings.getBoolean(modId, "deserterBountyActive");
        deserterBountySpawnChance = LunaSettings.getDouble(modId, "deserterBountySpawnChance");
        deserterBountyMinBounties = LunaSettings.getInt(modId, "deserterBountyMinBounties");
        deserterBountyMaxBounties = LunaSettings.getInt(modId, "deserterBountyMaxBounties");
        deserterBountyMinDuration = LunaSettings.getInt(modId, "deserterBountyMinDuration");
        deserterBountyMaxDuration = LunaSettings.getInt(modId, "deserterBountyMaxDuration");
        deserterMinFP = LunaSettings.getFloat(modId, "deserterMinFP");

        bountyHunterActive = LunaSettings.getBoolean(modId, "bountyHunterActive");
        bountyHunterSpawnChance = LunaSettings.getDouble(modId, "bountyHunterSpawnChance");
        bountyHunterMinBounties = LunaSettings.getInt(modId, "bountyHunterMinBounties");
        bountyHunterMaxBounties = LunaSettings.getInt(modId, "bountyHunterMaxBounties");
        bountyHunterMinFP = LunaSettings.getFloat(modId, "bountyHunterMinFP");

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
