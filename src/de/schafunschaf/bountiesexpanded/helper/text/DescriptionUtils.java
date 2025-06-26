package de.schafunschaf.bountiesexpanded.helper.text;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetGenerator;
import de.schafunschaf.bountiesexpanded.helper.fleet.FleetUtils;
import de.schafunschaf.bountiesexpanded.helper.location.LocationUtils;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BaseBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.RareFlagshipManager;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import de.schafunschaf.bountiesexpanded.util.FormattingTools;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.awt.*;
import java.util.List;
import java.util.Random;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.*;

public class DescriptionUtils {
    public static final float DEFAULT_IMAGE_HEIGHT = 100f;

    public static void generateFullShipListForIntel(TooltipMakerAPI info, float width, float padding, CampaignFleetAPI fleet, boolean showThreatDesc) {
        generateShipListForIntel(info, width, padding, fleet, fleet.getNumShips(), showThreatDesc, 3, false);
    }

    public static void generateShipListForIntel(TooltipMakerAPI info, float width, float padding, CampaignFleetAPI fleet, int maxShipsToDisplay, boolean showThreatDesc, int maxRows, boolean showShipsRemaining) {
        Random random = new Random(fleet.getCommander().getNameString().hashCode() * 170000L);
        List<FleetMemberAPI> fleetMemberList;
        if (Settings.isDebugActive()) {
            fleetMemberList = FleetGenerator.createCompleteCopyForIntel(fleet);
        } else
            fleetMemberList = FleetGenerator.createCopyForIntel(fleet, maxShipsToDisplay, random);

        generateShipListForIntel(info, width, padding, fleetMemberList, maxShipsToDisplay, showThreatDesc, maxRows, showShipsRemaining, random);
    }

    public static void generateShipListForIntel(TooltipMakerAPI info, float width, float padding, List<FleetMemberAPI> shipList, int maxShipsToDisplay, boolean showThreatDesc, int maxRows, boolean showShipsRemaining, Random random) {
        if (isNullOrEmpty(shipList))
            return;

        if (isNull(random))
            random = new Random(shipList.get(0).getId().hashCode());

        if (Settings.isDebugActive())
            maxRows = 0;

        CampaignFleetAPI fleet = shipList.get(0).getFleetData().getFleet();

        List<FleetMemberAPI> shipListDisplay;
        if (shipList.size() > maxShipsToDisplay) {
            shipListDisplay = FleetUtils.generateShipList(shipList, maxShipsToDisplay, random);
        }
        else {
            shipListDisplay = FleetUtils.orderListBySize(shipList);
        }

        int cols = 7;
        int rows = (int) Math.ceil(shipList.size() / (float) cols);
        if (maxRows > 0 && rows > maxRows)
            rows = maxRows;

        float iconSize = width / cols;

        info.addShipList(cols, rows, iconSize, fleet.getFaction().getBaseUIColor(), shipListDisplay, padding);

        if (showShipsRemaining && !Settings.isDebugActive()) {
            int num = shipList.size() - shipListDisplay.size();
            num = Math.round((float) num * (1f + random.nextFloat() * 0.5f));

            if (num < 5) num = 0;
            else if (num < 10) num = 5;
            else if (num < 20) num = 10;
            else num = 20;

            if (num > 1) {
                info.addPara("The intel assessment notes the fleet may contain upwards of %s other ships" +
                        " of lesser significance.", padding, Misc.getHighlightColor(), "" + num);
            } else {
                info.addPara("The intel assessment notes the fleet may contain several other ships" +
                        " of lesser significance.", padding);
            }
        }

        if (showThreatDesc) DescriptionUtils.generateThreatDescription(info, fleet, padding);

        if (Settings.isDebugActive()) {
            info.addSectionHeading("DEBUG INFO", Alignment.MID, padding);
            info.setBulletedListMode("  - ");
            //info.setTextWidthOverride(width);
            int enemyFP = fleet.getFleetPoints();
            int playerFP = Global.getSector().getPlayerFleet().getFleetPoints();
            info.addPara("ENEMY FP: " + enemyFP, padding, fleet.getFaction().getBaseUIColor(), String.valueOf(enemyFP));
            info.addPara("PLAYER FP: " + playerFP, 0f, Misc.getHighlightColor(), String.valueOf(playerFP));
            info.addPara(String.format("LOCATION: %s", fleet.getContainingLocation()), 0f);
            if (!fleet.getAssignmentsCopy().isEmpty()) {
                FleetAssignmentDataAPI assign = fleet.getAssignmentsCopy().get(0);
                info.addPara(String.format("ASSIGNMENT: %s, %s", assign.getAssignment(),
                        assign.getActionText()), 0f);
                info.addPara(String.format("TARGET: %s", assign.getTarget().getName()), 0f);
            }
        }
    }


