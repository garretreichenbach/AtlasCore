package atlas.contracts.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import atlas.contracts.AtlasContracts;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.data.contract.ContractDataManager;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import org.schema.game.common.data.player.PlayerState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class ListContractsCommand implements CommandInterface {

	@Override
	public String getCommand() {
		return "contracts_list";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"contract_list", "list_contracts", "list_contract"};
	}

	@Override
	public String getDescription() {
		return "Lists active contracts for the sender (or a player), with ids and rewards. Can be filtered by type.\n" + "- /%COMMAND% [player] [type...]";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args == null || args.length == 0) {
			listContracts(sender, PlayerDataManager.getInstance(sender.isOnServer()).getFromName(sender.getName(), sender.isOnServer()));
		} else {
			PlayerData target = PlayerDataManager.getInstance(sender.isOnServer()).getFromName(args[0], sender.isOnServer());
			if(target == null) {
				PlayerUtils.sendMessage(sender, "Player " + args[0] + " doesn't exist!");
			} else if(args.length > 1) {
				listContracts(sender, target, Arrays.copyOfRange(args, 1, args.length));
			} else {
				listContracts(sender, target);
			}
		}
		return true;
	}

	@Override
	public void serverAction(@Nullable PlayerState sender, String[] args) {
	}

	@Override
	public StarMod getMod() {
		return AtlasContracts.getInstance();
	}

	private void listContracts(PlayerState sender, PlayerData target, String... filter) {
		if(target == null) {
			PlayerUtils.sendMessage(sender, "Player data not found.");
			return;
		}
		// Parse optional type filters.
		ArrayList<ContractData.ContractType> types = new ArrayList<>();
		if(filter != null && filter.length > 0) {
			for(String s : filter) {
				ContractData.ContractType type = ContractData.ContractType.fromString(s);
				if(type == null) {
					PlayerUtils.sendMessage(sender, s + " is not a valid contract type.");
					return;
				}
				types.add(type);
			}
		}
		PlayerUtils.sendMessage(sender, "Contracts for " + target.getName() + " (Faction ID: " + target.getFactionId() + "):");
		if(target.getContracts() == null || target.getContracts().isEmpty()) {
			PlayerUtils.sendMessage(sender, "No active contracts.");
			return;
		}
		ContractDataManager manager = ContractDataManager.getInstance(sender.isOnServer());
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for(String uuid : target.getContracts()) {
			ContractData contract = manager.getFromUUID(uuid, sender.isOnServer());
			if(contract == null) continue; // stale uuid (contract removed)
			if(!types.isEmpty() && !types.contains(contract.getContractType())) continue;
			builder.append("\n- ").append(contract.getName())
					.append(" [").append(contract.getContractType().displayName).append("]")
					.append(" — ").append(contract.getReward()).append(" Gold Bars")
					.append(" (id: ").append(uuid).append(")");
			count++;
		}
		if(count > 0) PlayerUtils.sendMessage(sender, builder.toString().trim());
		else PlayerUtils.sendMessage(sender, "No contracts match the specified filter.");
	}
}
