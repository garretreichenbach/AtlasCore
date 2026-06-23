package atlas.contracts.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import atlas.contracts.AtlasContracts;
import atlas.contracts.data.contract.ContractDataManager;
import org.apache.commons.lang3.math.NumberUtils;
import org.schema.game.common.data.player.PlayerState;

import javax.annotation.Nullable;

public class RandomContractsCommand implements CommandInterface {

	@Override
	public String getCommand() {
		return "contracts_random";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"contract_random", "random_contracts", "random_contract"};
	}

	@Override
	public String getDescription() {
		return "Creates randomly generated contracts and adds them to the contracts list.\n" + "- /%COMMAND% [amount] : Generates [amount] random contracts, or a single one if no amount is specified.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args != null && args.length == 1) {
			if(NumberUtils.isNumber(args[0].trim()) && Integer.parseInt(args[0].trim()) > 0) {
				generateRandomContract(sender, Integer.parseInt(args[0].trim()));
			} else {
				return false;
			}
		} else if(args == null || args.length == 0) {
			generateRandomContract(sender, 1);
		} else {
			return false;
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

	private void generateRandomContract(PlayerState sender, int amount) {
		int i;
		for(i = 0; i < amount; i++) {
			ContractDataManager.getInstance(sender.isOnServer()).generateRandomContract();
		}
		PlayerUtils.sendMessage(sender, "Generated " + i + " random contracts.");
	}
}
