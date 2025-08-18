package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.skirmish;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.ui.TooltipAPIUtils;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BaseBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyResult;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity.BountyEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.MissionHandler;
import de.schafunschaf.bountiesexpanded.util.FormattingTools;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.combat.ShipAPI.HullSize;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;
import static de.schafunschaf.bountiesexpanded.util.FormattingTools.singularOrPlural;

@Getter
@Setter
public class SkirmishBountyEntity implements BountyEntity {
    private final String skirmishIcon = "bountiesExpanded_skirmish";
    private final int baseShipBounty;
    private final FactionAPI offeringFaction;
    private final FactionAPI targetedFaction;
    private final MarketAPI targetMarket;
    private final String[] creditsPerSize;
    private final MissionHandler missionHandler = null;
    private float targetRepBeforeBattle = 0;
    private SkirmishBountyIntel bountyIntel;
    private final boolean nex;

    public SkirmishBountyEntity(FactionAPI offeringFaction, FactionAPI targetedFaction, MarketAPI targetMarket, boolean nex) {
        this.offeringFaction = offeringFaction;
        this.targetedFaction = targetedFaction;
        this.targetMarket = targetMarket;
        this.nex = nex;
        float base = Settings.skirmishBaseShipBounty;
        float scale = 0.85f + (float) Math.random() * 0.3f;
        float multiplier = nex ? 1.5f : 1f;
        this.baseShipBounty = (int) FormattingTools.roundWholeNumber((base * scale) * multiplier, 1);
        this.creditsPerSize = new String[]{
                Misc.getDGSCredits(FormattingTools.roundWholeNumber(baseShipBounty * Misc.getSizeNum(HullSize.FRIGATE), 2)),
                Misc.getDGSCredits(FormattingTools.roundWholeNumber(baseShipBounty * Misc.getSizeNum(HullSize.DESTROYER), 2)),
                Misc.getDGSCredits(FormattingTools.roundWholeNumber(baseShipBounty * Misc.getSizeNum(HullSize.CRUISER), 2)),
                Misc.getDGSCredits(FormattingTools.roundWholeNumber(baseShipBounty * Misc.getSizeNum(HullSize.CAPITAL_SHIP), 2))};
    }

    @Override
    public BaseEventManager getBountyManager() {
        return SkirmishBountyManager.getInstance();
    }

    @Override
    public PersonAPI getOfferingPerson() {
        return null;
    }

    @Override
    public PersonAPI getTargetedPerson() {
        return null;
    }

    @Override
    public SectorEntityToken getSpawnLocation() {
        return null;
    }

    @Override
    public SectorEntityToken getTravelDestination() {
        return null;
    }

    public CampaignFleetAPI getFleet() {
        return null;
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", skirmishIcon);
    }

    @Override
    public String getTitle(BountyResult result) {
        String name = targetMarket.getName();
        StarSystemAPI system = targetMarket.getStarSystem();
        if (system != null) {
            name = system.getBaseName();
        }
        if (bountyIntel.isEnding()) {
            return "Skirmish Bounty Ended - " + name;
        }
        return "Skirmish Bounty - " + name;
    }

    @Override
    public Difficulty getDifficulty() {
        return null;
    }

