package de.schafunschaf.bountiesexpanded.helper.credits;

import com.fs.starfarer.api.Global;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.level.LevelPicker;
import de.schafunschaf.bountiesexpanded.util.FormattingTools;

public class CreditCalculator {
    public static int vanillaCalculation(float multiplier) {
        int bountyLevel = LevelPicker.pickLevel(0);
        return calculate(bountyLevel, multiplier + 1f);
    }

    public static int vanillaCalculation(int level, float multiplier) {
        return calculate(level, multiplier + 1f);
    }

    public static int getRewardByFP(int fleetPoints, float multiplier) {
        int value = (int) (250 * fleetPoints * (1f + multiplier));
        return FormattingTools.roundWholeNumber(value, 3);
    }

    private static int calculate(int level, float multiplier) {
        float base = Settings.baseReward;
        float perLevel = Settings.rewardPerTier;
        float random = perLevel * (int) (Math.random() * 15) / 15f;

        return FormattingTools.roundWholeNumber((int) ((base + perLevel * level + random) * multiplier), 3);
    }
}
