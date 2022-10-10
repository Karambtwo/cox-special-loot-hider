package com.coxspecialloothider;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatMessageManager;
import java.util.ArrayList;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;

import static net.runelite.api.ChatMessageType.FRIENDSCHAT;

@Slf4j
@PluginDescriptor(
	name = "CoX Censor"
)
public class CoxSpecialLootHiderPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private CoxSpecialLootHiderConfig config;

	private ArrayList<Tuple<MessageNode, String>> revealMessages = new ArrayList<>();
	private ArrayList<Tuple<String, String>> listOfLoot = new ArrayList<>();
	private boolean itemReceived = false;

	private static final String[] listOfItems = {"Dexterous prayer scroll", "Arcane prayer scroll", "Twisted buckler",
			"Dragon hunter crossbow", "Dinh's bulwark", "Ancestral hat", "Ancestral robe top", "Ancestral robe bottom",
			"Dragon claws", "Elder maul", "Kodai insignia", "Twisted bow"};

	@Override
	protected void startUp() throws Exception
	{
		client.refreshChat();
	}

	@Override
	protected void shutDown() throws Exception
	{
		showLoot();
	}

	private void clearLists(){
		revealMessages.clear();
		listOfLoot.clear();
		itemReceived = false;
		client.refreshChat();
	}

	//Replaces censored messages with the originals
	private void showLoot(){
		for(Tuple<MessageNode, String> message : revealMessages){
			//Gets the original message as string, sets the string at the referenced ChatMessage
			message.getFirst().setValue(message.getSecond());
			message.getFirst().setRuneLiteFormatMessage(message.getSecond());
		}
		clearLists();
		client.refreshChat();
	}

	//Shows purple upon looting the chest, private storage, or bank
	//If no purple, it wont do anything. If there is, it will un-hide it
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded wid){
		if(wid.getGroupId() == WidgetID.CHAMBERS_OF_XERIC_REWARD_GROUP_ID ||
				wid.getGroupId() == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_GROUP_ID ||
				wid.getGroupId() == WidgetID.BANK_GROUP_ID){
			System.out.print("Hello, you are showing the loot");//TODO REMOVE
			showLoot();
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event) {
		if (!"chatFilterCheck".equals(event.getEventName()) || !itemReceived) {
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		String[] stringStack = client.getStringStack();
		int stringStackSize = client.getStringStackSize();

		final int messageType = intStack[intStackSize - 2];
		final int messageId = intStack[intStackSize - 1];
		String message = stringStack[stringStackSize - 1];

		ChatMessageType chatMessageType = ChatMessageType.of(messageType);
		final MessageNode messageNode = client.getMessages().get(messageId);

		if (chatMessageType == ChatMessageType.FRIENDSCHATNOTIFICATION){

			//Clear list of currently censored items in case the player did not reveal previously
			if(message.contains("Congratulations - your raid is complete!")){
				clearLists();
				return;
			}


			//Iterating through the list of CoX uniques
			for (String item : listOfItems){
				System.out.println("Message: " + message + ", item: " + item + ". Does message contain item?: ");//TODO remove after testing
				System.out.println(message.contains(item));//TODO remove after testing

				//Check if CoX unique is in the message
				if(message.contains(item)){
					System.out.println("CHAT CONTAINS AN ITEM. NEW LOOT RECEIVED: ");//TODO remove after testing
					String censored = message.replace(item, "???");
					System.out.println(censored);//TODO remove after testing
					revealMessages.add(new Tuple(messageNode, message));
					System.out.println("Reveal message added: " + new Tuple(messageNode, message));//TODO remove after testing
					messageNode.setValue(censored);
					messageNode.setRuneLiteFormatMessage(censored);
					break;
				}
			}
		}
		else if(itemReceived){
			//If GAMEMESSAGE, check if it a collection log item and if it was received in the raid
			if(chatMessageType == ChatMessageType.GAMEMESSAGE &&
					message.contains("New item added to your collection log:")){
				for (Tuple<String, String> loot : listOfLoot) {
					if(message.contains(loot.getSecond())) {
						revealMessages.add(new Tuple(messageNode, message));
						intStack[intStackSize - 3] = 0;
					}
				}
			}
			//If CLAN/GIM CLAN MESSAGE, either censor the loot if special raid message or remove
			//Collection log broadcasts
			else if((chatMessageType == ChatMessageType.CLAN_MESSAGE ||
					chatMessageType == ChatMessageType.CLAN_GIM_MESSAGE)){
				//See it it contains both the player and item that received loot
				System.out.println("CLAN BROADCAST TRIGGERED AFTER ITEM RECEIVED");//TODO remove after testing
				for (Tuple<String, String> loot : listOfLoot) {
					//First is name, Second is item
					if (message.contains(loot.getSecond()) && message.contains(loot.getFirst())) {
						if(message.contains("received a new collection log item:")) {
							revealMessages.add(new Tuple(messageNode, message));
							intStack[intStackSize - 3] = 0;
							break;
						}
						else{
							String value = message.substring(0, message.indexOf(":")) + ": ???";
							revealMessages.add(new Tuple(messageNode, message));
							System.out.println("SPECIAL LOOT FROM RAID MESSAGE AND REVEAL ADDED: " + message);//TODO remove after testing
							messageNode.setValue(value);
							messageNode.setRuneLiteFormatMessage(value);
						}
					}
				}
			}
		}
	}


	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		System.out.println("New Message " + chatMessage + " and the sender: " + chatMessage.getSender());//TODO REMOVE AFTER TESTING

		/*Leave this for testing if modifications are needed.
		if (chatMessage.getType() == ChatMessageType.OBJECT_EXAMINE) {
			client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION,"", "Karambtwo - Twisted bow", "");
			//client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION,"", "OtherAccount - Dragon claws", "");
		}
		if (chatMessage.getType() == ChatMessageType.ITEM_EXAMINE) {
			client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "Karambtwo received special loot from a raid: Twisted bow (1,680,988,483)", "Valiance");
			client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "Karambtwo received a new collection log item: Twisted bow (507/1,587)", "Valiance");
			client.addChatMessage(ChatMessageType.GAMEMESSAGE,"", "New item added to your collection log: Twisted bow (507/1,587)", "");

			//client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "OtherAccount received special loot from a raid: Dragon claws (1,680,988,483)", "Valiance");
			//client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "OtherAccount received a new collection log item: Dragon claws (507/1,587)", "Valiance");
		}
		if (chatMessage.getType() == ChatMessageType.NPC_EXAMINE) {
			client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "Karambtwo received special loot from a raid: Dragon claws (1,200,000)", "Valiance");
			client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "Karambtwo received a new collection log item: Dragon claws (507/1,587)", "Valiance");

			//client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "OtherAccount received special loot from a raid: Twisted bow (1,680,988,483)", "Valiance");
			//client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "OtherAccount received a new collection log item: Twisted bow (507/1,587)", "Valiance");
		}
		if (chatMessage.getType() == ChatMessageType.FRIENDSCHAT) {
			client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "Karambtwo received special loot from a raid: Tumeken's shadow (uncharged) (1,680,988,483)", "Valiance");
			client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "Karambtwo received a new collection log item: Tumeken's shadow (uncharged) (507/1,587)", "Valiance");
			client.addChatMessage(ChatMessageType.GAMEMESSAGE,"", "New item added to your collection log: Tumeken's shadow (uncharged) (507/1,587)", "");

			//client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "OtherAccount received special loot from a raid: Twisted bow (1,680,988,483)", "Valiance");
			//client.addChatMessage(ChatMessageType.CLAN_MESSAGE,"", "OtherAccount received a new collection log item: Twisted bow (507/1,587)", "Valiance");
		}*/


		//Check FC broadcasts to see if a raid was completed
		//This sets the trigger for chat filtering and maintains list of loot
		if (chatMessage.getType() == ChatMessageType.FRIENDSCHATNOTIFICATION){

			//Clear list of currently censored items in case the player did not reveal previously
			if(chatMessage.getMessage().contains("Congratulations - your raid is complete!")){
				clearLists();
				return;
			}


			//Iterating through the list of CoX uniques
			for (String item : listOfItems){
				//Check if CoX unique is in the message. Flip flag and add to loot received
				if(chatMessage.getMessage().contains(item)){
					itemReceived = true;
					String name = chatMessage.getMessage().substring(0, chatMessage.getMessage().indexOf("-") - 1);
					listOfLoot.add(new Tuple(name, item));
					System.out.println("Loot added: " + new Tuple(name, item).toString());//TODO remove after testing
					//censorMessage() //TODO remove after testing
					break;
				}
			}
		}
	}

	/* TODO Remove is testing goes well
	private void censorMessage(ChatMessage chatMessage, String item){

		//Adds it to the list of messages for the when plugin is turned off
		//revealMessages.add(new Tuple<ChatMessage, String>(chatMessage, chatMessage.getMessage()));
		System.out.println("Reveal message added: " + new Tuple<ChatMessage, String>(chatMessage, chatMessage.getMessage()));//TODO REMOVE
		String msg;

		//If Clan or GIM Clan broadcast, use substring to price does not show
		if(chatMessage.getType() == ChatMessageType.CLAN_MESSAGE ||
				chatMessage.getType() == ChatMessageType.CLAN_GIM_MESSAGE) {
			//Remove completely if collection log. Dont spoil uniques

			if(chatMessage.getMessage().contains("received a new collection log item:")){
				msg = "";//null;//TODO MAKE THIS NOT TAKE UP A A LINE. MAYBE JUST DELETE NODE
				//chatMessage.getMessageNode().unlink();
				//chatLineBuffer.removeMessageNode()
			}
			else{//Normal raid clan broadcast
				msg = chatMessage.getMessage().substring(0, chatMessage.getMessage().indexOf(":")) + ": ???";
			}
		}
		else if(chatMessage.getType() == ChatMessageType.GAMEMESSAGE) {
			msg = "";
		}
		else{
			//Replaces item with ???
			msg = chatMessage.getMessage().replace(item, "???");
		}

		System.out.print("HERE WAS THE CENSORED MSG: " + msg);//TODO REMOVE AFTER TESTING

		//Changing the message of the chatMessage. This only sets the message on the backend
		chatMessage.setMessage(msg);

		//Updating it on the UI end
		final MessageNode messageNode = chatMessage.getMessageNode();
		messageNode.setRuneLiteFormatMessage(msg);
		client.refreshChat();
	}*/

	@Provides
	CoxSpecialLootHiderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoxSpecialLootHiderConfig.class);
	}
}

class Tuple<T, U> {
	T first;
	U second;

	public Tuple(T first, U second){
		this.first = first;
		this.second = second;
	}

	public T getFirst(){
		return first;
	}

	public U getSecond(){
		return second;
	}

	public String toString(){
		return "(" + getFirst() + "," + getSecond() + ")";
	}
}
