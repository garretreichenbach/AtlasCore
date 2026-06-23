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
import java.util.List;

public class CompleteContractsCommand implements CommandInterface {

	@Override
	public String getCommand() {
		return "contracts_complete";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"contract_complete", "complete_contracts", "complete_contract"};
	}

	@Override
	public String getDescription() {
		return "Force-completes a contract.\n" + "- /%COMMAND% <contract id|all/*> [player] : Completes the contract for the sender, or a specified player.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args.length < 1) return false;
		PlayerData target;
		if(args.length > 1) {
			target = PlayerDataManager.getInstance(sender.isOnServer()).getFromName(args[1], sender.isOnServer());
			if(target == null) {
				PlayerUtils.sendMessage(sender, "Player " + args[1] + " doesn't exist!");
				return true;
			}
		} else {
			target = PlayerDataManager.getInstance(sender.isOnServer()).getFromName(sender.getName(), sender.isOnServer());
		}
		if(target == null) {
			PlayerUtils.sendMessage(sender, "Player data not found.");
			return true;
		}
		ContractDataManager manager = ContractDataManager.getInstance(sender.isOnServer());
		if(args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("*")) {
			// Resolve the player's claimed UUIDs to live contracts, skipping any that no longer exist.
			List<ContractData> contracts = new ArrayList<>();
			for(String uuid : new ArrayList<>(target.getContracts())) {
				ContractData contract = manager.getFromUUID(uuid, sender.isOnServer());
				if(contract != null) contracts.add(contract);
			}
			completeContracts(sender, target, contracts);
		} else {
			ContractData contract = manager.getFromUUID(args[0].trim(), sender.isOnServer());
			if(contract != null) {
				List<ContractData> single = new ArrayList<>();
				single.add(contract);
				completeContracts(sender, target, single);
			} else {
				PlayerUtils.sendMessage(sender, "No valid contract found with id " + args[0].trim());
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

	private void completeContracts(PlayerState sender, PlayerData target, List<ContractData> contracts) {
		if(contracts == null || contracts.isEmpty()) {
			PlayerUtils.sendMessage(sender, "No active contracts found for player " + target.getName() + ".");
			return;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("Completed the following contracts for player ").append(target.getName()).append(":");
		int completed = 0;
		for(ContractData contract : contracts) {
			if(contract == null) continue;
			if(!contract.getClaimants().containsKey(target.getName())) {
				PlayerUtils.sendMessage(sender, target.getName() + " has not claimed \"" + contract.getName() + "\".");
				continue;
			}
			builder.append("\n- ").append(contract.getName());
			ContractDataManager.completeContract(target, contract);
			completed++;
		}
		if(completed > 0) PlayerUtils.sendMessage(sender, builder.toString().trim());
		else PlayerUtils.sendMessage(sender, "No matching claimed contracts to complete for " + target.getName() + ".");
	}
}
