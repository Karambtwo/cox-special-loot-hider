package com.coxspecialloothider;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatMessageManager;
import java.util.ArrayList;

@Slf4j
@PluginDescriptor(name = "CoX Censor")

public class CoxSpecialLootHiderPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private CoxSpecialLootHiderConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	private ArrayList<String> turnOffMessages = new ArrayList<String>();
	private ArrayList<MessageNode> lootMessageNodes = new ArrayList<MessageNode>();
	private boolean chestLooted;

	private static final String[] listOfItems = {"Dexterous prayer scroll", "Arcane prayer scroll", "Twisted buckler",
			"Dragon hunter crossbow", "Dinh's bulwark", "Ancestral hat", "Ancestral robe top", "Ancestral robe bottom",
			"Dragon claws", "Elder maul", "Kodai insignia", "Twisted bow"};

	@Override
	protected void startUp() {
		turnOffMessages.clear();
		client.refreshChat();
	}

	@Override
	protected void shutDown() {
		revealLoot();
		chestLooted = false;
	}

	private void revealLoot() {
		// Shows the player the messages that were censored
		// On completion of another raid, the list is cleared

		for (int i = 0; i < turnOffMessages.size(); i++) {
			lootMessageNodes.get(i).setValue(turnOffMessages.get(i));
		}

		client.refreshChat();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
		final int chestID = WidgetID.CHAMBERS_OF_XERIC_REWARD_GROUP_ID;
		if (widgetLoaded.getGroupId() == chestID) {
			if (chestLooted) {
				return;
			}
			chestLooted = true;
			revealLoot();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		// solo mode
		if (config.soloOnly() && client.getVar(Varbits.RAID_PARTY_SIZE) > 1) {
			return;
		}

		if (chatMessage.getType() == ChatMessageType.FRIENDSCHATNOTIFICATION) {
			//Shown when completing a raid. When this happens, the previous loots are cleared from our storage of
			//messages so that the player only sees items from the last raid when turning off the plugin
			if(chatMessage.getMessage().contains("Congratulations - your raid is complete!")){
				turnOffMessages.clear();
				lootMessageNodes.clear();
				chestLooted = false;
			}

			if (chestLooted) { return; }

			//Iterating through the list of CoX uniques
			for (String item : listOfItems) {

				//Check if item is in the message
				if(chatMessage.getMessage().contains(item)) {

					//Adds it to the list of messages for the when plugin is turned off
					turnOffMessages.add(chatMessage.getMessage());
					
					//Replaces item name with ???
					String msg = chatMessage.getMessage().replace(item, "???");

					//Changing the message of the chatMessage. This only sets the message on the backend
					chatMessage.setMessage(msg);

					//Updating it on the UI end
					final MessageNode messageNode = chatMessage.getMessageNode();
					messageNode.setRuneLiteFormatMessage(msg);
					chatMessageManager.update(messageNode);
					client.refreshChat();

					lootMessageNodes.add(messageNode);  // store the message node for reveal
					break;
				}
			}
		}
	}

	@Provides
	CoxSpecialLootHiderConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(CoxSpecialLootHiderConfig.class);
	}
}