    public static void generateFancyCommanderDescription(TooltipMakerAPI info, float padding, CampaignFleetAPI fleet, PersonAPI person) {
        if (isNull(person))
            return;
        if (isNull(person.getStats()))
            return;

        PersonAPI commander = fleet.getCommander();
        String heOrShe = person.getHeOrShe();
        String levelDesc;
        String skillDesc;

        int personLevel = person.getStats().getLevel();
        if (personLevel <= 4) levelDesc = "an unremarkable officer";
        else if (personLevel <= 7) levelDesc = "a capable officer";
        else if (personLevel <= 11) levelDesc = "a highly capable officer";
        else levelDesc = "an exceptionally capable officer";

        boolean hasSIC = Global.getSettings().getModManager().isModEnabled("second_in_command");
        if (hasSIC) {
            SCData fleetData = SCUtils.getFleetData(fleet);
            List<second_in_command.specs.SCOfficer> officers = fleetData.getOfficersInFleet();
            WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

            for (SCOfficer officer : officers) {
                var aptitudePlugin = officer.getAptitudePlugin();
                var aptitudeId = aptitudePlugin.getId();
                switch (aptitudeId) {
                    case "sc_tactical":
                        picker.add("having a strong tactical command of the battlespace");
                        break;
                    case "sc_management":
                        picker.add("having a very courageous crew");
                        break;
                    case "sc_smallcraft":
                        picker.add("highly coordinated frigate attacks");
                        break;
                    case "sc_strikecraft":
                        picker.add("a noteworthy level of skill in running carrier operations");
                        break;
                    case "sc_technology":
                        picker.add("using overclocked flux coils");
                        break;
                    case "sc_warfare":
                        picker.add("tenacity in battle");
                        break;
                    case "sc_improvisation":
                        picker.add("using military-grade duct tape");
                        break;
                    case "sc_starfaring":
                        picker.add("having highly skilled navigators");
                        break;
                    case "sc_piracy":
                        picker.add("using underhanded tactics in battle");
                        break;
                    case "sc_automated":
                        picker.add("[REDACTED]");
                        break;

                }
            }

            Random random = new Random(person.getId().hashCode() * 1337L);
            picker.setRandom(random);

            skillDesc = picker.isEmpty() ? "nothing, really" : picker.pick();
        } else {
            List<MutableCharacterStatsAPI.SkillLevelAPI> knownSkills = commander.getStats().getSkillsCopy();
            WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

            for (MutableCharacterStatsAPI.SkillLevelAPI skill : knownSkills) {
                String skillName = skill.getSkill().getId();
                switch (skillName) {
                    case Skills.COORDINATED_MANEUVERS:
                        picker.add("a high effectiveness in coordinating the maneuvers of ships during combat");
                        break;
                    case Skills.WOLFPACK_TACTICS:
                        picker.add("using highly coordinated frigate attacks");
                        break;
                    case Skills.CREW_TRAINING:
                        picker.add("having a very courageous crew");
                        break;
                    case Skills.CARRIER_GROUP:
                        picker.add("an exceptional level of skill in running carrier operations");
                        break;
                    case Skills.OFFICER_TRAINING:
                        picker.add("having extremely skilled subordinates");
                        break;
                    case Skills.OFFICER_MANAGEMENT:
                        picker.add("having a high number of skilled subordinates");
                        break;
                    case Skills.NAVIGATION:
                        picker.add("having highly skilled navigators");
                        break;
                    case Skills.SENSORS:
                        picker.add("having overclocked sensory equipment");
                        break;
                    case Skills.ELECTRONIC_WARFARE:
                        picker.add("being proficient in electronic warfare");
                        break;
                    case Skills.FIGHTER_UPLINK:
                        picker.add("using customized fighter targeting algorithms");
                        break;
                    case Skills.FLUX_REGULATION:
                        picker.add("using overclocked flux coils");
                        break;
                    case Skills.PHASE_CORPS:
                        picker.add("using experimental phase coils");
                        break;
                    case Skills.FIELD_REPAIRS:
                        picker.add("having highly skilled mechanics");
                        break;
                    case Skills.DERELICT_CONTINGENT:
                        picker.add("using military-grade duct tape");
                        break;
                    case Skills.HELMSMANSHIP:
                        picker.add("meticulous ship engine maintenance");
                        break;
                    case Skills.TARGET_ANALYSIS:
                        picker.add("precise targeting analysis of enemy vessels");
                        break;
                    case Skills.COMBAT_ENDURANCE:
                        picker.add("having impressive endurance under sustained fire");
                        break;
                    case Skills.POINT_DEFENSE:
                        picker.add("having exceptional point-defense");
                        break;
                    case Skills.IMPACT_MITIGATION:
                        picker.add("making quick reactions to mitigate impact damage");
                        break;
                    case Skills.BALLISTIC_MASTERY:
                        picker.add("having mastery of ballistic weaponry");
                        break;
                    case Skills.FIELD_MODULATION:
                        picker.add("active tuning of energy field modulation");
                        break;
                    case Skills.DAMAGE_CONTROL:
                        picker.add("having rapid damage control procedures");
                        break;
                    case Skills.SYSTEMS_EXPERTISE:
                        picker.add("expert knowledge of ship systems");
                        break;
                    case Skills.MISSILE_SPECIALIZATION:
                        picker.add("using specialized missile loaders");
                        break;
                    case Skills.TACTICAL_DRILLS:
                        picker.add("practicing rigorous tactical combat drills");
                        break;
                    case Skills.BEST_OF_THE_BEST:
                        picker.add("exceptional officer prowess");
                        break;
                    case Skills.SUPPORT_DOCTRINE:
                        picker.add("profound understanding of support doctrines");
                        break;
                    case Skills.GUNNERY_IMPLANTS:
                        picker.add("enhanced reflexes via gunnery implants");
                        break;
                    case Skills.ENERGY_WEAPON_MASTERY:
                        picker.add("having mastery of energy-based weapons");
                        break;
                    case Skills.CYBERNETIC_AUGMENTATION:
                        picker.add("utilization of advanced cybernetic enhancements");
                        break;
                    case Skills.NEURAL_LINK:
                        picker.add("using an experimental direct neural interface for instantaneous command");
                        break;
                    case Skills.AUTOMATED_SHIPS:
                        picker.add("[REDACTED]");
                        break;
                    case Skills.BULK_TRANSPORT:
                        picker.add("efficient management of bulk cargo transport");
                        break;
                    case Skills.SALVAGING:
                        picker.add("expert salvaging and resource reclamation");
                        break;
                    case Skills.POLARIZED_ARMOR:
                        picker.add("using polarized armor plating");
                        break;
                    case Skills.ORDNANCE_EXPERTISE:
                        picker.add("expertise in ordnance selection and handling");
                        break;
                    case Skills.CONTAINMENT_PROCEDURES:
                        picker.add("having knowledge of hazardous containment procedures");
                        break;
                    case Skills.MAKESHIFT_EQUIPMENT:
                        picker.add("ingenuity in crafting makeshift ship equipment");
                        break;
                    case Skills.INDUSTRIAL_PLANNING:
                        picker.add("strategic industrial resource planning");
                        break;
                    case Skills.HULL_RESTORATION:
                        picker.add("skillful hull restoration techniques");
                        break;
                }
            }

            Random random = new Random(person.getId().hashCode() * 1337L);
            picker.setRandom(random);

            skillDesc = picker.isEmpty() ? "nothing, really" : picker.pick();
        }

        if (levelDesc.contains("unremarkable")) {
            info.addPara(String.format("%s is known for %s and is an otherwise unremarkable officer.", Misc.ucFirst(heOrShe), skillDesc), padding);
        }
        else {
            info.addPara(String.format("%s is %s known for %s.", Misc.ucFirst(heOrShe), levelDesc, skillDesc), padding);
        }

    }

