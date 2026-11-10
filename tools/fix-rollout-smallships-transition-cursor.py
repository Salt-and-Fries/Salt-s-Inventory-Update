"""Preserve the shared cursor after Small Ships closes its previous page."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
 path=ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/SmallShipsActions.java';text=path.read_text()
 if 'The old page mirror was cleared by native removal.' in text:continue
 start='                DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{'
 end='                });\n            });'
 assert text.count(start)==1 and text.count(end)==1,path
 text=text.replace(start,'                try {\n'+start)
 text=text.replace(end,'''                });
                } finally {
                    if(!DesktopSessions.hasOpenSessionMatching(c.player(),menu->menu==c.menu())) {
                        // The old page mirror was cleared by native removal.
                        // Paging consumes no items. Keep the canonical cursor
                        // through Core's enclosing native-payload callback.
                        c.menu().setCarried(c.player().inventoryMenu.getCarried().copy());
                    }
                }
            });''')
 path.write_text(text);print(mc)
