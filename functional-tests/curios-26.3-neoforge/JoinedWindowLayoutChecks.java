import com.salts_inventory_update.client.AttachedWindowLayout;
import com.salts_inventory_update.compat.curios.CuriosPanelLayout;

/** Focused, game-independent invariants for the joined Character/accessory presentation. */
public final class JoinedWindowLayoutChecks {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        int checks = 0;
        for (int[] viewport : new int[][]{{320,180},{427,240},{640,360},{960,540}}) {
            for (int slots : new int[]{0,1,9,10,35,100,1000}) {
                int preferred = CuriosPanelLayout.preferredHeight(176, slots);
                for (int x : new int[]{-300,0,45,900}) for (int y : new int[]{-300,0,70,900}) {
                    var fit = AttachedWindowLayout.above(x,y,176,106,preferred,23,CuriosPanelLayout.WINDOW_GAP,viewport[0],viewport[1]);
                    require(fit.ownerX() == fit.childX() && fit.width() == 176, "Child must match owner width and X");
                    require(fit.childY() + fit.childHeight() + CuriosPanelLayout.WINDOW_GAP == fit.ownerY(), "Joined windows must retain their small gap");
                    require(fit.childY() - 23 >= 0, "Top tab escaped the upper wall");
                    require(fit.ownerY() + 106 <= viewport[1], "Character escaped the lower wall");
                    require(fit.ownerX() >= 0 && fit.ownerX()+176 <= viewport[0], "Joined group escaped horizontal walls");
                    require(fit.childHeight() <= preferred, "Attachment grew beyond its requested contents");
                    var grid = CuriosPanelLayout.fit(176,fit.childHeight(),slots);
                    require(grid.columns()*18+14+(grid.scrolling()?11:0) <=176, "Slots or scrollbar exceed panel width");
                    require(grid.visibleRows()*18+14 <= fit.childHeight(), "A complete visible row must fit");
                    require(grid.scrolling() == (grid.maxScroll()>0), "Scrollbar should exist only with off-screen rows");
                    require(grid.scrolling() || grid.visibleRows()*grid.columns()>=slots, "A fitting panel hides slots");
                    checks++;
                }
            }
        }
        var full = AttachedWindowLayout.above(10,20,176,106,600,23,CuriosPanelLayout.WINDOW_GAP,427,240);
        require(full.ownerY() == 134 && full.childY() == 23, "Overflow must push Character to the bottom and retain its top tab");
        var grid = CuriosPanelLayout.fit(176,CuriosPanelLayout.preferredHeight(176,35),35);
        require(!grid.scrolling() && grid.columns()==9 && grid.visibleRows()==4, "Ordinary sample slots should all fit at once");
        int visibilityChecks = 0;
        for (int width = 176; width <= 320; width++) for (int height : new int[]{32,86,140}) {
            for (int slots : new int[]{9,35,127}) {
                var panel = CuriosPanelLayout.fit(width,height,slots);
                int right = 6 + panel.viewportWidth(width);
                int toggleLeft = 8 + (panel.columns()-1)*18 + 12;
                require(toggleLeft + 8 <= right, "Rightmost native visibility toggle must render all eight pixels");
                require(right <= width - (panel.scrolling()?15:4), "Viewport must leave the frame and scrollbar unobstructed");
                require(toggleLeft >= 6, "Visibility toggle escaped the viewport's left edge");
                visibilityChecks++;
            }
        }
        System.out.println("Joined Curios layout invariants passed: " + checks + " placements plus overflow/full-grid checks; "
            + visibilityChecks + " visibility-toggle layouts");
    }
}
