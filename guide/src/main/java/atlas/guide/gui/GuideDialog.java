package atlas.guide.gui;

import atlas.guide.manager.GuideManager;
import atlas.guide.util.MarkdownGuiBlockRenderer;
import org.schema.game.client.view.mainmenu.MarkdownDocRenderer;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.List;

/**
 * In-game guide dialog that renders markdown documents using StarMade's native GUI system.
 *
 * <p>Layout:
 * <ul>
 *   <li><b>Left panel (~220 px)</b> — scrollable list of document title buttons</li>
 *   <li><b>Right panel (remaining width)</b> — scrollable content area rendered by
 *       {@link MarkdownGuiBlockRenderer} using StarMade's {@link MarkdownDocRenderer}</li>
 * </ul>
 *
 * <p>This fully replaces the old {@code glossarPanel} / Glossar library approach from EdenCore.
 */
public class GuideDialog extends GUIElement implements GUIActiveInterface {

    private static final int WIDTH       = 900;
    private static final int HEIGHT      = 600;
    private static final int LIST_WIDTH  = 220;
    private static final int CONTENT_W   = WIDTH - LIST_WIDTH - 20;

    private GUIMainWindow window;
    private GUIAncor      contentAnchor;
    private boolean       initialized;

    // ── constructors ──────────────────────────────────────────────────────────

    public GuideDialog(InputState state) {
        super(state);
    }

    /** Convenience — creates and activates the dialog from any context. */
    public GuideDialog() {
        this(api.common.GameClient.getClientState());
    }

    public void activate() {
        if(!initialized) onInit();
        setActive(true);
    }

    // ── GUIActiveInterface ────────────────────────────────────────────────────

    @Override
    public void onInit() {
        if(window != null) window.cleanUp();

        window = new GUIMainWindow(getState(), WIDTH, HEIGHT, "GuideDialog");
        window.onInit();
        window.orientate(ORIENTATION_HORIZONTAL_MIDDLE | ORIENTATION_VERTICAL_MIDDLE);

        window.setCloseCallback(new GUICallback() {
            @Override
            public void callback(GUIElement e, MouseEvent event) {
                if(event.pressedLeftMouse()) {
                    getState().getWorldDrawer().getGuiDrawer().getPlayerPanel().deactivateAll();
                }
            }
            @Override
            public boolean isOccluded() {
                return !getState().getController().getPlayerInputs().isEmpty();
            }
        });

        buildContent();
        initialized = true;
    }

    @Override
    public void draw() {
        if(!initialized) onInit();
        window.draw();
    }

    @Override
    public void cleanUp() {
        if(window != null) window.cleanUp();
        initialized = false;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void buildContent() {
        GUIContentPane tab = window.addTab("Guide");
        GUIPane root = tab.getContent(0);

        // Title list — left side
        GUIPane listPane = new GUIPane(getState(), root, LIST_WIDTH, HEIGHT - 60);
        listPane.onInit();
        listPane.setPos(0, 0, 0);
        root.attach(listPane);
        buildTitleList(listPane);

        // Content area — right side (scrollable)
        GUIScrollablePane scrollPane = new GUIScrollablePane(getState(), root,
                CONTENT_W, HEIGHT - 60);
        scrollPane.onInit();
        scrollPane.setPos(LIST_WIDTH + 10, 0, 0);
        root.attach(scrollPane);

        contentAnchor = new GUIAncor(getState(), CONTENT_W, HEIGHT - 60);
        contentAnchor.onInit();
        scrollPane.setContent(contentAnchor);

        // Render the first doc by default
        List<String> titles = GuideManager.getTitles();
        if(!titles.isEmpty()) renderDoc(titles.get(0));
    }

    private void buildTitleList(GUIPane parent) {
        List<String> titles = GuideManager.getTitles();
        int y = 5;
        for(final String title : titles) {
            GUITextButton btn = new GUITextButton(getState(), parent, title);
            btn.setCallback(new GUICallback() {
                @Override
                public void callback(GUIElement e, MouseEvent event) {
                    if(event.pressedLeftMouse()) renderDoc(title);
                }
                @Override
                public boolean isOccluded() { return false; }
            });
            btn.onInit();
            btn.setPos(4, y, 0);
            parent.attach(btn);
            y += 24;
        }
    }

    private void renderDoc(String title) {
        if(contentAnchor == null) return;
        contentAnchor.cleanUp();
        contentAnchor.onInit();

        String markdown = GuideManager.getRaw(title);
        List<MarkdownDocRenderer.RenderedBlock> blocks = MarkdownDocRenderer.render(markdown);
        MarkdownGuiBlockRenderer.renderBlocks(getState(), contentAnchor, blocks, CONTENT_W - 16, 8);
    }
}
