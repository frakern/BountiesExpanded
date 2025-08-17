package de.schafunschaf.bountiesexpanded.scripts.campaign.intel.bounties.deserter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import de.schafunschaf.bountiesexpanded.Settings;
import de.schafunschaf.bountiesexpanded.helper.text.DescriptionUtils;
import de.schafunschaf.bountiesexpanded.helper.ui.TooltipAPIUtils;
import de.schafunschaf.bountiesexpanded.scripts.campaign.intel.NameStringCollection;
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
import java.util.Collections;

import static com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNotNull;
import static de.schafunschaf.bountiesexpanded.util.ComparisonTools.isNull;
import static de.schafunschaf.bountiesexpanded.util.FormattingTools.singularOrPlural;

@Getter
@Setter
public class DeserterBountyEntity implements BountyEntity {
    private String deserterBountyIcon;
    private final int baseReward;
    private final int level;
    private final float fleetQuality;
    private final Difficulty difficulty;
    private final FactionAPI targetedFaction;
    private final FactionAPI offeringFaction;
    private final CampaignFleetAPI fleet;
    private final PersonAPI targetedPerson;
    private final FleetMemberAPI flagship;
    private final SectorEntityToken spawnLocation;
    private final SectorEntityToken travelDestination;
    private final MissionHandler missionHandler;
    private float targetRepBeforeBattle;
    private DeserterBountyIntel bountyIntel;
    private final String misdeed1;
    private final String misdeed2;

    public DeserterBountyEntity(int baseReward, int level, float fleetQuality, Difficulty difficulty, FactionAPI offeringFaction, CampaignFleetAPI fleet, PersonAPI targetedPerson, SectorEntityToken spawnLocation, SectorEntityToken travelDestination, MissionHandler missionHandler) {
        this.baseReward = baseReward;
        this.level = level;
        this.fleetQuality = fleetQuality;
        this.difficulty = difficulty;
        this.targetedFaction = offeringFaction;
        this.offeringFaction = offeringFaction;
        this.fleet = fleet;
        this.targetedPerson = targetedPerson;
        this.flagship = fleet.getFleetData().getMemberWithCaptain(targetedPerson);
        this.spawnLocation = spawnLocation;
        this.travelDestination = travelDestination;
        this.missionHandler = missionHandler;
        this.deserterBountyIcon = "bountiesExpanded_deserter_crest";
        ArrayList<String> crimeReasonsCopy = new ArrayList<>(NameStringCollection.deserterMisdeeds);
        Collections.shuffle(crimeReasonsCopy);
        this.misdeed1 = buildMisdeedsString(crimeReasonsCopy.get(0));
        this.misdeed2 = buildMisdeedsString(crimeReasonsCopy.get(1));
    }

    @Override
    public BaseEventManager getBountyManager() {
        return DeserterBountyManager.getInstance();
    }

    @Override
    public PersonAPI getOfferingPerson() {
        return null;
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

        if (isNull(result)) {
            info.addPara("Offered by: %s", initPad, bulletColor, offeringFaction.getBaseUIColor(), Misc.ucFirst(offeringFaction.getDisplayNameWithArticle()));
            info.addPara("Reward: %s", bulletPadding, bulletColor, highlightColor, Misc.getDGSCredits(baseReward));
            info.addPara("Time left: %s" + singularOrPlural(days, " day"), bulletPadding, bulletColor, highlightColor, String.valueOf(days));
        } else {
            switch (result.type) {
                case END_PLAYER_BOUNTY:
                    String payout = Misc.getDGSCredits(result.payment);

                    if (mode != ListInfoMode.IN_DESC) {
                        info.addPara("%s received", initPad, bulletColor, highlightColor, payout);
                        CoreReputationPlugin.addAdjustmentMessage(result.rep.delta, offeringFaction, null,
                                null, null, info, bulletColor, isUpdate, 0f);
                    }
                    break;
                case END_PLAYER_NO_BOUNTY:
                case END_PLAYER_NO_REWARD:
                case END_TIME:
                case END_OTHER:
                    break;
            }
        }

        baseBountyIntel.unindent(info);
    }

