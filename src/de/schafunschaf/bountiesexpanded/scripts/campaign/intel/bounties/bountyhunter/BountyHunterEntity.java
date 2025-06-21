package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BaseBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyResult;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity.BountyEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.MissionHandler;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.Random;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;

@Getter
@Setter
public class BountyHunterEntity implements BountyEntity {
    private final String assassinationIcon = "bountiesExpanded_assassination";
    private final int level;
    private final float fleetQuality;
    private final MissionHandler missionHandler;
    private final Difficulty difficulty;

    private final FactionAPI offeringFaction;
    private final CampaignFleetAPI fleet;
    private final PersonAPI offeringPerson;
    private final SectorEntityToken spawnLocation;

    private final int obfuscatedFleetSize;
    private float targetRepBeforeBattle = 0;
    private BountyHunterIntel intel;

    public BountyHunterEntity(FactionAPI offeringFaction, CampaignFleetAPI fleet, PersonAPI offeringPerson, SectorEntityToken spawnLocation, Difficulty difficulty, int level, float fleetQuality, MissionHandler missionHandler) {
        this.offeringFaction = offeringFaction;
        this.fleet = fleet;
        this.offeringPerson = offeringPerson;
        this.spawnLocation = spawnLocation;
        this.missionHandler = missionHandler;
        this.difficulty = difficulty;
        this.level = level;
        this.fleetQuality = fleetQuality;

        this.obfuscatedFleetSize = Math.max(fleet.getNumShips() - 4 + new Random().nextInt(9), 1);
    }

    @Override
    public int getBaseReward() { return 0; }

    @Override
    public FactionAPI getTargetedFaction() {
        return Global.getSector().getPlayerFaction();
    }
    @Override
    public PersonAPI getTargetedPerson() {
        return Global.getSector().getPlayerPerson();
    }

    @Override
    public SectorEntityToken getTravelDestination() { return null; }

    @Override
    public BaseEventManager getBountyManager() {
        return BountyHunterManager.getInstance();
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", assassinationIcon);
    }

    @Override
    public String getTitle(BountyResult result) {
        // TODO Adjust title.
        if (isNotNull(result)) {
            switch (result.type) {
                case END_PLAYER_BOUNTY:
                case END_PLAYER_NO_BOUNTY:
                case END_PLAYER_NO_REWARD:
                    return "Bounty Hunter - Completed";
                case END_OTHER:
                case END_TIME:
                    return "Bounty Hunter - Ended";
            }
        }
        return offeringFaction.getDisplayName() + " - Bounty Hunter";
    }

    @Override
    public BaseBountyIntel getBountyIntel() {
        return intel;
    }

    @Override
    public void addBulletPoints(BaseBountyIntel baseBountyIntel, TooltipMakerAPI info, ListInfoMode mode) {
        Color highlightColor = Misc.getHighlightColor();
        Color bulletColor = baseBountyIntel.getBulletColorForMode(mode);
        float initPad = (mode == ListInfoMode.IN_DESC) ? 10f : 3f;
        float bulletPadding = mode == ListInfoMode.IN_DESC ? 3f : 0f;
        BountyResult result = baseBountyIntel.getResult();
        float duration = baseBountyIntel.getDuration();
        float elapsedDays = baseBountyIntel.getElapsedDays();
        int days = Math.max((int) (duration - elapsedDays), 1);
        boolean isUpdate = baseBountyIntel.getListInfoParam() != null;

        baseBountyIntel.bullet(info);

        String name = Misc.ucFirst(offeringFaction.getDisplayName());
        info.addPara("A bounty has been put on you.", initPad, bulletColor, offeringFaction.getBaseUIColor() ,"bounty");

        var bullet = "";

        if (!isNull(result)) {
            switch (result.type) {
                case END_OTHER:
                    bullet = "Fleet failed to assemble";
                    break;
                case END_PLAYER_NO_REWARD:
                    bullet = "Fleet defeated";
                    break;
                case END_TIME:
                    bullet = "Mission over";
                    break;
                case END_PLAYER_NO_BOUNTY:
                    bullet = "Cancelled: No longer hostile";
                    break;
            }
            info.addPara(bullet, bulletPadding, bulletColor, Misc.getGrayColor());
        }
        else if (intel.assembling) {
            String dtl = Math.round(intel.daysToLaunch) + " " + BaseIntelPlugin.getDaysString(intel.daysToLaunch);
            if (!getSpawnLocation().isVisibleToPlayerFleet()) {
                bullet = String.format("Launching from unknown location in %s", dtl);
                info.addPara(bullet, bulletPadding, bulletColor, highlightColor, "$dtl");

            } else {
                bullet = String.format("The fleet will launch from %s in %s", spawnLocation.getMarket().getName(), dtl);
                info.addPara(bullet, bulletPadding, bulletColor, highlightColor, spawnLocation.getMarket().getName(), dtl);
            }
        }
        else {
            bullet = String.format("Fleet launched from %s", spawnLocation.getMarket().getName());
            info.addPara(bullet, bulletPadding, bulletColor, highlightColor, spawnLocation.getMarket().getName());
        }

        baseBountyIntel.unindent(info);
    }

