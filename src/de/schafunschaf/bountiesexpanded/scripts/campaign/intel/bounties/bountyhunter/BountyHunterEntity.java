package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.bountyhunter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.credits.CreditCalculator;
import de.schafunschaf.bountiesexpanded.helper.text.DescriptionUtils;
import de.schafunschaf.bountiesexpanded.helper.ui.TooltipAPIUtils;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.NameStringCollection;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BaseBountyIntel;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.BountyResult;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.entity.BountyEntity;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.Difficulty;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.parameter.MissionHandler;
import de.schafunschaf.bountiesexpanded.util.CollectionUtils;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;
import static de.schafunschaf.bountiesexpanded.util.FormattingTools.singularOrPlural;

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

        String name = Misc.ucFirst(offeringFaction.getDisplayNameWithArticle());
        info.addPara(String.format("A bounty contract has been put on you by %s", name), initPad, bulletColor, offeringFaction.getBaseUIColor() ,"bounty", name);

        var bullet = "";

        if (!isNull(result)) {
            switch (result.type) {
                case END_OTHER:
                    bullet = "Contract canceled";
                    break;
                case END_PLAYER_NO_REWARD:
                    bullet = "Bounty Hunter fleet defeated";
                    break;
                case END_TIME:
                    bullet = "Bounty Hunter gave up";
                    break;
                case END_PLAYER_NO_BOUNTY:
                    bullet = "Cancelled: No longer hostile";
                    break;
            }
            info.addPara(bullet, bulletPadding, bulletColor, Misc.getGrayColor());
        }
        else if (intel.assembling) {
            String dtl = Math.round(intel.daysToLaunch) + " " + BaseIntelPlugin.getDaysString(intel.daysToLaunch);
            bullet = String.format("The contract is open for %s", dtl);
            info.addPara(bullet, bulletPadding, bulletColor, highlightColor, dtl);
        }
        else {
            String durStr = Misc.getAtLeastStringForDays((int) intel.getDuration());
            bullet = String.format("The Bounty Hunter fleet has set out from %s and will pursue you for %s", spawnLocation.getMarket().getName(), durStr);
            info.addPara(bullet, bulletPadding, bulletColor, highlightColor, spawnLocation.getMarket().getName(), durStr);
        }

        baseBountyIntel.unindent(info);
    }

    @Override
    public void createSmallDescription(BaseBountyIntel baseBountyIntel, TooltipMakerAPI info, float width, float height) {

        Color highlightColor = Misc.getHighlightColor();
        List<FleetMemberAPI> flagshipCopy = getFlagshipCopy();
        BountyResult result = baseBountyIntel.getResult();
        float opad = 10f;
        String bountyCredits = String.valueOf(CreditCalculator.getRewardByFP(Global.getSector().getPlayerFleet().getFleetPoints(), difficulty.getModifier() * 5f));

        LabelAPI para;
        if (isNotNull(offeringPerson)) {
            TooltipAPIUtils.addPersonWithFactionRepBar(info, width, opad, opad, offeringPerson);
            var bullet = String.format("You have received rumors that %s a %s of %s has put out a bounty contract on your head for %s credits.", offeringPerson.getName().getFullName(), offeringPerson.getPost(), offeringFaction.getDisplayNameWithArticle(), bountyCredits);
            para = info.addPara(bullet, opad, Misc.getTextColor(), offeringFaction.getColor(), offeringPerson.getName().getFullName(), offeringPerson.getPost(), offeringFaction.getDisplayNameWithArticle(), bountyCredits);
        }
        else {
            info.addImage(offeringFaction.getLogo(), width, 128f, opad);
            var bullet = String.format("You have received rumors that someone within %s put out a bounty on your head for %s credits.", offeringFaction.getDisplayNameWithArticle(), bountyCredits);
            para = info.addPara(bullet, opad, Misc.getTextColor(), offeringFaction.getColor(), offeringFaction.getDisplayNameWithArticle(), bountyCredits);
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
                    text = "The contract expired without a taker";
                    break;
                case END_PLAYER_NO_REWARD:
                    text = "The bounty hunter fleet has been defeated.";
                    break;
                case END_TIME:
                    text = "The contract has expired and the bounty hunter fleet has given up.";
                    break;
                case END_PLAYER_NO_BOUNTY:
                    text = offeringFaction.getDisplayNameWithArticle() + " " + offeringFaction.getDisplayNameIsOrAre() + " no longer hostile. The bounty has been terminated.";
                    break;
            }

            info.addPara(text, opad, Misc.getTextColor(), Misc.getHighlightColor(), offeringFaction.getDisplayNameWithArticle());

        }
        else if (intel.assembling) {
            String dtl = Math.round(intel.daysToLaunch) + " " + BaseIntelPlugin.getDaysString(intel.daysToLaunch);
            text = String.format("The contract is open for %s", dtl);
            info.addPara(text, opad, Misc.getTextColor(), highlightColor, dtl);
        }
        else {
            String personality = (String) CollectionUtils.getRandomEntry(NameStringCollection.piratePersonalities);
            String durStr = Misc.getAtLeastStringForDays((int) intel.getDuration());
            text = String.format("The contract has been given to the %s bounty hunter %s %s. %s will pursue you for %s", personality, fleet.getCommander().getRank(), fleet.getCommander().getNameString(), fleet.getCommander().getHisOrHer(), durStr);
            info.addPara(text, opad, Misc.getTextColor(), Misc.getHighlightColor(), fleet.getCommander().getNameString(), durStr);

            addBulletPoints(baseBountyIntel, info, ListInfoMode.IN_DESC);

            DescriptionUtils.generateFancyCommanderDescription(info, opad, fleet, fleet.getCommander());

            DescriptionUtils.generateFancyFleetDescription(info, opad, fleet, fleet.getCommander());

            info.addSectionHeading("Fleet Intel", baseBountyIntel.getFactionForUIColors().getBaseUIColor(), baseBountyIntel.getFactionForUIColors().getDarkUIColor(), Alignment.MID, opad);

            int cols = 1;
            int rows = 1;
            float iconSize = width / 3;
            if (!Settings.isDebugActive())
                info.addShipList(cols, rows, iconSize, Color.BLACK, flagshipCopy, opad);
            info.addPara("Intercepted communications suggest that " + fleet.getCommander().getHisOrHer() + " fleet contains roughly %s additional " + singularOrPlural(obfuscatedFleetSize, "ship") + ".",
                    opad, highlightColor, String.valueOf(obfuscatedFleetSize));
            DescriptionUtils.generateThreatDescription(info, fleet, opad);

            if (Settings.isDebugActive()) {
                info.addPara("Debug information", opad);
                intel.bullet(info);
                DescriptionUtils.generateShipListForIntel(info, width, opad, fleet, fleet.getNumShips(), 1, false);
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

    private java.util.List<FleetMemberAPI> getFlagshipCopy() {
        java.util.List<FleetMemberAPI> copyList = new ArrayList<>();

        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        boolean deflate = false;
        if (!fleet.isInflated()) {
            fleet.inflateIfNeeded();
            deflate = true;
        }
        for (FleetMemberAPI member : members) {
            if (!member.isFlagship())
                continue;

            copyList.add(member);
        }
        if (deflate)
            fleet.deflate();
        return copyList;
    }

}