    @Override
    public void createSmallDescription(BaseBountyIntel baseBountyIntel, TooltipMakerAPI info, float width, float height) {
        boolean isRetrievalMission = false;
        Color highlightColor = Misc.getHighlightColor();
        String hisOrHer = getTargetedPerson().getHisOrHer();
        String briefingText = String.format("A bounty has been put on the head of %s, wanted dead for %s, %s, and betrayal of %s.\n\n" +
                        "To claim this bounty, we need to end %s life by destroying the %s.",
                targetedPerson.getNameString(), misdeed1, misdeed2, offeringFaction.getDisplayNameWithArticle(), hisOrHer, flagship.getShipName());
        Color factionColor = baseBountyIntel.getFactionForUIColors().getBaseUIColor();
        BountyResult result = baseBountyIntel.getResult();
        float opad = 10f;
        int maxShipsOnIntel = 7;
        boolean showShipsRemaining = fleet.getNumShips() > maxShipsOnIntel;

        if (isNull(result)) {
            TooltipAPIUtils.addCustomImagesWithSingleRepBar(info, width, opad, 10f,
                    targetedPerson.getPortraitSprite(),
                    offeringFaction.getLogo(), offeringFaction.getRelToPlayer().getRel());
            Color[] highlightColors = new Color[]{factionColor, highlightColor, highlightColor, highlightColor, factionColor, factionColor};
            info.addSectionHeading("Briefing", factionColor, baseBountyIntel.getFactionForUIColors().getDarkUIColor(), Alignment.MID, opad);
            info.addPara(briefingText, opad, highlightColors, targetedPerson.getNameString(), misdeed1, misdeed2, "betrayal", offeringFaction.getDisplayNameWithArticle(), flagship.getShipName());

            addBulletPoints(baseBountyIntel, info, ListInfoMode.IN_DESC);

            DescriptionUtils.generateFancyFleetDescription(info, opad, fleet, targetedPerson);
            DescriptionUtils.generateFancyCommanderDescription(info, opad, fleet, targetedPerson);

            if (fleet.getContainingLocation() == travelDestination.getContainingLocation()) {
                DescriptionUtils.generateDestinationFakeHideoutDescription(info, baseBountyIntel, opad);
            } else {
                DescriptionUtils.generateFakeTravelDescription(info, baseBountyIntel, opad);
            }

            info.addSectionHeading("Fleet Intel", factionColor, baseBountyIntel.getFactionForUIColors().getDarkUIColor(), Alignment.MID, isRetrievalMission ? 0f : opad);
            info.addPara("The bounty posting also contains partial intel on some of the ships under " + targetedPerson.getHisOrHer() + " command.", opad);
            if (!Settings.isDebugActive()) {
                DescriptionUtils.generateShipListForIntel(info, width, opad, fleet, maxShipsOnIntel, true, true, showShipsRemaining);
            }
            else {
                DescriptionUtils.generateFullShipListForIntel(info, width, opad, fleet, false);
                info.addPara("FLEET QUALITY: " + fleetQuality, 0f);
                info.addPara("TIER: " + getLevel(), 0f);
                info.addPara("DIFFICULTY: %s",
                        0f, difficulty.getColor(), difficulty.getShortDescription());
            }
        } else {
            switch (result.type) {
                case END_PLAYER_BOUNTY:
                    String debriefingText = "Mission completed. %s has been eliminated.";

                    TooltipAPIUtils.addCustomImagesWithSingleRepBarAndChange(info, width, opad, 10f,
                            targetedPerson.getPortraitSprite(),
                            offeringFaction.getLogo(), offeringFaction.getRelToPlayer().getRel(), result.rep.delta);
                    info.addSectionHeading("Briefing", factionColor, baseBountyIntel.getFactionForUIColors().getDarkUIColor(), Alignment.MID, opad);
                    info.addPara(briefingText, Misc.getGrayColor(), opad);

                    info.addPara(debriefingText, opad, factionColor, targetedPerson.getFaction().getRank(targetedPerson.getRankId()) + " " + targetedPerson.getNameString());
                    baseBountyIntel.bullet(info);
                    info.addPara("%s Credits received", opad, highlightColor, Misc.getDGSCredits(result.payment));
                    baseBountyIntel.unindent(info);
                    break;
                case END_PLAYER_NO_BOUNTY:
                case END_PLAYER_NO_REWARD:
                case END_OTHER:
                case END_TIME:
                    TooltipAPIUtils.addCustomImagesWithSingleRepBar(info, width, opad, 10f,
                            targetedPerson.getPortraitSprite(),
                            offeringFaction.getLogo(), offeringFaction.getRelToPlayer().getRel());
                    info.addSectionHeading("Briefing", factionColor, baseBountyIntel.getFactionForUIColors().getDarkUIColor(), Alignment.MID, opad);
                    info.addPara(briefingText, Misc.getGrayColor(), opad);

                    info.addPara("This mission is no longer on offer.", opad);
                    break;
            }
        }
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", deserterBountyIcon);
    }

    @Override
    public String getTitle(BountyResult result) {
        if (isNotNull(result)) {
            switch (result.type) {
                case END_PLAYER_BOUNTY:
                    return "Deserter Bounty - Completed";
                case END_PLAYER_NO_BOUNTY:
                case END_PLAYER_NO_REWARD:
                case END_OTHER:
                case END_TIME:
                    return "Deserter Bounty - Failed";
            }
        }
        return String.format("Deserter Bounty - %s", targetedPerson.getNameString());
    }

    private String buildMisdeedsString(String misdeed) {
        String returnString = misdeed;

        returnString = returnString.replace("$faction", offeringFaction.getDisplayName());
        returnString = returnString.replace("$market", spawnLocation.getMarket().getName());
        returnString = returnString.replace("$facLeader", offeringFaction.getPost(Ranks.FACTION_LEADER));
        returnString = returnString.replace("$aOrAnFaction", FormattingTools.aOrAn(offeringFaction.getDisplayName()) + " " + offeringFaction.getDisplayName());

        return returnString;
    }


}
