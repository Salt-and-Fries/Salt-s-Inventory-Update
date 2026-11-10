"""Native painting selector search/scroll/selection and exact close-time item update."""
from pathlib import Path
import hashlib,json,sys
import nbtlib
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack,typed


def without_variant(text):
    value=nbtlib.parse_nbt(text);assert isinstance(value,nbtlib.Compound)
    variant=None
    if 'Count'in value:
        tag=value.get('tag',{});entity=tag.get('EntityTag',{})
        if 'variant'in entity:variant=str(entity.pop('variant'))
        if 'EntityTag'in tag and not entity:del tag['EntityTag']
    else:
        components=value.get('components',{});entity=components.get('minecraft:entity_data',{})
        if 'variant'in entity:variant=str(entity.pop('variant'))
        if 'id'in entity:
            assert str(entity.pop('id'))=='minecraft:painting','Unexpected native entity identity'
        if 'minecraft:entity_data'in components and not entity:del components['minecraft:entity_data']
    return typed(value),variant


def expected_search(all_paintings,query):
    return [v['id']for v in all_paintings if any(query.lower()in v[k].lower()for k in ['title','author','dimensions'])]


def inventory(state):return tuple(stack(v['item'])for v in state['inventory'])
def empty_cursors(state):return all(not nbtlib.parse_nbt(v['item'])for v in state['cursors'])


class Gallery:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.n=0;self.checks=[]
        self.command='server-command'if loader=='neoforge'else'command'
        self.out=self.profile/'gallery-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,text):self.act(self.command,command=text,ticks=6)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=['gallery-painting-selector'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Native held-painting selector search, no-match/clear, scroll, moved selection, protected source, close-time variant data, reopening and source invalidation. Not every painting placement or localization.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label,ledger=False):
        self.act('regression-gallery-ledger'if ledger else'regression-gallery-state',ticks=5)
        data=json.loads((self.profile/'release-gallery-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(data,indent=2)+'\n');return data
    def input(self,x,y,action='tap',**kw):self.act('regression-gallery-input',source_x=x,source_y=y,action=action,ticks=12,**kw)
    def enter(self):self.act('regression-use-held-item',shift=True,ticks=20)
    def search(self,state,text):
        point=state['client']['searchPoint'];self.input(point['x'],point['y']);self.act('key',key=269,ticks=1)
        for _ in range(50):self.act('key',key=259,ticks=0)
        if text:self.act('text',text=text,ticks=8)
        result=self.state('search-'+str(len(self.checks)))
        self.check('native-search-exact-results-'+str(len(self.checks)),result['client']['search']==text and result['client']['searchFocused']and [v['id']for v in result['client']['paintings']]==expected_search(result['client']['allPaintings'],text)and result['client']['startIndex']==0)
        return result
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:air',ticks=4);self.cmd('gamemode survival @p');self.cmd('clear @p');self.act('regression-reset-hand',ticks=3)
        data='{release_gallery_marker:61,typed:[B;4b,8b]}'
        suffix=data if self.mc=='1.20.1'else'[minecraft:custom_data='+data+']'
        self.cmd('item replace entity @p container.0 with minecraft:painting'+suffix+' 3')
        self.cmd('item replace entity @p container.9 with minecraft:diamond'+suffix+' 7')
        original=self.state('original',True);self.enter();initial=self.state('initial');sid=initial['client']['session']
        self.check('ordinary-held-painting-opens-original-zero-slot-menu',initial['menus']==1 and initial['client']['menu']=='com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu'and initial['client']['screen']=='com.teamabnormals.gallery.client.gui.screens.inventory.PaintingSelectorScreen'and initial['client']['slots']==0)
        all_paintings=initial['client']['allPaintings'];self.check('native-painting-catalog-populated-with-scrollable-choices',len(all_paintings)>20 and initial['client']['paintings']==all_paintings and initial['client']['startIndex']==0)
        self.act('regression-player-click',slot=0,ticks=12);protected=self.state('protected-source')
        self.check('open-selector-protects-complete-held-source-stack',inventory(protected)==inventory(original)and empty_cursors(protected))
        if protected['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=4)
        self.act('regression-move',session=sid,dx=16,dy=12,ticks=4);moved=self.state('moved')
        self.check('selector-moves-before-native-search-and-selection',moved['client']['bounds']!=protected['client']['bounds'])
        none=self.search(moved,'no-siu-painting-match-917');self.check('native-no-match-is-empty-without-item-mutation',not none['client']['paintings']and inventory(none)==inventory(original)and empty_cursors(none))
        cleared=self.search(none,'');self.input(160,50,'scroll',amount=-1);scrolled=self.state('scrolled')
        self.check('native-wheel-scrolls-exactly-one-row',scrolled['client']['startIndex']==4 and inventory(scrolled)==inventory(original)and empty_cursors(scrolled))
        self.input(96,30);selected=self.state('scrolled-choice')
        wanted=scrolled['client']['paintings'][4]['id']
        self.check('moved-scrolled-hit-selects-exact-server-and-client-variant',selected['selected']==wanted and selected['client']['selected']==wanted)
        kebab=next(v for v in all_paintings if v['id']=='minecraft:kebab');filtered=self.search(selected,kebab['title'])
        self.check('title-search-includes-native-kebab',any(v['id']=='minecraft:kebab'for v in filtered['client']['paintings']))
        position=next(i for i,v in enumerate(filtered['client']['paintings'])if v['id']=='minecraft:kebab');assert position<20
        self.input(96+(position%4)*16,30+(position//4)*18);chosen=self.state('chosen')
        self.check('actual-native-selection-reaches-authoritative-menu',chosen['selected']=='minecraft:kebab'and chosen['client']['selected']=='minecraft:kebab'and inventory(chosen)==inventory(original)and empty_cursors(chosen))
        self.act('regression-control',session=sid,control='CLOSE',ticks=16);closed=self.state('closed',True)
        expected=list(inventory(original));expected[0]=stack(closed['inventory'][0]['item'])
        self.check('close-applies-only-native-variant-to-original-full-stack',closed['menus']==0 and without_variant(closed['inventory'][0]['item'])==(without_variant(original['inventory'][0]['item'])[0],'minecraft:kebab')and inventory(closed)==tuple(expected)and empty_cursors(closed))
        self.enter();reopened=self.state('reopened')
        self.check('ordinary-reopening-loads-persisted-native-variant',reopened['selected']=='minecraft:kebab'and reopened['client']['selected']=='minecraft:kebab'and inventory(reopened)==inventory(closed)and empty_cursors(reopened))
        self.cmd('item replace entity @p container.0 with minecraft:air');self.act('wait',ticks=16);invalid=self.state('source-invalid',True)
        self.check('external-source-removal-closes-selector-without-recreating-item',invalid['menus']==0 and not nbtlib.parse_nbt(invalid['inventory'][0]['item'])and inventory(invalid)[1:]==inventory(closed)[1:]and empty_cursors(invalid))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),gallery_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Gallery(profile,mc,loader).run()
