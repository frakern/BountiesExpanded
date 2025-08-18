package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import de.schafunschaf.bountiesexpanded.ExternalDataSupplier;
import de.schafunschaf.bountiesexpanded.plugins.BountiesExpandedPlugin;
import lombok.extern.log4j.Log4j;

import java.util.*;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

@Log4j
public class RareFlagshipManager {
    public static final String RARE_FLAGSHIP_KEY = "$bountiesExpanded_rareFlagship";
    private static final Map<String, RareFlagshipData> rareFlagshipData = new HashMap<>();

    public static void loadRareFlagshipData() {
        rareFlagshipData.putAll(ExternalDataSupplier.loadRareFlagshipData(BountiesExpandedPlugin.RARE_FLAGSHIPS_FILE));
        log.info(String.format("BountiesExpanded: loaded %s rare flagships", rareFlagshipData.size()));
        for (Map.Entry<String, RareFlagshipData> rareFlagshipDataEntry : rareFlagshipData.entrySet()) {
            String flagshipID = rareFlagshipDataEntry.getValue().getFlagshipID();
            String flagshipVariantID = rareFlagshipDataEntry.getValue().getFlagshipVariantID();
            float weight = rareFlagshipDataEntry.getValue().getWeight();
            Set<String> factionID = rareFlagshipDataEntry.getValue().getFactionIDs();
            int fleetPoints = rareFlagshipDataEntry.getValue().getFleetPoints();

            log.info(String.format("ID: '%s' || VariantID: '%s' || Weight: '%s' || FactionID: '%s' || FleetPoints: '%s'", flagshipID, flagshipVariantID, weight, factionID, fleetPoints));
        }
    }

    public static Map<String, RareFlagshipData> getRareFlagshipData() {
        return new HashMap<>(rareFlagshipData);
    }

    public static RareFlagshipData getRareFlagship(String flagshipID) {
        return rareFlagshipData.get(flagshipID);
    }

    public static Set<String> getAllRareFlagships() {
        Set<String> allFlagshipIDs = new HashSet<>();
        for (Map.Entry<String, RareFlagshipData> entry : rareFlagshipData.entrySet())
            allFlagshipIDs.add(entry.getValue().getFlagshipVariantID());
        return allFlagshipIDs;
    }

    public static RareFlagshipData pickRareFlagship(String factionID, FleetMemberAPI currFlagship) {
        return pickRareFlagship(null, factionID, currFlagship);
    }

    public static RareFlagshipData pickRareFlagship(String flagshipID, String factionID, FleetMemberAPI currFlagship) {
        if (isNotNull(flagshipID)) {
            return getRareFlagship(flagshipID);
        }

        int baseFp = currFlagship.getFleetPointCost();
        List<RareFlagshipData> allowedShips = new ArrayList<>();
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        Map<String, RareFlagshipData> rareFlagshipData = getRareFlagshipData();

        // recalculate weights to take into account difference of current ship fp vs rare ship fp.
        int min = 9999;
        int max = 0;
        // find ships for valid faction and are not smaller than current ship.
        for (Map.Entry<String, RareFlagshipData> flagshipDataEntry : rareFlagshipData.entrySet()) {
            RareFlagshipData candidateShip = flagshipDataEntry.getValue();
            if (candidateShip.getFactionIDs().contains(factionID) && candidateShip.getFleetPoints() >= baseFp) {
                allowedShips.add(candidateShip);
                int diff = candidateShip.getFleetPoints() - baseFp;
                int absDiff = Math.abs(diff);
                if (absDiff < min) {
                    min = absDiff;
                }
                if (absDiff > max) {
                    max = absDiff;
                }
            }
        }

        // recalculate ship weights.
        for (RareFlagshipData candidateShip : allowedShips) {
            int diff = candidateShip.getFleetPoints() - baseFp;
            int absDiff = Math.abs(diff);
            float diffMult = 1f - ((absDiff - min) / (max - min == 0 ? 0.0000001f : max - min));
            float weight = candidateShip.getWeight() * diffMult;
            picker.add(candidateShip.getFlagshipID(), weight);
        }

        if (picker.isEmpty()) {
            return null;
        } else {
            // we add a null at 1.0 weight just to make the spawn weights work
            // and to reduce incidence of way-out-of-band flagship picks
            // because like, otherwise if a 0.01 weight 10000FP ship is the only one for the faction...
            // you're gonna see it every time
            picker.add(null, 1f);
            return getRareFlagship(picker.pick());
        }
    }

    public static boolean replaceFlagship(CampaignFleetAPI fleet) {
        return replaceFlagship(fleet, 1f);
    }

    public static boolean replaceFlagship(CampaignFleetAPI fleet, float chance) {
        if (Math.random() < chance) {
            String factionID = fleet.getFaction().getId();
            PersonAPI fleetCommander = fleet.getCommander();

            RareFlagshipData rareFlagshipData = RareFlagshipManager.pickRareFlagship(factionID, fleet.getFlagship());
            if (isNotNull(rareFlagshipData)) {
                ShipVariantAPI variant = Global.getSettings().getVariant(rareFlagshipData.getFlagshipVariantID());
                if (isNotNull(variant)) {
                    FleetMemberAPI rareFlagship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
                    if (isNotNull(rareFlagship)) {
                        fleet.getFleetData().addFleetMember(rareFlagship);
                        fleet.getFleetData().setFlagship(rareFlagship);
                        fleet.getFlagship().setCaptain(fleetCommander);
                        FleetMemberAPI flagship = fleet.getFlagship();
                        fleet.getMemoryWithoutUpdate().set(RareFlagshipManager.RARE_FLAGSHIP_KEY, flagship);
                        log.info(String.format("BountiesExpanded: Fleet got lucky! Added '%s' as rare flagship", flagship.getHullSpec().getHullName()));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean checkShipSize(ShipAPI.HullSize maxShipSize, Map.Entry<String, RareFlagshipData> dataEntry) {
        String flagshipVariantID = dataEntry.getValue().getFlagshipVariantID();
        float maxShipSizeNum = Misc.getSizeNum(maxShipSize);
        ShipVariantAPI flagshipVariant = Global.getSettings().getVariant(flagshipVariantID);
        if (isNull(flagshipVariant)) return false;
        float sizeNumFlagship = Misc.getSizeNum(flagshipVariant.getHullSpec().getHullSize());

        return sizeNumFlagship <= maxShipSizeNum;
    }
}
