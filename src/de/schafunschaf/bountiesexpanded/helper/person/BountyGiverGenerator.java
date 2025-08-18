package de.schafunschaf.bountiesexpanded.helper.person;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

public class BountyGiverGenerator {

    public static PersonAPI generateBountyGiver(MarketAPI market) {
        if (isNull(market)) return null;
        WeightedRandomPicker<PersonAPI> picker = new WeightedRandomPicker<>();
        picker.addAll(market.getPeopleCopy());
        return picker.pick();
    }
}
