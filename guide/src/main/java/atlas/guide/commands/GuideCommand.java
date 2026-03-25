package atlas.guide.commands;

import api.mod.StarMod;
import api.network.packets.PacketUtil;
import api.utils.game.chat.CommandInterface;
import atlas.core.network.PlayerActionCommandPacket;
import atlas.guide.AtlasGuide;
import org.schema.game.common.data.player.PlayerState;

import javax.annotation.Nullable;

/**
 * {@code /guide} command — sends an OPEN_GUIDE action packet to the requesting client
 * so the guide dialog opens on their screen.
 */
public class GuideCommand implements CommandInterface {

	@Override
	public String getCommand() {
		return "guide";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"glossar", "glossary"};
	}

	@Override
	public String getDescription() {
		return "Opens the in-game guide.";
	}

	@Override
	public boolean isAdminOnly() {
		return false;
	}

	@Override
	public boolean onCommand(PlayerState playerState, String[] args) {
		PacketUtil.sendPacket(playerState, new PlayerActionCommandPacket(AtlasGuide.OPEN_GUIDE));
		return true;
	}

	@Override
	public void serverAction(@Nullable PlayerState playerState, String[] args) {}

	@Override
	public StarMod getMod() {
		return AtlasGuide.getInstance();
	}
}