    @Override
    public void createSmallDescription(BaseBountyIntel baseBountyIntel, TooltipMakerAPI info, float width, float height) {

        Color highlightColor = Misc.getHighlightColor();
        BountyResult result = baseBountyIntel.getResult();
        float opad = 10f;

        LabelAPI para;
        if (isNotNull(offeringPerson)) {
            info.addImages(width, 128, opad, opad, offeringFaction.getCrest(), offeringPerson.getPortraitSprite());
            var bullet = String.format("%s a %s of %s has set a bounty on your head due to your continued influence on sector politics. A fleet will soon try to claim it.", offeringPerson.getName().getFullName(), offeringPerson.getPost(), offeringFaction.getDisplayNameWithArticle());
            para = info.addPara(bullet, opad, Misc.getTextColor(), offeringFaction.getColor(), offeringPerson.getName().getFullName(), offeringPerson.getPost(), offeringFaction.getDisplayNameWithArticle());
        }
        else {
            info.addImage(offeringFaction.getLogo(), width, 128f, opad);
            var bullet = String.format("Someone within %s set a bounty on your head due to your continued influence on sector politics. A fleet will soon try to claim it.", offeringFaction.getDisplayNameWithArticle());
            para = info.addPara(bullet, opad, Misc.getTextColor(), offeringFaction.getColor(), offeringFaction.getDisplayNameWithArticle());
        }

        para.setHighlight(offeringFaction.getDisplayNameWithArticleWithoutArticle());
        para.setHighlightColor(offeringFaction.getBaseUIColor());

        if (!offeringFaction.getRelToPlayer().isHostile()) {
            info.addSpacer(opad);
            info.addPara("This fleet operates without official support of its governing faction. Defeating it is unlikely to cause reductions in reputations.", opad);
        }

        info.addSectionHeading("Status",
                offeringFaction.getBaseUIColor(),
                offeringFaction.getDarkUIColor(),
                Alignment.MID,
                opad);

        var text = "";

        if (!isNull(result)) {
            switch (result.type) {
                case END_OTHER:
                    text = "The fleet has failed to spawn. The event is now over.";
                    break;
                case END_PLAYER_NO_REWARD:
                    text = "The fleet has been defeated.";
                    break;
                case END_TIME:
                    text = "The fleet is returning to base.";
                    break;
                case END_PLAYER_NO_BOUNTY:
                    text = offeringFaction.getDisplayNameWithArticle() + " " + offeringFaction.getDisplayNameIsOrAre() + " no longer hostile. The bounty has been terminated.";
                    break;
            }

            info.addPara(text, opad, Misc.getTextColor(), Misc.getHighlightColor(), offeringFaction.getDisplayNameWithArticle());

        }
        else if (intel.assembling) {
            String dtl = Math.round(intel.daysToLaunch) + " " + BaseIntelPlugin.getDaysString(intel.daysToLaunch);
            if (!getSpawnLocation().isVisibleToPlayerFleet()) {
                text = String.format("Launching from unknown location in %s", dtl);

            } else {
                text = String.format("The fleet will launch from %s in %s", spawnLocation.getMarket().getName(), dtl);
            }
            info.addPara(text, opad, Misc.getTextColor(), highlightColor, spawnLocation.getMarket().getName(), dtl);

        }
        else {
            String durStr = Misc.getAtLeastStringForDays((int) intel.getDuration());
            text = String.format("The fleet is currently active and will pursue you for %s", durStr);
            info.addPara(text, opad, Misc.getTextColor(), Misc.getHighlightColor(), durStr);
        }

        if (fleet != null && Settings.isDebugActive()) {
            info.addPara("Debug information", opad);
            intel.bullet(info);
            info.addPara(String.format("Current location: %s", fleet.getContainingLocation()), 0);
            if (!fleet.getAssignmentsCopy().isEmpty()) {
                FleetAssignmentDataAPI assign = fleet.getAssignmentsCopy().get(0);
                info.addPara(String.format("Current assignment: %s, %s, target %s", assign.getAssignment(),
                        assign.getActionText(), assign.getTarget()), 0);
            }
            if (intel.locationToken != null) {
                info.addPara(String.format("Location token is in: %s", intel.locationToken.getContainingLocation()), 0);
            }
            info.addPara("Tracking mode: " + intel.trackingMode, 0);
            intel.unindent(info);
        }

    }

}