    public static void generateFancyFleetDescription(TooltipMakerAPI info, float padding, CampaignFleetAPI fleet, PersonAPI person) {
        if (isNull(person))
            return;

        boolean isRareShip = fleet.getMemoryWithoutUpdate().contains(RareFlagshipManager.RARE_FLAGSHIP_KEY);
        int fleetSize = fleet.getNumShips();
        FleetMemberAPI flagship = fleet.getFlagship();
        PersonAPI commander = fleet.getCommander();
        String shipName = flagship.getShipName();
        String shipClass = flagship.getHullSpec().getHullNameWithDashClass();
        String aOrAn = FormattingTools.aOrAn(shipClass);
        String rareString = isRareShip ? "a rare" : aOrAn;
        String shipDesignation = flagship.getHullSpec().getDesignation().toLowerCase();
        String hisOrHer = person.getHisOrHer();
        String fleetDesc;
        String outputText;

        if (fleetSize <= 4) fleetDesc = "few ships";
        else if (fleetSize <= 7) fleetDesc = "small fleet";
        else if (fleetSize <= 14) fleetDesc = "medium-sized fleet";
        else if (fleetSize <= 21) fleetDesc = "large fleet";
        else if (fleetSize <= 28) fleetDesc = "very large fleet";
        else if (fleetSize <= 35) fleetDesc = "gigantic fleet";
        else fleetDesc = "grand armada";

        Color[] highlightColors = new Color[]{
                commander.getFaction().getBaseUIColor(),
                Misc.getHighlightColor(),
                commander.getFaction().getBaseUIColor(),
                Misc.getHighlightColor(),
                Misc.getHighlightColor()
        };
        String[] highlights = new String[]{
                commander.getFaction().getRank(commander.getRankId()) + " " + person.getName().getFullName(),
                fleetDesc,
                shipName,
                shipClass,
                shipDesignation
        };

        outputText = String.format("%s is rumored to be accompanied by a %s and is known to personally command the %s, " + rareString + " %s %s, as " + hisOrHer + " flagship.", (Object[]) highlights);

        info.addPara(outputText, padding, highlightColors, highlights);
    }

