package de.schafunschaf.bountiesexpanded;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.NameStringCollection;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.RareFlagshipData;
import lombok.extern.log4j.Log4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ParsingTools.parseJSONArray;

@Log4j
public class ExternalDataSupplier {
    public static void loadBlacklists(String fileName) {
        try {
            JSONObject blacklist = Global.getSettings().loadJSON(fileName);
            Blacklists.addFactions(parseJSONArray(blacklist.getJSONArray("blacklistedFactions")));
        } catch (IOException | JSONException exception) {
            log.error("BountiesExpanded - Failed to load Blacklists! - " + exception.getMessage());
        }
    }

    public static void loadNameStringFiles(String fileName) {
        try {
            JSONObject fleetNames = Global.getSettings().loadJSON(fileName);
            NameStringCollection.suspiciousNames.addAll(parseJSONArray(fleetNames.getJSONArray("suspiciousNames")));
            NameStringCollection.fleetActionTexts.addAll(parseJSONArray(fleetNames.getJSONArray("fleetActionTexts")));
            NameStringCollection.pirateJobs.addAll(parseJSONArray(fleetNames.getJSONArray("pirateJobs")));
            NameStringCollection.piratePersonalities.addAll(parseJSONArray(fleetNames.getJSONArray("piratePersonalities")));
            NameStringCollection.pirateTitles.addAll(parseJSONArray(fleetNames.getJSONArray("pirateTitles")));
            NameStringCollection.killWords.addAll(parseJSONArray(fleetNames.getJSONArray("killWords")));
            NameStringCollection.crimeReasons.addAll(parseJSONArray(fleetNames.getJSONArray("crimeReasons")));
            NameStringCollection.crimeTypes.addAll(parseJSONArray(fleetNames.getJSONArray("crimeTypes")));
            NameStringCollection.crimeVictims.addAll(parseJSONArray(fleetNames.getJSONArray("crimeVictims")));
            NameStringCollection.pirateFleetNames.addAll(parseJSONArray(fleetNames.getJSONArray("pirateFleetNames")));
        } catch (IOException | JSONException exception) {
            log.error("BountiesExpanded - Failed to load FleetNames! - " + exception.getMessage());
        }
    }

    public static Map<String, RareFlagshipData> loadRareFlagshipData(String fileName) {
        Map<String, RareFlagshipData> rareFlagshipDataMap = new HashMap<>();

        try {
            JSONArray uniqueBountyDataJSON = Global.getSettings().getMergedSpreadsheetDataForMod("bounty", fileName, "BountiesExpanded");

            for (int i = 0; i < uniqueBountyDataJSON.length(); ++i) {
                JSONObject row = uniqueBountyDataJSON.getJSONObject(i);
                if (row.has("bounty") && row.getString("bounty") != null && !row.getString("bounty").isEmpty()) {
                    String flagshipID = row.getString("bounty");
                    log.info("loading rare flagship " + flagshipID);

                    String factionStrings = row.optString("factions");
                    Set<String> factionList = new HashSet<>();
                    if (isNotNull(factionStrings)) {
                        String[] faction = factionStrings.split("\\s*(,\\s*)+");
                        for (int x = 0; x < faction.length; x++)
                            faction[x] = faction[x].replace("persean_league", Factions.PERSEAN);

                        factionList.addAll(Arrays.asList(faction));
                    }

                    int fleetPoints;
                    try {
                        fleetPoints = Global.getSettings().getVariant(row.getString("variant")).getHullSpec().getFleetPoints();
                    } catch (NullPointerException ex) {
                        log.warn("couldn't get fleet points for rare flagship " + flagshipID + " - skipping");
                        log.warn(ex);
                        fleetPoints = 0;
                    }
                    if (fleetPoints > 0) {
                        RareFlagshipData flagshipData = new RareFlagshipData(flagshipID,
                                row.getString("variant"),
                                factionList,
                                (float) row.getDouble("weight"),
                                fleetPoints
                        );
                        log.info("loaded rare flagship " + flagshipID);
                        rareFlagshipDataMap.put(flagshipID, flagshipData);
                    }
                } else {
                    log.info("hit empty line, rare flagship loading ended");
                }
            }
        } catch (IOException | JSONException exception) {
            log.error("BountiesExpanded - Failed to load RareFlagshipData! - " + exception.getMessage());
        }

        return rareFlagshipDataMap;
    }
}
