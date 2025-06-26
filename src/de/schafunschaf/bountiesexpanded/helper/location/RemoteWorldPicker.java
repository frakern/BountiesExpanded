package de.schafunschaf.bountiesexpanded.helper.location;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity.EntityProvider;
import lombok.extern.log4j.Log4j;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.*;

@Log4j
public class RemoteWorldPicker {
    public static SectorEntityToken pickRandomHideout(boolean allowHiddenPirate) {
        StarSystemAPI system = pickSystem(null, allowHiddenPirate, 10000);
        return pickEntity(system);
    }

    public static SectorEntityToken pickRandomHideout(Map<String, Integer> requiredTags, boolean allowHiddenPirate) {
        StarSystemAPI system = pickSystem(requiredTags, allowHiddenPirate, 10000);
        return pickEntity(system);
    }

    public static SectorEntityToken pickRandomHideout(Map<String, Integer> requiredTags, boolean allowHiddenPirate, float targetMaxLY) {
        StarSystemAPI system = pickSystem(requiredTags, allowHiddenPirate, targetMaxLY);
        return pickEntity(system);
    }

    private static StarSystemAPI pickSystem(Map<String, Integer> requiredTags, boolean allowHiddenPirate, float targetMaxLY) {
        WeightedRandomPicker<StarSystemAPI> systemPicker = new WeightedRandomPicker<>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            float days = Global.getSector().getClock().getElapsedDaysSince(system.getLastPlayerVisitTimestamp());
            if (days < 20f) continue;

            if (system.getCenter().getMemoryWithoutUpdate().contains(EntityProvider.RECENTLY_USED_FOR_BOUNTY)) continue;

            // Skip if too close to player fleet.
            float distToPlayer = Misc.getDistanceToPlayerLY(system.getLocation());
            final float noSpawnRange = Global.getSettings().getFloat("personBountyNoSpawnRangeAroundPlayerLY");
            if (distToPlayer < noSpawnRange) continue;

            // Skip if pulsar system.
            if (system.hasPulsar()) continue;

            // Give weight to larger systems (more/better planets).
            float weight = system.getPlanets().size();
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                if (isNotNull(planet.getMarket())) {
                    float hazardValue = planet.getMarket().getHazardValue();
                    if (hazardValue <= 0f) weight += 5f;
                    else if (hazardValue <= 0.25f) weight += 3f;
                    else if (hazardValue <= 0.5f) weight += 1f;
                }
            }

            float tagMult = 0f;
            // Check for required tags.
            if (isNotNull(requiredTags)) {
                if (!containsAny(system.getTags(), requiredTags.keySet())) continue;

                if (!isNullOrEmpty(requiredTags)) {
                    for (Map.Entry<String, Integer> entry : requiredTags.entrySet()) {
                        tagMult += system.hasTag(entry.getKey()) ? entry.getValue() : 0;
                    }
                }
            }

            // Skip if has hidden market.
            boolean hasHiddenMarket = false;
            for (MarketAPI market : Misc.getMarketsInLocation(system)) {
                if (market.isHidden()) {
                    // Unless we are allowing pirate hidden markets.
                    if (allowHiddenPirate && market.getFactionId().equals(Factions.PIRATES)) {
                        hasHiddenMarket = false;
                        tagMult += 5f;
                    }
                    else {
                        hasHiddenMarket = true;
                        continue;
                    }
                }
                break;
            }
            if (hasHiddenMarket) continue;

            // Give less weight to systems beyond targetMaxLY but do not remove entirely.
            float distToCoreWorlds = Misc.getDistanceLY(Misc.ZERO, system.getLocation());
            float distanceMult = computeDistanceMult(distToCoreWorlds, targetMaxLY);

