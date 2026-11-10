"""Focused native-widget and server-outcome checks, not a complete gameplay suite."""
from pathlib import Path
import sys,json,time
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
run=ROOT/'build/remaining-ui-audit/gameplay'/('1.20.1-fabric-integrated-'+(sys.argv[1] if len(sys.argv)>1 else 'minecells04'))
out=ROOT/'output/minecells-implemented';out.mkdir(parents=True,exist_ok=True)
state={};checks=[]
def a(kind,**kw):
    global state
    result=send(run,kind,**kw);state=result['client']
    if 'capture' in kw:(out/(kw['capture']+'.json')).write_text(json.dumps(result,indent=2))
    return result
def check(name,condition,details=None):
    row={'check':name,'passed':bool(condition)}
    if details is not None:row['details']=details
    checks.append(row);(out/'focused-checks.json').write_text(json.dumps({'full_suite_run':False,'checks':checks},indent=2))
    print(('PASS ' if condition else 'FAIL ')+name,flush=True)
    if not condition:raise AssertionError(row)
def window():return next(w for w in state['allWindows'] if 'minecells' in w)
def vals():return window()['minecells']
def click(x,y):
    a('pointer',x=x,y=y,ticks=3);a('click',x=x,y=y,button=0,ticks=1);a('release',x=x,y=y,button=0,ticks=5)
def move_window(bounds,x,y):
    sx=bounds['x']+10;sy=bounds['y']+10;dx=x-bounds['x'];dy=y-bounds['y']
    a('pointer',x=sx,y=sy,ticks=2);a('click',x=sx,y=sy,button=0,ticks=1)
    a('drag',x=sx+dx,y=sy+dy,dx=dx,dy=dy,button=0,ticks=2);a('release',x=sx+dx,y=sy+dy,button=0,ticks=5)
def elements(suffix):return [e for e in vals()['elements'] if e['type'].endswith(suffix)]
def point(e):
    w=window();r=w['composition']['regions'][0];n=w['nativeBounds'];c=w['minecellsContent'];s=r['source']
    return c['x']+r['x']+e['x']+e['width']/2-n['x']-s['x'],c['y']+r['y']+e['y']+e['height']/2-n['y']-s['y']
def hit(e):click(*point(e))
def paper():hit(next(e for e in elements('$TexturedButton') if e['width']==64))
def category(i):hit(elements('$CategoryButton')[i])
def search():hit(elements('$NoShadowTextBox')[0]);a('text',text='Balanced',ticks=5)
def select_first():hit(elements('$RecipeComponent')[0]);hit(max(elements('$TexturedButton'),key=lambda e:e['y']))
def forge():hit(next(e for e in elements('$TexturedButton') if e['width']==16))
def capture(name):a('pointer',x=50,y=200,capture=name,ticks=20)
def server(name):
    a('minecells-server',ticks=25);data=json.loads((run/'minecells-server.json').read_text());(out/(name+'.json')).write_text(json.dumps(data,indent=2));return data
def inventory_nonempty(data):return [v for v in data['inventory'] if 'minecraft:air' not in v['item']]
def seed():
    for slot,item,count in [(0,'minecraft:stick',1),(1,'minecells:cell_infused_steel',6),(2,'minecraft:lapis_lazuli',4),(3,'minecraft:emerald',2)]:
        a('command',command=f'item replace entity @p inventory.{slot} with {item} {count}')
def open_fixture(block):
    a('fixture',block=block);a('command',command='gamemode survival @p');a('command',command='tp @p 8.5 65 12.5 90 0');a('command',command='kill @e[type=minecraft:item]');a('open');a('wait',ticks=30)

deadline=time.monotonic()+45
while not (run/'ready.json').exists() and time.monotonic()<deadline:time.sleep(.2)
assert (run/'ready.json').exists(),'Native client did not become ready'
open_fixture('minecells:cell_crafter')
check('Normal block opens approved 178 x 100 crafter',window()['bounds']['width']==178 and window()['bounds']['height']==100)
check('Native unlocked and locked recipe list reaches its own window',vals()['allRecipes']==79 and vals()['blockPos'].endswith('x=8, y=65, z=8}'))
check('No storage or duplicated inventory cells in the crafter',len(window()['slots'])==36 and all(s['player'] for s in window()['slots']) and window()['composition']['regions'][0]['source']['y']+70<80)
move_window(window()['bounds'],231,65)
check('Matching header drags the whole window',window()['bounds']['x']==231 and window()['bounds']['y']==65,window()['bounds'])
a('inventory',ticks=15)
# Keep only the standard inventory and the subject window for the comparison.
for name in ['CHARACTER','CRAFTING','EQUIPMENT']:
    for w in list(state.get('workspaceWindows',[])):
        if w['kind']==name and 'CLOSE' in w.get('controls',{}):click(**w['controls']['CLOSE'])
