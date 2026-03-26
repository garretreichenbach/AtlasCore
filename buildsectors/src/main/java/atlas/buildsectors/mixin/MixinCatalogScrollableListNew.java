package atlas.buildsectors.mixin;

import api.common.GameClient;
import atlas.buildsectors.data.BuildSectorData;
import atlas.buildsectors.data.BuildSectorDataManager;
import org.schema.game.client.controller.PlayerGameTextInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.catalog.newcatalog.CatalogScrollableListNew;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.data.EntityRequest;
import org.schema.game.server.data.admin.AdminCommands;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUICheckBoxTextPair;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends the vanilla blueprint catalog LOAD button so that players inside a
 * build sector who have been granted {@link BuildSectorData.PermissionTypes#SPAWN}
 * permission can also spawn blueprints, without needing admin or shipyard-edit access.
 *
 * <p><b>Two injection points:</b></p>
 * <ol>
 *   <li>{@link #isInShipyardEdit()} — {@code @Overwrite} makes the method also
 *       return {@code true} for build-sector SPAWN holders.  Because this method
 *       is called from both the button-attachment guard in
 *       {@code updateListEntries} <em>and</em> from the LOAD button's anonymous
 *       {@link org.schema.schine.graphicsengine.forms.gui.GUICallback} closure,
 *       a single overwrite fixes both without touching the closure's synthetic
 *       method.</li>
 *   <li>{@link #injectBuildSectorLoad} — vanilla's {@code load()} starts with an
 *       early-return guard for non-admin, non-shipyard players.  This inject
 *       intercepts before that guard and, for build-sector SPAWN holders, shows
 *       its own name-input dialog that sends {@code LOAD_AS_FACTION} directly.</li>
 * </ol>
 *
 * <p>Registered in {@code atlasbuildsectors.mixins.json} under {@code "client"}.</p>
 */
@Mixin(value = CatalogScrollableListNew.class, remap = false)
public abstract class MixinCatalogScrollableListNew {

	// ── shadows ──────────────────────────────────────────────────────────────

	@Shadow
	private boolean spawnDocked;
	@Shadow
	private boolean useOwnFaction;

	/**
	 * Returns {@code true} if the given player is currently inside a build sector
	 * that grants the {@link BuildSectorData.PermissionTypes#SPAWN} permission.
	 */
	private static boolean getBuildSectorSpawnPermission(PlayerState player) {
		BuildSectorData sector = BuildSectorDataManager.getInstance(false).getCurrentBuildSector(player);
		if(sector == null) return false;
		return sector.getPermission(player.getName(), BuildSectorData.PermissionTypes.SPAWN);
	}

	@Shadow
	public abstract boolean isPlayerAdmin();

	// ── @Overwrite ───────────────────────────────────────────────────────────

	@Shadow
	private boolean canSpawnDocked() {
		throw new AssertionError();
	}

	// ── @Inject ───────────────────────────────────────────────────────────────

	/**
	 * Replaces the vanilla test "is the player currently in shipyard-edit mode?"
	 * with an extended test that also returns {@code true} when the local player
	 * has been granted {@link BuildSectorData.PermissionTypes#SPAWN} permission
	 * inside a build sector.
	 *
	 * <p>The vanilla implementation is preserved verbatim as the first clause;
	 * the build-sector check is appended with {@code ||}.  All callers of this
	 * method — the button-attachment guard in {@code updateListEntries}, the LOAD
	 * button's inline callback, and {@code load()} itself — therefore see the
	 * combined result automatically.</p>
	 *
	 * @reason Extend LOAD button visibility and access to build-sector SPAWN holders.
	 * @author AtlasCore
	 */
	@Overwrite
	private boolean isInShipyardEdit() {
		return (GameClient.getCurrentControl() instanceof SegmentController && ((SegmentController) GameClient.getCurrentControl()).isVirtualBlueprint()) || getBuildSectorSpawnPermission((GameClient.getClientState()).getPlayer());
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	/**
	 * Intercepts {@code load(CatalogPermission)} <em>before</em> the vanilla
	 * admin-or-shipyard guard runs.
	 *
	 * <p>Vanilla's guard (simplified):</p>
	 * <pre>
	 * if (!isPlayerAdmin()) {
	 *     if (isInShipyardEdit()) { if (!canSpawnDocked()) { error; return; } }
	 *     else { error; return; }
	 * }
	 * </pre>
	 *
	 * <p>For a build-sector SPAWN holder: {@code isInShipyardEdit()} is now
	 * {@code true} (via the overwrite above), but {@code canSpawnDocked()} is
	 * typically {@code false} (they're not in a virtual design), causing vanilla
	 * to abort with "must be looking at a rail block".  This inject catches that
	 * case first, shows its own spawn-name dialog, and cancels the vanilla
	 * execution.</p>
	 *
	 * <p>Admin players and real shipyard-edit players are unaffected: they are
	 * let through to the vanilla path unchanged.</p>
	 */
	@Inject(method = "load", at = @At("HEAD"), cancellable = true)
	private void injectBuildSectorLoad(CatalogPermission permission, CallbackInfo ci) {
		// Admins: vanilla handles everything correctly already.
		if(isPlayerAdmin()) return;

		// Real shipyard-edit: vanilla handles correctly.
		boolean realShipyard = GameClient.getCurrentControl() instanceof SegmentController && ((SegmentController) GameClient.getCurrentControl()).isVirtualBlueprint();
		if(realShipyard) return;

		// Not a build-sector SPAWN holder either — let vanilla error out normally.
		GameClientState state = GameClient.getClientState();
		PlayerState player = state.getPlayer();
		if(!getBuildSectorSpawnPermission(player)) return;

		// Build-sector SPAWN holder: show the name dialog and send LOAD_AS_FACTION.
		openBuildSectorSpawnDialog(permission, state, player);
		ci.cancel();
	}

	/**
	 * Opens a spawn-name input dialog and, on confirmation, sends
	 * {@link AdminCommands#LOAD_AS_FACTION} (or the docked variant when the
	 * player is aiming at a rail block).
	 */
	private void openBuildSectorSpawnDialog(CatalogPermission permission, GameClientState state, PlayerState player) {
		String desc = Lng.str("Please type in a name for your new Ship!");
		PlayerGameTextInput pp = new PlayerGameTextInput("CatalogScrollableListNew_f_load", state, 400, 240, 50, Lng.str("New Ship"), desc, permission.getUid() + "_" + System.currentTimeMillis()) {

			@Override
			public String[] getCommandPrefixes() {
				return null;
			}

			@Override
			public String handleAutoComplete(String s, TextCallback callback, String prefix) {
				return s;
			}

			@Override
			public boolean isOccluded() {
				return state.getController().getPlayerInputs().indexOf(this) != state.getController().getPlayerInputs().size() - 1;
			}

			@Override
			public void onDeactivate() {
			}

			@Override
			public void onFailedTextCheck(String msg) {
				setErrorMessage(Lng.str("SHIPNAME INVALID:") + " " + msg);
			}

			@Override
			public boolean onInput(String entry) {
				if(state.getCharacter() == null || state.getCharacter().getPhysicsDataContainer() == null || !state.getCharacter().getPhysicsDataContainer().isInitialized()) {
					return false;
				}
				boolean docked = spawnDocked && canSpawnDocked();
				state.getController().sendAdminCommand(docked ? AdminCommands.LOAD_AS_FACTION_DOCKED : AdminCommands.LOAD_AS_FACTION, permission.getUid(), entry, useOwnFaction ? player.getFactionId() : 0);
				return true;
			}
		};

		pp.setInputChecker(new InputChecker() {
			@Override
			public boolean check(String s, TextCallback cb) {
				if(EntityRequest.isShipNameValid(s)) return true;
				cb.onFailedTextCheck(Lng.str("Must only contain letters or numbers or (_-)!"));
				return false;
			}
		});

		pp.getInputPanel().onInit();

		GUICheckBoxTextPair useFact = new GUICheckBoxTextPair(state, Lng.str("Set as own Faction (needs faction block)"), 280, FontLibrary.getBlenderProMedium14(), 24) {
			@Override
			public boolean isActivated() {
				return useOwnFaction;
			}

			@Override
			public void deactivate() {
				useOwnFaction = false;
			}

			@Override
			public void activate() {
				if(player.getFactionId() > 0) {
					useOwnFaction = true;
				} else {
					state.getController().popupAlertTextMessage(Lng.str("You are not in a faction!"), 0);
					useOwnFaction = false;
				}
			}
		};
		useFact.setPos(3, 35, 0);
		((GUIDialogWindow) pp.getInputPanel().background).getMainContentPane().getContent(0).attach(useFact);

		Object spawnDockedLabel = new Object() {
			@Override
			public String toString() {
				return canSpawnDocked() ? Lng.str("Spawn docked") : Lng.str("Spawn docked (must be aiming at a rail block)");
			}
		};
		GUICheckBoxTextPair useSpawnDocked = new GUICheckBoxTextPair(state, spawnDockedLabel, 280, FontLibrary.getBlenderProMedium14(), 24) {
			@Override
			public boolean isActivated() {
				return spawnDocked;
			}

			@Override
			public void deactivate() {
				spawnDocked = false;
			}

			@Override
			public void activate() {
				if(canSpawnDocked()) {
					spawnDocked = true;
				} else {
					state.getController().popupAlertTextMessage(Lng.str("Must be aiming at a rail block!"), 0);
					spawnDocked = false;
				}
			}
		};
		useSpawnDocked.setPos(3, 65, 0);
		((GUIDialogWindow) pp.getInputPanel().background).getMainContentPane().getContent(0).attach(useSpawnDocked);

		pp.activate();
	}
}