    public static String generateShipNameWithClass(FleetMemberAPI ship, boolean isRareShip) {
        if (isNull(ship))
            return "NO SHIP FOR NAME AND CLASS";

        String rareString = isRareShip ? "rare " : "";
        String shipName = ship.getShipName();
        String shipClass = ship.getHullSpec().getHullNameWithDashClass();
        String aOrAn = FormattingTools.aOrAn(shipClass);
        String shipDesignation = ship.getHullSpec().getDesignation().toLowerCase();
        String shipType = String.format("%s%s %s", rareString, shipClass, shipDesignation);

        return String.format("%s, %s %s", shipName, aOrAn, shipType);
    }

    public static String generateShipClassWithDesignation(FleetMemberAPI ship, boolean isRareShip) {
        if (isNull(ship))
            return "NO SHIP FOR NAME AND CLASS";

        String rareString = isRareShip ? "rare " : "";
        String shipClass = ship.getHullSpec().getHullNameWithDashClass();
        String aOrAn = isRareShip ? "a" : FormattingTools.aOrAn(shipClass);
        String shipDesignation = ship.getHullSpec().getDesignation().toLowerCase();
        String shipType = String.format("%s%s %s", rareString, shipClass, shipDesignation);

        return String.format("%s %s", aOrAn, shipType); // a (rare) Wolf-Class frigate
    }

    public static void generateHideoutDescription(TooltipMakerAPI info, BaseBountyIntel baseBountyIntel, Color highlightColor) {
        String isOrWas = isNull(baseBountyIntel.getFleet().getAI().getCurrentAssignmentType()) ? "was last seen " : "is ";
        SectorEntityToken hideout = baseBountyIntel.getSpawnLocation();
        info.addPara(
                "The fleet " + isOrWas + "near " + hideout.getName() + " in the "
                        + hideout.getStarSystem().getName() + ".",
                10f, highlightColor, hideout.getName(), hideout.getStarSystem().getName());
    }

