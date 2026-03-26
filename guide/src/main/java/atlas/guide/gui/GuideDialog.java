package atlas.guide.gui;

import api.common.GameClient;
import atlas.guide.manager.GuideManager;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.client.view.mainmenu.MarkdownDocRenderer;
import org.schema.game.client.view.mainmenu.MarkdownGuiBlockRenderer;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITabbedContent;
import org.schema.schine.input.InputState;

import java.util.List;

/**
 * In-game guide dialog that renders markdown documents using StarMade's native GUI system.
 *
 * <p>Each registered guide document appears as a separate tab. The active tab's content
 * is rendered using StarMade's {@link MarkdownDocRenderer}.
 *
 * <p>This fully replaces the old {@code glossarPanel} / Glossar library approach from EdenCore.
 *
 * @author TheDerpGamer
 */
public class GuideDialog extends PlayerInput {

    private final GuidePanel panel;

    public GuideDialog() {
        super(GameClient.getClientState());
        (panel = new GuidePanel(getState(), this)).onInit();
    }

    @Override
    public void onDeactivate() {}

    @Override
    public void handleMouseEvent(MouseEvent mouseEvent) {}

    @Override
    public GuidePanel getInputPanel() { return panel; }

    public static class GuidePanel extends GUIInputPanel {

        private GUITabbedContent tabbedContent;

        public GuidePanel(InputState state, GUICallback guiCallback) {
            super("GuidePanel", state, 900, 600, guiCallback, "", "");
        }

        @Override
        public void onInit() {
            super.onInit();
            GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
            contentPane.setTextBoxHeightLast(500);

            if(tabbedContent != null) tabbedContent.clearTabs();
            tabbedContent = new GUITabbedContent(getState(), contentPane.getContent(0));
            tabbedContent.onInit();

            List<String> titles = GuideManager.getTitles();
            if(titles.isEmpty()) {
                GUIContentPane noDocsTab = tabbedContent.addTab("Guide");
                noDocsTab.setTextBoxHeightLast(460);
                GUIAncor textAnchor = new GUIAncor(getState(), 860, 460);
                GUITextOverlay text = new GUITextOverlay(860, 460, getState());
                text.onInit();
                text.setTextSimple("No guide documents are registered.");
                textAnchor.attach(text);
                noDocsTab.getContent(0).attach(textAnchor);
            } else {
                for(String title : titles) {
                    GUIContentPane tab = tabbedContent.addTab(title);
                    tab.setTextBoxHeightLast(460);
                    GUIAncor textAnchor = new GUIAncor(getState(), 860, 460) {
                        boolean rendered;
                        @Override
                        public void draw() {
                            super.draw();
                            if(!rendered) {
                                rendered = true;
                                renderDoc(title, this);
                            }
                        }
                    };
                    tab.getContent(0).attach(textAnchor);
                }
            }

            contentPane.getContent(0).attach(tabbedContent);
        }

        private void renderDoc(String title, GUIAncor anchor) {
            String markdown = GuideManager.getRaw(title);
            List<MarkdownDocRenderer.RenderedBlock> blocks = MarkdownDocRenderer.render(markdown);
            MarkdownGuiBlockRenderer.renderBlocks(getState(), anchor, blocks, (int) anchor.getWidth() - 16, 8);
        }
    }
}
