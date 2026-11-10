"""Retain Resourceful Lib's original and versioned native flower-action channels."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
for version in ['1.21.1','1.21.11','26.1.2','26.2','26.3']:
 path=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat/bumblezone/BumblezoneWindows.java'
 if not path.exists():continue
 s=path.read_text()
 if 'networking/v1/the_bumblezone/crystalline_flower_clicked_enchantment_button_packet' in s:
  start=s.find('                var channel=')
  if start>=0:
   end=s.index('\n                }\n',start)
   s=s[:start]+''.join('    '+line for line in s[start:end].splitlines(keepends=True))+s[end:]
   path.write_text(s)
  continue
 pattern=r'                var channel=(\w+)\.fromNamespaceAndPath\("the_bumblezone","networking/the_bumblezone/crystalline_flower_clicked_enchantment_button_packet"\);'
 m=re.search(pattern,s);assert m,path
 old_end=s.index('                });\n            }',m.end())
 start='''                // Newer Resourceful Lib wraps the same validated native
                // action under networking/v1; older releases keep the old path.
                for(String path:List.of(
                    "networking/the_bumblezone/crystalline_flower_clicked_enchantment_button_packet",
                    "networking/v1/the_bumblezone/crystalline_flower_clicked_enchantment_button_packet")) {
                var channel='''+m[1]+'.fromNamespaceAndPath("the_bumblezone",path);'
 s=s[:m.start()]+start+s[m.end():old_end]+s[old_end:].replace('                });\n            }','                });\n                }\n            }',1)
 path.write_text(s)
 print(version,'registered both native channel generations')
