package com.salts_inventory_update.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.salts_inventory_update.client.automatic.AutomaticContainerLayout;
import com.salts_inventory_update.client.automatic.AutomaticContainerLayout.Cell;
import com.salts_inventory_update.client.automatic.AutomaticContainerLayout.Control;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticContainerLayoutTest {
    private List<Cell> player(int y) {
        var cells = new ArrayList<Cell>();
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) cells.add(new Cell(8 + col * 18, y + row * 18, true));
        return cells;
    }
    @Test void conventionalMachineRetainsEveryRealMachineCellAndExcludesPlayerRows() {
        var cells = player(84); cells.addAll(List.of(new Cell(56,17,false),new Cell(56,53,false),new Cell(116,35,false)));
        var l = AutomaticContainerLayout.infer(176,166,cells,List.of());
        assertNotNull(l); assertEquals(162,l.width());
        for (Cell c : cells) if (!c.player()) {
            assertTrue(c.x()-1>=l.x()); assertTrue(c.y()-1>=l.y());
            assertTrue(c.x()+17<=l.x()+l.width()); assertTrue(c.y()+17<=l.y()+l.height());
        }
        assertTrue(l.y()+l.height()<84);
    }
    @Test void ownershipWorksWithInterleavedMenuIds() {
        var cells = player(145); cells.add(new Cell(8,19,false)); cells.add(new Cell(152,109,false));
        var first=AutomaticContainerLayout.infer(176,227,cells,List.of());
        Collections.rotate(cells,17);
        assertEquals(first,AutomaticContainerLayout.infer(176,227,cells,List.of()));
    }
    @Test void upperControlsCanExtendPastTheNominalPanel() {
        var cells=player(84);cells.add(new Cell(56,17,false));
        var l=AutomaticContainerLayout.infer(176,166,cells,List.of(new Control(-20,-8,30,20)));
        assertEquals(-20,l.x());assertEquals(-8,l.y());assertTrue(l.width()>=189);
    }
    @Test void controlsInThePlayerAreaAreNotHosted() {
        var cells=player(84);cells.add(new Cell(56,17,false));
        assertEquals(AutomaticContainerLayout.infer(176,166,cells,List.of()),
            AutomaticContainerLayout.infer(176,166,cells,List.of(new Control(-200,100,400,20))));
    }
    @Test void noMachineNoPlayerOrOverlappingInventoriesAreRejected() {
        assertNull(AutomaticContainerLayout.infer(176,166,player(84),List.of()));
        assertNull(AutomaticContainerLayout.infer(176,166,List.of(new Cell(8,19,false)),List.of()));
        var cells=player(84);cells.add(new Cell(8,80,false));
        assertNull(AutomaticContainerLayout.infer(176,166,cells,List.of()));
    }
    @Test void tallStorageRetainsTheFinalRow() {
        var cells=player(220);
        for(int row=0;row<10;row++)for(int col=0;col<9;col++)cells.add(new Cell(8+18*col,19+18*row,false));
        var l=AutomaticContainerLayout.infer(176,302,cells,List.of());
        assertNotNull(l);assertTrue(l.y()+l.height()>=198);assertTrue(l.y()+l.height()<220);
    }
    @Test void fallbackKeepsNativeSidesWithoutChangingTheApiTemplatesInferredCrop() {
        var cells=player(84);cells.add(new Cell(56,17,false));
        var template=AutomaticContainerLayout.infer(176,166,cells,List.of());
        var automatic=template.withNativeSides(176);
        assertEquals(7,template.x());assertEquals(162,template.width());
        assertEquals(0,automatic.x());assertEquals(176,automatic.width());
        assertEquals(template.y(),automatic.y());assertEquals(template.height(),automatic.height());
        assertEquals(automatic,automatic.withNativeSides(176));
    }
    @Test void nativeSideExpansionDoesNotClipOutboardWidgetsOrTallSlotRows() {
        var cells=player(220);cells.add(new Cell(8,19,false));cells.add(new Cell(152,181,false));
        var original=AutomaticContainerLayout.infer(176,302,cells,List.of(new Control(-20,-8,30,20),new Control(180,20,20,18)));
        var automatic=original.withNativeSides(176);
        assertEquals(-20,automatic.x());assertEquals(220,automatic.width());
        assertEquals(-8,automatic.y());assertEquals(original.height(),automatic.height());
        assertTrue(automatic.y()+automatic.height()>=198);
        assertTrue(automatic.y()+automatic.height()<220);
    }
    @Test void automaticBodyExcludesTheLowerInventoriesDifferentlyColoredBorder() {
        var cells=player(145);
        for(int row=0;row<6;row++)cells.add(new Cell(8,19+18*row,false));
        var template=AutomaticContainerLayout.infer(176,225,cells,List.of());
        var automatic=template.automaticBody(176,cells,List.of());
        assertEquals(135,template.y()+template.height());
        assertEquals(129,automatic.y()+automatic.height());
        assertTrue(automatic.y()+automatic.height()>=109+19);
        assertEquals(176,automatic.width());
    }
    @Test void aRealControlInTheLowerBandStillRetainsItsCompleteHitArea() {
        var cells=player(84);cells.add(new Cell(56,17,false));
        var controls=List.of(new Control(50,60,20,20));
        var inferred=AutomaticContainerLayout.infer(176,166,cells,controls);
        var automatic=inferred.automaticBody(176,cells,controls);
        assertEquals(80,automatic.y()+automatic.height());
        assertTrue(automatic.y()+automatic.height()<84);
    }
}
