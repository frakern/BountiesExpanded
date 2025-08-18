package de.schafunschaf.bountiesexpanded.helper.location;

import com.fs.starfarer.api.impl.campaign.ids.Tags;

import java.util.*;

public class TagCollection {
    public static final Map<String, Integer> VANILLA_BOUNTY_SYSTEM_TAGS = new HashMap<>();
    static {
        VANILLA_BOUNTY_SYSTEM_TAGS.put(Tags.THEME_MISC_SKIP, 1);
        VANILLA_BOUNTY_SYSTEM_TAGS.put(Tags.THEME_MISC, 3);
        VANILLA_BOUNTY_SYSTEM_TAGS.put(Tags.THEME_REMNANT_NO_FLEETS, 3);
        VANILLA_BOUNTY_SYSTEM_TAGS.put(Tags.THEME_RUINS, 5);
        VANILLA_BOUNTY_SYSTEM_TAGS.put(Tags.THEME_REMNANT_DESTROYED, 3);
        VANILLA_BOUNTY_SYSTEM_TAGS.put(Tags.THEME_CORE_UNPOPULATED, 1);
    }

    public static final Map<String, Integer> REMNANT_SYSTEM_TAGS = new HashMap<>();
    static {
        REMNANT_SYSTEM_TAGS.put(Tags.THEME_REMNANT, 1);
        REMNANT_SYSTEM_TAGS.put(Tags.THEME_REMNANT_MAIN, 2);
        REMNANT_SYSTEM_TAGS.put(Tags.THEME_REMNANT_SECONDARY, 1);
        REMNANT_SYSTEM_TAGS.put(Tags.THEME_REMNANT_RESURGENT, 2);
    }

    public static final Map<String, Integer> DERELICT_SYSTEM_TAGS = new HashMap<>();
    static {
        DERELICT_SYSTEM_TAGS.put(Tags.THEME_DERELICT, 1);
        DERELICT_SYSTEM_TAGS.put(Tags.THEME_DERELICT_CRYOSLEEPER, 2);
        DERELICT_SYSTEM_TAGS.put(Tags.THEME_DERELICT_MOTHERSHIP, 2);
        DERELICT_SYSTEM_TAGS.put(Tags.THEME_DERELICT_SURVEY_SHIP, 2);
        DERELICT_SYSTEM_TAGS.put(Tags.THEME_DERELICT_PROBES, 1);
    }
}
