package de.schafunschaf.bountiesexpanded.helper.faction;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNullOrEmpty;

public class BountyHunterFactionPicker {

    public static List<FactionAPI> getHostileFactionsToPlayer(Set<String> blacklist) {
        List<FactionAPI> hostileFactions = new ArrayList<>();

        for (FactionAPI checkedFaction : Global.getSector().getAllFactions()) {
            if (!isNullOrEmpty(blacklist))
                if (blacklist.contains(checkedFaction.getId()))
                    continue;
            if (checkedFaction.getId().equals(Global.getSector().getPlayerFaction().getId())) continue;

            if (checkedFaction.getRelToPlayer().isHostile()) {
                hostileFactions.add(checkedFaction);
            }
        }

        return hostileFactions;
    }

    public static FactionAPI pickFaction() {
        return pickFaction(null);
    }

    public static FactionAPI pickFaction(Set<String> blacklist) {
        List<FactionAPI> factionList = new ArrayList<>();

        for (FactionAPI checkedFaction : Global.getSector().getAllFactions()) {
            if (checkedFaction.isPlayerFaction())
                continue;
            if (!isNullOrEmpty(blacklist))
                if (blacklist.contains(checkedFaction.getId()))
                    continue;
            factionList.add(checkedFaction);
        }
        return pickFactionFromList(factionList);
    }

    private static FactionAPI pickFactionFromList(List<FactionAPI> factionList) {
        if (isNullOrEmpty(factionList))
            return null;
        WeightedRandomPicker<FactionAPI> picker = new WeightedRandomPicker<>();
        for (FactionAPI checkedFaction : factionList) {
            if (isNull(checkedFaction))
                continue;

            float weight = 1f;

            weight -= checkedFaction.getRelToPlayer().getRel();

            picker.add(checkedFaction, weight);

        }
        return picker.pick();
    }
}