    /**
     * Will show location of fleet by planet type and constellation.
     */
    public static void generateFakeHideoutDescription(TooltipMakerAPI info, BaseBountyIntel baseBountyIntel, float padding) {
        String heOrShe = FormattingTools.capitalizeFirst(baseBountyIntel.getPerson().getHeOrShe());
        SectorEntityToken spawnLocation = baseBountyIntel.getSpawnLocation();
        SectorEntityToken fakeLocation = spawnLocation.getContainingLocation().createToken(0.0F, 0.0F);

        fakeLocation.setOrbit(Global.getFactory().createCircularOrbit(spawnLocation, 0.0F, 1000.0F, 100.0F));
        String loc = BreadcrumbSpecial.getLocatedString(fakeLocation);

        if (spawnLocation instanceof PlanetAPI) {
            loc = loc.replaceAll("orbiting", "hiding out near");
            loc = loc.replaceAll("located in", "hiding in");
        }
        else if (spawnLocation instanceof JumpPointAPI) {
            loc = loc.replaceAll("orbiting", "raiding around");
            loc = loc.replaceAll("located in", "raiding in");
        }
        else if (spawnLocation instanceof CampaignTerrainAPI) {
            loc = loc.replaceAll("orbiting", "hiding out near");
            loc = loc.replaceAll("located in", "hiding in");
        }
        else if (spawnLocation.getCustomPlugin() instanceof DerelictShipEntityPlugin) {
            loc = loc.replaceAll("orbiting", "salvaging");
            loc = loc.replaceAll("located in", "hiding in");
        }
        else if (spawnLocation instanceof CustomCampaignEntityAPI) {
            if (spawnLocation.getCustomEntityType().equals(Entities.INACTIVE_GATE) ) {
                loc = loc.replaceAll("orbiting", "flying through");
                loc = loc.replaceAll("located in", "hiding in");
            }
            else {
                loc = loc.replaceAll("orbiting", "looting");
                loc = loc.replaceAll("located in", "hiding in");
            }
        }
        else {
            loc = loc.replaceAll("orbiting", "hiding out near");
            loc = loc.replaceAll("located in", "hiding in");
        }

        info.addPara(heOrShe + " was last seen " + loc + ".", padding);
    }

    /**
     * Will show location of fleet by planet type and constellation.
     */
    public static void generateDestinationFakeHideoutDescription(TooltipMakerAPI info, BaseBountyIntel baseBountyIntel, float padding) {
        String heOrShe = FormattingTools.capitalizeFirst(baseBountyIntel.getPerson().getHeOrShe());
        SectorEntityToken travelDestination = baseBountyIntel.getTravelDestination();
        SectorEntityToken fakeLocation = travelDestination.getContainingLocation().createToken(0.0F, 0.0F);

        fakeLocation.setOrbit(Global.getFactory().createCircularOrbit(travelDestination, 0.0F, 1000.0F, 100.0F));
        String loc = BreadcrumbSpecial.getLocatedString(fakeLocation);
        loc = loc.replaceAll("orbiting", "patrolling near");
        loc = loc.replaceAll("located in", "hiding in");

        info.addPara(heOrShe + " was last seen " + loc + ".", padding);
    }

    public static void generatePatrolDescription(TooltipMakerAPI info, BaseBountyIntel baseBountyIntel, float padding) {
        CampaignFleetAPI fleet = baseBountyIntel.getFleet();

//        String loc;
//        String terrainString = BreadcrumbSpecial.getTerrainString(fleet);
//        if (isNotNull(terrainString)) {
//            String systemDescription = BreadcrumbSpecial.getLocationDescription(fleet, true);
//            loc = String.format("The fleet was last seen flying through %s in %s.", terrainString, systemDescription);
//            info.addPara(loc, padding);
//        }
//        else {
//            loc = BreadcrumbSpecial.getLocatedString(LocationUtils.getNearestLocation(fleet));
//            loc = loc.replaceAll("orbiting", "patrolling near");
//            loc = loc.replaceAll("located", "patrolling");
//            info.addPara("The fleet was last seen " + loc + ".", padding);
//        }

        info.addPara("The fleet is located in " + baseBountyIntel.getSpawnLocation().getStarSystem().getName() + " and will most likely be found either in orbit around " +
                baseBountyIntel.getSpawnLocation().getMarket().getName() + ", or patrolling one of the system's objectives "
                + "(such as a comm relay) or jump-points.", padding);
    }

