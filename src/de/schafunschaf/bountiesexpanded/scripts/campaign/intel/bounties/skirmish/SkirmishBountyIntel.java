package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.skirmish;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.util.Misc;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BaseBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyResult;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyResultType;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyType;
import de.schafunschaf.bountiesexpanded.util.FormattingTools;
import lombok.Getter;

import java.util.*;

import static com.fs.starfarer.api.combat.ShipAPI.HullSize;
import static de.schafunschaf.bountiesexpanded.helper.MiscBountyUtils.getUpdatedRep;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

@Getter
public class SkirmishBountyIntel extends BaseBountyIntel {
    private final SkirmishBountyEntity skirmishBountyEntity;
    private final MarketAPI market;
    private final LocationAPI location;
    private final FactionAPI offeringFaction;
    private final FactionAPI targetedFaction;
    private final int baseShipBounty;
    private int numBattles = 0;
    private float playerInvolvement = 0f;
    private Map<HullSize, int[]> destroyedShips;
    protected SkirmishBountyResult latestResult;
    protected boolean commerceMode = false; // due to Commerce industry in player system

    public SkirmishBountyIntel(SkirmishBountyEntity skirmishBountyEntity, MarketAPI market) {
        super(BountyType.SKIRMISH, skirmishBountyEntity, skirmishBountyEntity.getMissionHandler(), null, null, null, null);
        this.market = market;
        this.location = market.getContainingLocation();
        this.offeringFaction = skirmishBountyEntity.getOfferingFaction();
        this.targetedFaction = skirmishBountyEntity.getTargetedFaction();
        this.duration = new Random().nextInt(Settings.skirmishMaxDuration - Settings.skirmishMinDuration) + Settings.skirmishMinDuration;
        this.skirmishBountyEntity = skirmishBountyEntity;
        this.baseShipBounty = skirmishBountyEntity.getBaseShipBounty();
        skirmishBountyEntity.setBountyIntel(this);
        Misc.makeImportant(fleet, "pbe");
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (isEnded() || isEnding()) return;

        if (!battle.isPlayerInvolved()) return;

        if (!Misc.isNear(primaryWinner, market.getLocationInHyperspace())) return;

        int payment = 0;
        float fpDestroyed = 0;
        for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
            if (!otherFleet.getFaction().equals(targetedFaction)) continue;

            float bounty = 0;
            for (FleetMemberAPI loss : Misc.getSnapshotMembersLost(otherFleet)) {
                float mult = Misc.getSizeNum(loss.getHullSpec().getHullSize());
                bounty += mult * baseShipBounty;
                fpDestroyed += loss.getFleetPointCost();
            }

            payment += (int) (bounty * battle.getPlayerInvolvementFraction());
        }

        if (payment > 0) {
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(payment);

            float repFP = (int)(fpDestroyed * battle.getPlayerInvolvementFraction());
            ReputationActionResponsePlugin.ReputationAdjustmentResult rep = Global.getSector().adjustPlayerReputation(
                    new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.SYSTEM_BOUNTY_REWARD, repFP, null, null, true, false),
                    offeringFaction.getId());
            latestResult = new SkirmishBountyResult(payment, battle.getPlayerInvolvementFraction(), rep);
            sendUpdateIfPlayerHasIntel(latestResult, false);
        }
    }

    @Override
    protected void advanceImpl(float amount) {
        float days = Global.getSector().getClock().convertToDays(amount);

        elapsedDays += days;

        if (elapsedDays >= duration && !isDone() && !commerceMode) {
            endAfterDelay();
            boolean current = location == Global.getSector().getCurrentLocation();
            sendUpdateIfPlayerHasIntel(new Object(), !current);
            return;
        }
        if (targetedFaction != market.getFaction() || !market.isInEconomy()) {
            endAfterDelay();
            boolean current = location == Global.getSector().getCurrentLocation();
            sendUpdateIfPlayerHasIntel(new Object(), !current);
            return;
        }
    }

    @Override
    protected void cleanUp(boolean onlyIfImportant) {
        SkirmishBountyManager.getInstance().unregisterBounty(skirmishBountyEntity);
        super.cleanUp(onlyIfImportant);
    }

    @Override
    public void endImmediately() {
        SkirmishBountyManager.getInstance().unregisterBounty(skirmishBountyEntity);
        super.endImmediately();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> intelTags = super.getIntelTags(map);
        intelTags.add(offeringFaction.getId());
        return intelTags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return market.getPrimaryEntity();
    }

    public static class SkirmishBountyResult {
        public int payment;
        public float fraction;
        public ReputationActionResponsePlugin.ReputationAdjustmentResult rep;
        public SkirmishBountyResult(int payment, float fraction, ReputationActionResponsePlugin.ReputationAdjustmentResult rep) {
            this.payment = payment;
            this.fraction = fraction;
            this.rep = rep;
        }

    }
}
