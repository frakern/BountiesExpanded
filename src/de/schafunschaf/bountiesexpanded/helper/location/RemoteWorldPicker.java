package de.schafunschaf.bountiesexpanded.helper.location;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CampaignTerrain;
import de.schafunschaf.bountiesexpanded.Settings;
import lombok.extern.log4j.Log4j;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.*;

@Log4j
public class RemoteWorldPicker {
    public static SectorEntityToken pickRandomHideout(boolean useVanillaMethod) {
        StarSystemAPI system = pickSystem(null, useVanillaMethod, 10000);
        return pickEntity(system);
    }

    public static SectorEntityToken pickRandomHideout(Map<String, Integer> requiredTags, boolean useVanillaMethod) {
        StarSystemAPI system = pickSystem(requiredTags, useVanillaMethod, 10000);
        return pickEntity(system);
    }

    public static SectorEntityToken pickRandomHideout(Map<String, Integer> requiredTags, boolean useVanillaMethod, int rangeLY) {
        StarSystemAPI system = pickSystem(requiredTags, useVanillaMethod, rangeLY);
        return pickEntity(system);
    }

    public static void createFakeLocationHint(SectorEntityToken hideoutLocation, PersonAPI person, TooltipMakerAPI info, float padding) {
        if (hideoutLocation != null) {
            SectorEntityToken fake = hideoutLocation.getContainingLocation().createToken(0, 0);
            fake.setOrbit(Global.getFactory().createCircularOrbit(hideoutLocation, 0, 1000, 100));

            String loc = BreadcrumbSpecial.getLocatedString(fake);
            loc = loc.replaceAll("orbiting", "hiding out near");
            loc = loc.replaceAll("located in", "hiding out in");
            String sheIs = "She is";
            if (person.getGender() == FullName.Gender.MALE) {
                sheIs = "He is";
            }
            info.addPara(sheIs + " rumored to be " + loc + ".", padding);
        }
    }

    private static StarSystemAPI pickSystem(Map<String, Integer> requiredTags, boolean useVanillaMethod, int rangeLY) {
        WeightedRandomPicker<StarSystemAPI> systemPicker = new WeightedRandomPicker<>();
        int mult = isNull(requiredTags) ? 1 : 0;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasPulsar())
                continue;

            if (isNotNull(requiredTags)) {
                if (!containsAny(system.getTags(), requiredTags.keySet()))
                    continue;

                if (!isNullOrEmpty(requiredTags))
                    for (Map.Entry<String, Integer> entry : requiredTags.entrySet())
                        mult = system.hasTag(entry.getKey()) ? entry.getValue() : 0;
            }

            // TODO Don't skip if hidden pirate market?
            boolean hasHiddenMarket = false;
            for (MarketAPI market : Misc.getMarketsInLocation(system)) {
                if (market.isHidden()) {
                    hasHiddenMarket = true;
                    continue;
                }
                break;
            }
            if (hasHiddenMarket)
                continue;

            float distToPlayer = Misc.getDistanceToPlayerLY(system.getLocation());
            final float noSpawnRange = Global.getSettings().getFloat("personBountyNoSpawnRangeAroundPlayerLY");
            if (distToPlayer < noSpawnRange)
                continue;

            float distToCoreWorlds = Misc.getDistanceLY(Misc.ZERO, system.getLocation());
            if (distToCoreWorlds > rangeLY)
                continue;

            if (useVanillaMethod) {
                float weight = system.getPlanets().size();
                for (PlanetAPI planet : system.getPlanets()) {
                    if (planet.isStar())
                        continue;
                    if (isNotNull(planet.getMarket())) {
                        float hazardValue = planet.getMarket().getHazardValue();
                        if (hazardValue <= 0f)
                            weight += 5f;
                        else if (hazardValue <= 0.25f)
                            weight += 3f;
                        else if (hazardValue <= 0.5f)
                            weight += 1f;
                    }
                }

                float dist = system.getLocation().length();
                float distMult = Math.max(0, 50000f - dist);

                systemPicker.add(system, weight * distMult * mult);
            } else
                systemPicker.add(system);
        }

        return systemPicker.pick();
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
            // skip derelict ships etc that will expire
            if (entity.hasTag(Tags.EXPIRES)) continue;
            // copied other skipped entities from AnalyzeEntityIntelCreator
            if (entity.hasTag(Tags.NOT_RANDOM_MISSION_TARGET)) continue;
            if (Misc.isImportantForReason(entity.getMemoryWithoutUpdate(), "aem")) continue;
            if (entity.getMemoryWithoutUpdate() != null && entity.getMemoryWithoutUpdate().getBoolean("$ttWeaponsCache")) continue;
            if (entity.getCircularOrbitRadius() > 10000f) continue;

            float distance = Misc.getDistance(entity.getLocation(), system.getCenter().getLocation());
            float distanceWeight = 1f / (0.25f + distance);

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
                        // ((CampaignTerrainAPI) entity).getType().equals(Terrain.MAGNETIC_FIELD) ||
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