    @Override
    public int getBaseReward() {
        return 0;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public float getFleetQuality() {
        return 0;
    }

    @Override
    public void addBulletPoints(BaseBountyIntel baseBountyIntel, TooltipMakerAPI info, ListInfoMode mode) {
        Color tc = baseBountyIntel.getBulletColorForMode(mode);

        Color highlightColor = Misc.getHighlightColor();
        Color bulletColor = baseBountyIntel.getBulletColorForMode(mode);
        float initPad = (mode == ListInfoMode.IN_DESC) ? 10f : 3f;
        float bulletPadding = mode == ListInfoMode.IN_DESC ? 3f : 0f;
        float duration = baseBountyIntel.getDuration();
        float elapsedDays = baseBountyIntel.getElapsedDays();
        int days = Math.max((int) (duration - elapsedDays), 1);

        SkirmishBountyIntel.SkirmishBountyResult latestResult;
        if (baseBountyIntel instanceof SkirmishBountyIntel skirmishBountyIntel) {
            latestResult = skirmishBountyIntel.getLatestResult();
        } else {
            latestResult = null;
        }

        baseBountyIntel.bullet(info);

        boolean isUpdate = baseBountyIntel.getListInfoParam() != null;

        if (baseBountyIntel.isEnding() && isUpdate) {
            //info.addPara("Over", initPad);
        } else {
            if (isUpdate && latestResult != null) {
                info.addPara("%s received", initPad, tc, highlightColor, Misc.getDGSCredits(latestResult.payment));
                if (Math.round(latestResult.fraction * 100f) < 100f) {
                    info.addPara("%s share based on damage dealt", 0f, tc, highlightColor,
                            "" + (int) Math.round(latestResult.fraction * 100f) + "%");
                }
                CoreReputationPlugin.addAdjustmentMessage(latestResult.rep.delta, offeringFaction, null,
                        null, null, info, tc, isUpdate, 0f);
            } else if (mode == ListInfoMode.IN_DESC) {
                info.addPara("%s base reward per frigate", initPad, tc, highlightColor, Misc.getDGSCredits(baseShipBounty));
                info.addPara("%s" + singularOrPlural(days, " day") + " remaining", bulletPadding, bulletColor, highlightColor, String.valueOf(days));

            } else {
                if (!baseBountyIntel.isEnding()) {
                    info.addPara("Offered by: " + offeringFaction.getDisplayName(), initPad, tc,
                            offeringFaction.getBaseUIColor(), offeringFaction.getDisplayName());
                    info.addPara("%s base reward per frigate", 0f, tc, highlightColor, Misc.getDGSCredits(baseShipBounty));
                    info.addPara("%s" + singularOrPlural(days, " day") + " remaining", bulletPadding, bulletColor, highlightColor, String.valueOf(days));
                }
            }
        }

        baseBountyIntel.unindent(info);
    }

    @Override
    public void createSmallDescription(BaseBountyIntel baseBountyIntel, TooltipMakerAPI info, float width, float height) {
        Color tc = Misc.getTextColor();
        float pad = 3f;

        Color highlightColor = Misc.getHighlightColor();
        SkirmishBountyIntel.SkirmishBountyResult latestResult;
        if (baseBountyIntel instanceof SkirmishBountyIntel skirmishBountyIntel) {
            latestResult = skirmishBountyIntel.getLatestResult();
        } else {
            latestResult = null;
        }

        float opad = 10f;

        TooltipAPIUtils.addFactionFlagsWithRep(info, width, opad, opad, offeringFaction, targetedFaction);

        String locStr = "near " + targetMarket.getName();
        if (targetMarket.getStarSystem() != null) {
            locStr = "in or near the " + targetMarket.getStarSystem().getNameWithLowercaseType();
        }

        Color[] factionAndHighlightColors = {offeringFaction.getColor(), targetedFaction.getColor()};
        info.addSectionHeading("Briefing", baseBountyIntel.getFactionForUIColors().getBaseUIColor(), baseBountyIntel.getFactionForUIColors().getDarkUIColor(), Alignment.MID, opad);
        if (this.isNex()) {
            info.addPara("%s is seeking privateers to join their fleet action against %s " + locStr + ".",
                    opad,
                    factionAndHighlightColors,
                    Misc.ucFirst(offeringFaction.getDisplayNameWithArticle()),
                    targetedFaction.getDisplayNameWithArticle());
        }
        else {
            info.addPara("Due to ongoing interfaction hostilities, %s has posted a standing bounty on any %s fleets " + locStr + ".",
                    opad,
                    factionAndHighlightColors,
                    offeringFaction.getDisplayNameWithArticle(),
                    targetedFaction.getDisplayNameWithArticleWithoutArticle());
        }

        if (baseBountyIntel.isEnding()) {
            info.addPara("This bounty is no longer on offer.", opad);
        }
        else {
            //		if (!Global.getSector().getListenerManager().hasListener(this)) {
//			Global.getSector().getListenerManager().addListener(this);
//			info.addPara("Listener not registered!", opad);
//		}

            addBulletPoints(baseBountyIntel, info, ListInfoMode.IN_DESC);

            info.addPara("Payment depends on the number and size of ships destroyed. " +
                            "Standing with " + offeringFaction.getDisplayNameWithArticle() + " may also improve.",
                    opad);
        }

        if (latestResult != null) {
            //Color color = faction.getBaseUIColor();
            //Color dark = faction.getDarkUIColor();
            //info.addSectionHeading("Most Recent Reward", color, dark, Alignment.MID, opad);
            info.addPara("Most recent bounty:", opad);
            baseBountyIntel.bullet(info);
            info.addPara("%s received", pad, tc, highlightColor, Misc.getDGSCredits(latestResult.payment));
            if (Math.round(latestResult.fraction * 100f) < 100f) {
                info.addPara("%s share based on damage dealt", 0f, tc, highlightColor,
                        "" + (int) Math.round(latestResult.fraction * 100f) + "%");
            }
            CoreReputationPlugin.addAdjustmentMessage(latestResult.rep.delta, offeringFaction, null,
                    null, null, info, tc, false, 0f);
            baseBountyIntel.unindent(info);
        }
    }
}