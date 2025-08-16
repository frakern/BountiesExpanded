package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter;

import com.fs.starfarer.api.util.WeightedRandomPicker;
import de.schafunschaf.bountiesexpanded.Settings;
import lombok.Getter;

import java.awt.*;

@Getter
public final class Difficulty {

    public static final Difficulty EASY = new Difficulty(
            "easy", "an", 0f, -1, Color.GREEN
    );
    public static final Difficulty MEDIUM = new Difficulty(
            "fair", "a", 0.1f, 0, Color.CYAN
    );
    public static final Difficulty CHALLENGING = new Difficulty(
            "challenging", "a", 0.2f, 1, Color.ORANGE
    );
    public static final Difficulty HARD = new Difficulty(
            "difficult", "a", 0.3f, 2, Color.RED
    );
    public static final Difficulty BOSS = new Difficulty(
            "BOSS", "a", 0.4f, 3, Color.MAGENTA
    );
    private static final Difficulty[] VALUES = {
            EASY, MEDIUM, HARD, CHALLENGING, BOSS
    };
    private final String shortDescription;
    private final String shortDescriptionAnOrA;
    private final float multiplier;
    private final int modifier;
    private final Color color;

    private Difficulty(String shortDescription, String shortDescriptionAnOrA, float multiplier, int modifier, Color color) {
        this.shortDescriptionAnOrA = shortDescriptionAnOrA;
        this.shortDescription = shortDescription;
        this.multiplier = multiplier;
        this.modifier = modifier;
        this.color = color;
    }

    public static Difficulty[] values() {
        return VALUES.clone();
    }

    public static Difficulty randomDifficulty() {
        WeightedRandomPicker<Difficulty> picker = new WeightedRandomPicker<>();
        picker.add(EASY, Settings.chanceEasy);
        picker.add(MEDIUM, Settings.chanceMedium);
        picker.add(CHALLENGING, Settings.chanceChallenging);
        picker.add(HARD, Settings.chanceHard);
        return picker.pick();
    }
}
