package de.schafunschaf.bountiesexpanded.helper.person;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import de.schafunschaf.bountiesexpanded.helper.level.LevelPicker;

import java.util.Arrays;
import java.util.List;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

public class OfficerGenerator {
    public static PersonAPI generateOfficer(FactionAPI faction) {
        return generateOfficer(faction, null, LevelPicker.pickLevel(0), null);
    }

    public static PersonAPI generateOfficer(FactionAPI faction, FullName name) {
        return generateOfficer(faction, name, LevelPicker.pickLevel(0), null);
    }

    public static PersonAPI generateOfficer(FactionAPI faction, String personality) {
        return generateOfficer(faction, null, LevelPicker.pickLevel(0), personality);
    }

    public static PersonAPI generateOfficer(FactionAPI faction, int level) {
        return generateOfficer(faction, null, level, null);
    }

    public static PersonAPI generateOfficer(FactionAPI faction, FullName name, String personality) {
        return generateOfficer(faction, name, LevelPicker.pickLevel(0), personality);
    }

    public static PersonAPI generateOfficer(FactionAPI faction, String personality, int level) {
        return generateOfficer(faction, null, level, personality);
    }

    public static PersonAPI generateOfficer(FactionAPI faction, FullName name, int level, String personality) {
        if (isNull(faction)) return null;

        List<String> personalities = Arrays.asList(Personalities.RECKLESS, Personalities.AGGRESSIVE,
                Personalities.STEADY, Personalities.CAUTIOUS, Personalities.TIMID);

        PersonAPI generatedOfficer = OfficerManagerEvent.createOfficer(faction, level);

        if (isNotNull(name)) {
            generatedOfficer.setName(name);
        }

        if (personalities.contains(personality)) {
            generatedOfficer.setPersonality(personality);
        }

        if (level <= 2) {
            generatedOfficer.setRankId(Ranks.SPACE_ENSIGN);
        } else if (level <= 6) {
            generatedOfficer.setRankId(Ranks.SPACE_CAPTAIN);
        } else if (level <= 8) {
            generatedOfficer.setRankId(Ranks.SPACE_COMMANDER);
        } else {
            generatedOfficer.setRankId(Ranks.SPACE_ADMIRAL);
        }

        return generatedOfficer;
    }
}
