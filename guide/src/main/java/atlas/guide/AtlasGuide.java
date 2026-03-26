package atlas.guide;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.misc.ControlBindingData;
import atlas.core.manager.PlayerActionRegistry;
import atlas.core.network.PlayerActionCommandPacket;
import atlas.guide.commands.GuideCommand;
import atlas.guide.tests.GuideManagerTest;
import org.schema.game.server.test.TestRegistry;
import atlas.guide.gui.GuideDialog;
import atlas.guide.manager.GuideManager;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

/**
 * AtlasGuide — modular in-game guide viewer.
 *
 * <p>Depends on AtlasCore. Registers a {@code /guide} command and a GUIDE top-bar
 * button that opens a markdown-rendered guide dialog.
 */
public class AtlasGuide extends StarMod implements IAtlasSubMod {

	private static AtlasGuide instance;

	/** The action ID returned by {@link PlayerActionRegistry} for opening the guide. */
	public static int OPEN_GUIDE;

	public AtlasGuide() {
		instance = this;
	}

	public static AtlasGuide getInstance() {
		return instance;
	}

	// ── StarMod lifecycle ────────────────────────────────────────────────────

	@Override
	public void onEnable() {
		SubModRegistry.register(this);
	}

	@Override
	public void onDisable() {}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		GuideManager.loadDocs(this);
		java.io.File docsDir = new java.io.File(getSkeleton().getResourcesFolder(), "docs");
		GuideManager.loadDocsFromDirectory(docsDir, this);
		ControlBindingData.load(this);
		ControlBindingData.registerBinding(this, "Open Guide", "Opens the Guide menu.", 181 /* F5 */);
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {}

	@Override
	public void onRegisterTests(TestRegistry.ModTestRegistrar registrar) {
		registrar.register(GuideManagerTest.class);
	}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public String getModId() {
		return "atlas_guide";
	}

	@Override
	public StarMod getMod() {
		return this;
	}

	@Override
	public void onAtlasCoreReady() {
		OPEN_GUIDE = PlayerActionRegistry.register(args -> openGuide());
		StarLoader.registerCommand(new GuideCommand());
	}

	@Override
	public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
		playerDropdown.addExpandedButton("GUIDE", new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) openGuide();
			}
			@Override
			public boolean isOccluded() { return false; }
		}, new GUIActivationHighlightCallback() {
			@Override
			public boolean isHighlighted(InputState inputState) { return false; }
			@Override
			public boolean isVisible(InputState inputState) { return true; }
			@Override
			public boolean isActive(InputState inputState) { return true; }
		});
	}

	@Override
	public void onKeyPress(String bindingName) {
		if("Open Guide".equals(bindingName)) openGuide();
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static void openGuide() {
		api.utils.textures.StarLoaderTexture.runOnGraphicsThread(() -> new GuideDialog().activate());
	}
}