            systemPicker.add(system, weight * tagMult * distanceMult);
        }

        return systemPicker.pick();
    }

    private static float computeDistanceMult(float distLY, float targetMaxLY) {
        // How gently it falls off beyond targetMaxLY
        float softness = 2f;
        // Check if beyond targetMaxLY and by how much.
        float excess = distLY - targetMaxLY;
        // Soft exponential falloff after targetMaxLY
        float penalty = 1f / (1f + (excess / softness));
        // Still favor slightly more distant in-range systems.
        float bonus = 1f + distLY / 100f;

        if (excess <= 0) {
            return bonus; // float greater than 1
        }
        else {
            return penalty * bonus; // float less than 1
        }
    }

    private static SectorEntityToken pickPlanet(StarSystemAPI system) {
        if (isNull(system))
            return null;

        WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
        for (SectorEntityToken planet : system.getPlanets()) {
            if (planet.isStar()) continue;
            if (isNotNull(planet.getMarket()) && !planet.getMarket().isPlanetConditionMarketOnly()) continue;
            if (Settings.ignorePlayerMarkets && planet.getMarket().isPlayerOwned()) continue;
            if (planet.getMarket().isInHyperspace()) continue;

            picker.add(planet);
        }
        return picker.pick();
    }

    public static SectorEntityToken pickEntity(StarSystemAPI system) {

        WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
        List<SectorEntityToken> entities = new ArrayList<SectorEntityToken>(system.getAllEntities());

        for (SectorEntityToken entity : entities) {
            // Skip small asteroids
            if (entity instanceof AsteroidAPI) continue;
            // Skip anything without a name (not sure what these are but they exist)
            if (isNull(entity.getName())) continue;
            // skip derelict ships etc that will expire
            if (entity.hasTag(Tags.EXPIRES)) continue;
            // copied other skipped entities from AnalyzeEntityIntelCreator
            if (entity.hasTag(Tags.NOT_RANDOM_MISSION_TARGET)) continue;
            if (Misc.isImportantForReason(entity.getMemoryWithoutUpdate(), "aem")) continue;
            if (entity.getMemoryWithoutUpdate() != null && entity.getMemoryWithoutUpdate().getBoolean("$ttWeaponsCache")) continue;
            if (entity.getCircularOrbitRadius() > 10000f) continue;

            if (entity instanceof PlanetAPI) {
                if (entity.isStar()) continue;
                if (isNotNull(entity.getMarket()) && !entity.getMarket().isPlanetConditionMarketOnly()) continue;
                if (Settings.ignorePlayerMarkets && entity.getMarket().isPlayerOwned()) continue;
                if (entity.getMarket().isInHyperspace()) continue;
                picker.add(entity, 1f);
            }
            else if (entity instanceof JumpPointAPI) {
                picker.add(entity, 1f);
            }
            else if (entity instanceof CampaignTerrainAPI) {
                if (entity.hasTag(Tags.DEBRIS_FIELD)) {
                    float dist = Misc.getDistance(new Vector2f(), entity.getLocation());
                    if (dist < 3000) {
                        picker.add(entity, 1.5f); // located in the heart of
                    } else if (dist > 12000) {
                        picker.add(entity, 0.25f); // located in the outer reaches of
                    } else {
                        picker.add(entity, 0.5f); // located some distance away from the center of
                    }
                }
                else if (
                        // ((CampaignTerrainAPI) entity).getType().equals(Terrain.MAGNETIC_FIELD) || // caused fleets to fly into sun
                        ((CampaignTerrainAPI) entity).getType().equals(Terrain.ASTEROID_FIELD) ||
                        ((CampaignTerrainAPI) entity).getType().equals(Terrain.ASTEROID_BELT)
                ) {
                    picker.add(entity, 5.0f);
                }
            }
            else if (entity.getCustomPlugin() instanceof DerelictShipEntityPlugin) {
                picker.add(entity, 0.5f);
            }
            else if (entity instanceof CustomCampaignEntityAPI) {
                if (
                        entity.getCustomEntityType().equals(Entities.ORBITAL_HABITAT_REMNANT) ||
                        entity.getCustomEntityType().equals(Entities.STATION_MINING_REMNANT) ||
                        entity.getCustomEntityType().equals(Entities.STATION_RESEARCH_REMNANT)
                ) {
                    picker.add(entity, 1.5f);
                }
                else if (
                        entity.getCustomEntityType().equals(Entities.DERELICT_SURVEY_PROBE) ||
                        entity.getCustomEntityType().equals(Entities.DERELICT_SURVEY_SHIP) ||
                        entity.getCustomEntityType().equals(Entities.DERELICT_MOTHERSHIP)
                ) {
                    picker.add(entity, 1f);
                }
                else if (
                        entity.getCustomEntityType().equals(Entities.COMM_RELAY) ||
                        entity.getCustomEntityType().equals(Entities.COMM_RELAY_MAKESHIFT) ||
                        entity.getCustomEntityType().equals(Entities.SENSOR_ARRAY) ||
                        entity.getCustomEntityType().equals(Entities.SENSOR_ARRAY_MAKESHIFT) ||
                        entity.getCustomEntityType().equals(Entities.NAV_BUOY) ||
                        entity.getCustomEntityType().equals(Entities.NAV_BUOY_MAKESHIFT)
                ) {
                    picker.add(entity, 0.25f);
                }
                else if (
                        entity.getCustomEntityType().equals(Entities.INACTIVE_GATE) ||
                        entity.getCustomEntityType().equals(Entities.SUPPLY_CACHE) ||
                        entity.getCustomEntityType().equals(Entities.SUPPLY_CACHE_SMALL) ||
                        entity.getCustomEntityType().equals(Entities.EQUIPMENT_CACHE) ||
                        entity.getCustomEntityType().equals(Entities.EQUIPMENT_CACHE_SMALL) ||
                        entity.getCustomEntityType().equals(Entities.WEAPONS_CACHE) ||
                        entity.getCustomEntityType().equals(Entities.WEAPONS_CACHE_SMALL) ||
                        entity.getCustomEntityType().equals(Entities.WEAPONS_CACHE_LOW) ||
                        entity.getCustomEntityType().equals(Entities.WEAPONS_CACHE_SMALL_LOW) ||
                        entity.getCustomEntityType().equals(Entities.WEAPONS_CACHE_HIGH) ||
                        entity.getCustomEntityType().equals(Entities.WEAPONS_CACHE_SMALL_HIGH) ||
                        entity.getCustomEntityType().equals(Entities.TECHNOLOGY_CACHE)
                ) {
                    picker.add(entity, 0.5f);
                }
            }

        }

        return picker.pick();
    }

}