inv=next(w for w in state['workspaceWindows'] if w['kind']=='INVENTORY')
move_window(inv['bounds'],232,177)
check('Separate SIU Inventory remains available',any(w['kind']=='INVENTORY' for w in state['workspaceWindows']))
paper()
check('Parchment mode retains complete 200 x 176 artwork',vals()['isRecipeListVisible']=='true' and window()['bounds']['width']==204 and window()['bounds']['height']==204)
capture('minecells-recipes-gui2')
buttons=elements('$TexturedButton');down=sorted(buttons,key=lambda e:e['y'])[2]
hit(down)
check('Native down arrow scrolls gear recipes',int(vals()['list-scrollOffset'])>0,vals()['list-scrollOffset'])
a('scroll',x=point(elements('$RecipeComponent')[0])[0],y=point(elements('$RecipeComponent')[0])[1],amount=1,ticks=4)
check('Mouse wheel scrolls back to first row',int(vals()['list-scrollOffset'])==0)
category(1);check('Decoration category works',vals()['list-selectedCategory']=='DECORATION')
category(2);check('Other category works',vals()['list-selectedCategory']=='OTHER')
category(0);search()
check('Native keyboard search filters to Balanced Blade',vals()['list-search'].lower()=='balanced' and vals()['visibleRecipes']==1)
select_first()
check('Recipe confirmation returns to crafting with selected result',vals()['isRecipeListVisible']=='false' and 'balanced_blade' in vals()['selectedRecipe'])
check('Missing materials keep the craft button disabled',not vals()['craftEnabled'])
forge();check('Disabled craft produces no output',server('normal-missing')['drops']==[])
seed();a('wait',ticks=20)
check('Player inventory ingredients enable native crafting',vals()['craftEnabled'])
capture('minecells-ready-gui2')
paper()
inv=next(w for w in state['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(s for s in inv['inventoryCells'] if s['inventorySlot']==9)
click(cell['x'],cell['y'])
before=server('inventory-while-browser')
check('Modal browser blocks clicks into the player inventory',len(inventory_nonempty(before))==4)
a('key',key=256,ticks=5)
check('Escape returns from parchment without closing crafter',vals()['isRecipeListVisible']=='false' and state['sessions']==1)
forge();after=server('normal-crafted')
check('Original craft consumes the exact ingredients',not inventory_nonempty(after))
check('Original crafter releases one Balanced Blade into world',sum('minecells:balanced_blade' in s and 'Count:1b' in s for s in after['drops'])==1,after['drops'])
check('Craft button disables again after spending materials',not vals()['craftEnabled'])
click(**window()['minecellsControls']['FOCUS'])
check('Focus control operates',any(w.get('focused')=='true' and w['kind']=='CONTAINER' for w in state['workspaceWindows']))
if 'ELLIPSIS' in window()['minecellsControls']:
    click(**window()['minecellsControls']['ELLIPSIS'])
    check('More-controls menu opens',vals()['managementOpen'])
    a('key',key=256,ticks=4)
    check('Escape dismisses more-controls menu',not vals()['managementOpen'])
else:
    check('Fitting header exposes standard controls',set(window()['minecellsControls'])=={'FOCUS','PIN','LOCK','LINK','CLOSE'})
click(**window()['minecellsControls']['CLOSE'])
check('Close removes only the crafter',state['sessions']==0 and any(w['kind']=='INVENTORY' for w in state['workspaceWindows']))

open_fixture('minecells:unbreakable_cell_crafter')
check('Unbreakable block opens same adapter',window()['template'].endswith('MineCellsClient$Template'))
paper();a('gui-scale',scale=3,ticks=20)
check('Live GUI-scale change keeps complete parchment geometry',window()['bounds']['height']==204 and vals()['isRecipeListVisible']=='true')
search();select_first();seed();a('wait',ticks=20)
check('Unbreakable block selects recipe and sees shared materials',vals()['craftEnabled'] and 'balanced_blade' in vals()['selectedRecipe'])
capture('minecells-unbreakable-gui3')
forge();after=server('unbreakable-crafted')
check('Unbreakable block preserves native crafting and item output',not inventory_nonempty(after) and any('minecells:balanced_blade' in d for d in after['drops']))
paper();move_window(window()['bounds'],100,45)
check('Parchment follows header movement',window()['bounds']['x']==100 and window()['bounds']['y']==45)
capture('minecells-parchment-gui3')
click(**window()['minecellsControls']['CLOSE'])
check('Close from parchment disposes owning session',state['sessions']==0)
print('Focused checks complete:',len(checks),flush=True)
