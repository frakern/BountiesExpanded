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
import com.fs.starfarer.api.ui.Alignment;
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
    private final String assassinationIcon = "bountiesExpanded_bounty_hunter";
    private final int level;
    private final float fleetQuality;
    private final MissionHandler missionHandler;
    private final Difficulty difficulty;
    private final String personality = (String) CollectionUtils.getRandomEntry(NameStringCollection.piratePersonalities);

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
        return Misc.ucFirst(offeringFaction.getDisplayName()) + " - Bounty Hunter";
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

        info.addPara("A bounty has been put on you", initPad, bulletColor, offeringFaction.getColor(), "bounty");

        var bullet = "";

        if (!isNull(result)) {
            switch (result.type) {
                case END_OTHER:
                    bullet = "Contract canceled";
                    break;
                case END_PLAYER_NO_REWARD:
                    bullet = "Bounty hunter fleet defeated";
                    break;
                case END_TIME:
                    bullet = "Contract expired";
                    break;
                case END_PLAYER_NO_BOUNTY:
                    bullet = "Cancelled: No longer hostile";
                    break;
            }
            info.addPara(bullet, bulletPadding, bulletColor, Misc.getGrayColor());
        }
        else if (intel.assembling) {
            String dtl = DescriptionUtils.getStringForMoreDays((int) intel.daysToLaunch);
            bullet = "The contract is open for %s";
            info.addPara(bullet, bulletPadding, bulletColor, highlightColor, dtl);
        }
        else {
            bullet = String.format("Bounty hunter departed %s", spawnLocation.getMarket().getName());
            info.addPara(bullet, bulletPadding, bulletColor, highlightColor, spawnLocation.getMarket().getName());
        }

        baseBountyIntel.unindent(info);
    }

    @Override
    public void createSmallDescription(BaseBountyIntel baseBountyIntel, TooltipMakerAPI info, float width, float height) {

        Color highlightColor = Misc.getHighlightColor();
        List<FleetMemberAPI> flagshipCopy = getFlagshipCopy();
        BountyResult result = baseBountyIntel.getResult();
        float opad = 10f;
        String bountyCredits = Misc.getDGSCredits(CreditCalculator.getRewardByFP(Global.getSector().getPlayerFleet().getFleetPoints(), difficulty.getModifier() * 5f));

        if (isNotNull(offeringPerson)) {
            TooltipAPIUtils.addPersonWithFactionRepBar(info, width, opad, opad, offeringPerson);
            Color[] highlightColors = new Color[]{offeringFaction.getColor(), Misc.getTextColor(), offeringFaction.getColor(), Misc.getHighlightColor()};
            String bullet = String.format("You have received rumors that %s a %s of %s has grown tired of your meddling in their affairs and has put out a bounty contract on your head for %s.", offeringPerson.getName().getFullName(), offeringPerson.getPost(), offeringFaction.getDisplayNameWithArticle(), bountyCredits);
            info.addPara(bullet, opad, highlightColors, offeringPerson.getName().getFullName(), offeringPerson.getPost(), offeringFaction.getDisplayName(), bountyCredits);
        }
        else {
            info.addImage(offeringFaction.getLogo(), width, 128f, opad);
            Color[] highlightColors = new Color[]{offeringFaction.getColor(), Misc.getHighlightColor()};
            String bullet = String.format("You have received rumors that someone within %s has put out a bounty on your head for %s.", offeringFaction.getDisplayNameWithArticle(), bountyCredits);
            info.addPara(bullet, opad, highlightColors, offeringFaction.getDisplayName(), bountyCredits);
        }

        if (!offeringFaction.getRelToPlayer().isHostile()) {
            info.addSpacer(opad);
            info.addPara("This contract is being issued without the official sanction of its governing faction. Any action is unlikely to cause reductions in reputations.", opad);
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
                    text = "The contract was not claimed.";
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
            String dtl = DescriptionUtils.getStringForMoreDays((int) intel.daysToLaunch);
            text = String.format("The contract will be open for %s or until a someone claims it.", dtl);
            info.addPara(text, opad, Misc.getTextColor(), highlightColor, dtl);
        }
        else {
            String durStr = Misc.getStringForDays((int) intel.getDuration());

            text = String.format("The contract has been claimed by the %s bounty hunter %s %s.", personality, fleet.getCommander().getFaction().getRank(fleet.getCommander().getRankId()), fleet.getCommander().getNameString());
            info.addPara(text, opad, Misc.getTextColor(), fleet.getCommander().getFaction().getColor(), fleet.getCommander().getFaction().getRank(fleet.getCommander().getRankId()), fleet.getCommander().getNameString());

            text = String.format("%s fleet has departed from %s and will pursue you for around %s.", Misc.ucFirst(fleet.getCommander().getHisOrHer()), spawnLocation.getMarket().getName(), durStr);
            info.addPara(text, opad, Misc.getTextColor(), Misc.getHighlightColor(), spawnLocation.getMarket().getName(), durStr);

            // Fleet Intel.
            info.addSectionHeading("Fleet Intel", baseBountyIntel.getFactionForUIColors().getBaseUIColor(), baseBountyIntel.getFactionForUIColors().getDarkUIColor(), Alignment.MID, opad);

            DescriptionUtils.generateFancyFleetDescription(info, opad, fleet, fleet.getCommander());

            DescriptionUtils.generateFancyCommanderDescription(info, opad, fleet, fleet.getCommander());

            int cols = 1;
            int rows = 1;
            float iconSize = width / 3;
            info.addShipList(cols, rows, iconSize, Color.BLACK, flagshipCopy, opad);

            info.addPara(spawnLocation.getMarket().getName() + " spaceport registry records indicate that " + fleet.getCommander().getHisOrHer() + " fleet likely contains around %s additional " + singularOrPlural(obfuscatedFleetSize, "ship") + ".",
                    opad, highlightColor, String.valueOf(obfuscatedFleetSize));
            DescriptionUtils.generateThreatDescription(info, fleet, opad);

            if (Settings.isDebugActive()) {
                intel.bullet(info);
                DescriptionUtils.generateFullShipListForIntel(info, width, opad, fleet);
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
                info.addPara("Time spent looking: " + intel.timeSpentLooking, 0);
                info.addPara("Days left: " + intel.daysLeft, 0);
                info.addPara("Found player yet: " + intel.foundPlayerYet, 0);
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