    public static void generateFakeTravelDescription(TooltipMakerAPI info, BaseBountyIntel baseBountyIntel, float padding) {
        String heOrShe = FormattingTools.capitalizeFirst(baseBountyIntel.getPerson().getHeOrShe());
        SectorEntityToken travelDestination = baseBountyIntel.getTravelDestination();
        SectorEntityToken fakeLocation = travelDestination.getContainingLocation().createToken(0.0F, 0.0F);

        fakeLocation.setOrbit(Global.getFactory().createCircularOrbit(travelDestination, 0.0F, 1000.0F, 100.0F));
        String obfuscatedLocation = BreadcrumbSpecial.getLocationDescription(travelDestination, false);
        String travelDescription = String.format("%s was last seen fleeing to %s.", heOrShe, obfuscatedLocation);

        info.addPara(travelDescription, padding);
    }

    public static void generateThreatDescription(TooltipMakerAPI info, CampaignFleetAPI enemyFleet, float padding) {
        float playerFleetStrength = FleetUtils.getFleetStrength(Global.getSector().getPlayerFleet());
        float enemyEffectiveStrength = FleetUtils.getFleetStrength(enemyFleet);
        // Add +-20% variation to enemy fleet strength.
        float variation = 0.20f;
        Random random = new Random(enemyFleet.getCommander().getNameString().hashCode() * 170000L);
        float randFactor = 1f + (random.nextFloat() * 2f - 1f) * variation; // will be between 0.8 and 1.2
        enemyEffectiveStrength *= randFactor;
        float playerStrengthDifference = enemyEffectiveStrength / playerFleetStrength;
        String threatLevel = "cakewalk";
        Color threatColor = new Color(0, 255, 150);
        if (playerStrengthDifference > 3f) {
            threatLevel = "nigh on impossible challenge";
            threatColor = new Color(255, 0, 255);
        } else if (playerStrengthDifference > 2f) {
            threatLevel = "deathtrap";
            threatColor = new Color(255, 0, 0);
        } else if (playerStrengthDifference > 1.75f) {
            threatLevel = "significant danger";
            threatColor = new Color(255, 70, 0);
        } else if (playerStrengthDifference > 1.4f) {
            threatLevel = "challenging encounter";
            threatColor = new Color(255, 150, 0);
        } else if (playerStrengthDifference > 1f) {
            threatLevel = "moderate threat";
            threatColor = new Color(200, 255, 0);
        } else if (playerStrengthDifference > 0.7f) {
            threatLevel = "small inconvenience";
            threatColor = new Color(0, 170, 0);
        }

        String descriptionText = String.format("Your tactical officer has run some calculations and classifies the target as a %s for our fleet.", threatLevel);

        info.addPara(descriptionText, padding, threatColor, threatLevel);
    }

    public static void addDifficultyText(TooltipMakerAPI info, float padding, Difficulty difficulty) {
        info.addPara("Your tactical officer classifies this fleet as " + difficulty.getShortDescriptionAnOrA() + " %s encounter.",
                padding, difficulty.getColor(), difficulty.getShortDescription());
    }

    public static String getStringForMoreDays(int days) {
        if (days <= 1f) {
            return "one more day";
        } else if (days <= 6f) {
            return "a few more days";
        } else if (days <= 7 + 6) {
            return "another week";
        } else if (days <= 14 + 6) {
            return "two more weeks";
        } else if (days <= 21 + 8) {
            return "three more weeks";
        } else if (days <= 30 + 29) {
            return "another month";
        } else if (days < 30 * 2 + 29) {
            return "two more months";
        } else if (days < 30 * 3 + 29) {
            return "three more months";
        } else {
            return "many more months";
        }
    }
}