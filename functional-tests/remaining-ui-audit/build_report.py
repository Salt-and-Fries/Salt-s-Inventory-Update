"""Build a visual report from the final, independently verified coverage ledger.

Do not run until the PDF skill's create marker has been emitted. Screenshots
are cropped only; common scaling within each pair preserves GUI proportions.
Review every rendered page and adjust crop rectangles in the case manifests
where the automatic bounds miss controls or artwork.
"""
import argparse
from collections import defaultdict
from io import BytesIO
import json
import math
from pathlib import Path

from PIL import Image
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.utils import ImageReader, simpleSplit
from reportlab.pdfgen import canvas
from reportlab.pdfbase.pdfmetrics import stringWidth

ROOT=Path(__file__).resolve().parents[2]
PAGE=landscape(A4)

def label(c,x,y,text,size=9,color='#202d3a'):
    c.setFillColor(colors.HexColor(color));c.setFont('Helvetica',size);c.drawString(x,y,str(text))

def fit(text,width,size=9):
    text=str(text).encode('ascii','replace').decode('ascii')
    while len(text)>3 and stringWidth(text,'Helvetica',size)>width:
        text=text[:-4]+'...'
    return text

def footer(c):
    label(c,770,18,str(c.getPageNumber()),8,'#52697a')

def crop(side):
    path=side.get('capture');rect=side.get('crop_rect')
    if not path or not rect:return None
    with Image.open(ROOT/path) as source:
        result=source.convert('RGB').crop(tuple(rect))
    return result

def image(c,img,x,y,width,height,scale):
    buf=BytesIO();img.save(buf,format='PNG');buf.seek(0)
    w,h=img.size;cw,ch=w*scale,h*scale
    c.drawImage(ImageReader(buf),x+(width-cw)/2,y+(height-ch)/2,cw,ch)

def draw_pair(c,case,y):
    left=crop(case['native']);right=crop(case['core']);boxw=373;boxh=183
    runtime='production' if '-prod-' in case['native'].get('capture','') else 'named development runtime'
    title=case['native'].get('title') or case['id'].replace('-',' ')
    title=title.removeprefix('-1.')
    label(c,36,y+225,fit(title,760,11),11)
    label(c,36,y+212,fit(case['minecraft']+' '+case['loader']+' | '+runtime+' | '+case['outcome'],760,8),8,'#52697a')
    label(c,36,y+198,'NATIVE - SIU absent',8,'#46617a');label(c,433,y+198,'CORE ONLY',8,'#46617a')
    available=[img for img in (left,right) if img is not None]
    if available:
        factor=min(min(boxw/img.width,boxh/img.height) for img in available)
        for img,x in ((left,36),(right,433)):
            if img is not None:
                image(c,img,x,y+4,boxw,boxh,factor)
            else:
                label(c,x,y+100,'Capture unavailable; see case ledger.',10)
    else:
        label(c,36,y+100,'Captures unavailable; see case ledger.',10)

def build(ledger,output):
    data=json.loads(ledger.read_text(encoding='utf-8')); summary=data['summary']
    cases=defaultdict(list)
    for row in data['cases']:cases[row['entry']].append(row)
    entries={r['entry']:r for r in data['appendix']}
    c=canvas.Canvas(str(output),pagesize=PAGE,pageCompression=1)
    c.setTitle('Remaining mod inventory UI - native and Core-only comparison')
    c.setFillColor(colors.HexColor('#142b3b'));c.rect(0,0,*PAGE,fill=1,stroke=0)
    label(c,44,510,'Remaining mod inventory UI',29,'#ffffff')
    label(c,44,474,'Native gameplay beside Core-only automatic handling',16,'#c9dfec')
    label(c,44,421,f"{summary['verified_screenshot_pairs']} recorded screenshot pairs  -  {summary['entries_with_pairs']} audit entries",13,'#ffffff')
    label(c,44,401,'647 original audit rows across 199 pages; 29 delivered rows excluded; 618 remaining accounted.',10,'#c9dfec')
    label(c,44,385,'Actual screenshots; no Compatibility or Controlify adapters. Opening and appearance only.',11,'#c9dfec')
    label(c,44,365,'Per-case runtime, provenance, setup and coverage limits are retained in the accompanying ledger.',10,'#c9dfec')
    label(c,44,329,'A visual baseline with explicit failures and gaps - not a compatibility certification.',12,'#ffffff')
    footer(c);c.showPage()
    label(c,36,560,'Scope and reading the comparisons',18)
    for i,line in enumerate([
        'One representative audited release per interface; versions and loader are shown with each pair.',
        'Automatic: Core hosted the native screen. Built-in: a Core built-in handled the menu.',
        'Native preserved: the original screen remained open. Failures and missing cases stay in the ledger.',
        'Named-development screenshots use a mapped QA runtime. Production captures use original mod binaries.',
        'Earlier named native runs used creative mode while Core runs used survival; controls may depend on mode.',
        'Images show appearance at opening. They do not establish full functionality or every possible state.',
        'The appendix accounts for other audit entries; it does not assert that non-container entries have no UI.',
        'Some upstream menus exceed the fixed viewport. Actual in-game clipping is retained and noted.'
    ]): label(c,42,520-i*34,line,10)
    footer(c);c.showPage()
    label(c,36,560,'What this report establishes',18)
    label(c,42,527,'Recorded comparison outcomes',12)
    y=503
    for outcome,count in summary['outcomes'].items():
        label(c,48,y,fit(f'{count:>3}  {outcome}',740,10),10);y-=21
    y-=15
    targets=sorted({r['minecraft']+' '+r['loader'] for r in data['cases']})
    for line in simpleSplit('Captured targets: '+', '.join(targets)+'.','Helvetica',10,750):
        label(c,42,y,line,10,'#52697a');y-=15
    for note in [
        'An outcome labels the observed presentation, not full feature correctness. A native screen can remain intact without becoming an SIU window.',
        'The selected original releases are shown in each mod section. This is not a test of every supported Minecraft version or loader.',
        'Unreached interfaces and optional-dependency variants are explicitly listed later. A source class can be a helper, an inactive feature, or another release variant.',
        'Revive multiplayer loot could not be captured because the real second client was rejected for an invalid session. No authentication workaround or simulated player was substituted.',
        'Repeated clicks left the hosted Traveler\'s Backpack settings entrypoint unchanged, and the Universal Graves list did not advance to storage. Direct grave use did open storage. Tierify\'s reforge entrypoint was also missing. See the comparisons and notes.',
        'The NeoForge Create stock-request screen was visible, but the observer reported a different server menu. This is recorded as a discrepancy for follow-up; request handling was not certified.',
    ]:
        y-=12
        for line in simpleSplit(note,'Helvetica',10,750):label(c,42,y,line,10,'#52697a');y-=15
    footer(c);c.showPage()
    ordered=sorted(cases)
    page_number=4+math.ceil(len(ordered)/32)
    starts={}
    for entry in ordered:
        starts[entry]=page_number;page_number+=math.ceil(len(cases[entry])/2)
    c.bookmarkPage('visual-index');c.addOutlineEntry('Visual comparison index','visual-index',level=0,closed=False)
    for first in range(0,len(ordered),32):
        label(c,36,565,'Visual comparison index',18)
        label(c,36,542,'Mod / audit entry - number of comparisons - first page. PDF bookmarks also open each section.',9,'#52697a')
        for n,entry in enumerate(ordered[first:first+32]):
            column,row=divmod(n,16);x=36+column*397;y=516-row*28
            title=entries.get(entry,{}).get('title',str(entry))
            if entry==559:title='Armor Statues / Statue Menus'
            label(c,x,y,fit(f'{entry}  {title}',306,10),10)
            label(c,x+308,y,f'{len(cases[entry])} / p. {starts[entry]}',9,'#52697a')
        footer(c);c.showPage()
    for entry in sorted(cases):
        rows=cases[entry];info=entries.get(entry,{});title=info.get('title',str(entry))
        if entry==559:
            title='Armor Statues / Statue Menus'
        for first in range(0,len(rows),2):
            if first==0:
                key='entry-'+str(entry);c.bookmarkPage(key);c.addOutlineEntry(title,key,level=0,closed=False)
            label(c,36,566,fit(title,760,17),17)
            versions=sorted({str(r.get('version','')) for r in info.get('observed_original_releases',[]) if r.get('version')})
            release='Audited release: '+', '.join(versions) if versions else 'Actual gameplay - native and Core-only'
            url=info.get('source_url')
            if url:release+=' | '+str(url)
            label(c,36,547,fit(release,760,9),9,'#52697a')
            if url:c.linkURL(str(url),(36,542,806,556),relative=0,thickness=0)
            draw_pair(c,rows[first],307)
            if first+1<len(rows):draw_pair(c,rows[first+1],48)
            footer(c);c.showPage()
    grouped_notes=defaultdict(list)
    for case in data['cases']:
        if case.get('setup_note'):grouped_notes[case['setup_note']].append(case.get('title') or case['id'])
    findings=[]
    for note,titles in grouped_notes.items():
        names=list(dict.fromkeys(titles));title=', '.join(names[:3])
        if len(names)>3:title+=f' (+{len(names)-3} other entries)'
        findings.append((title,note))
    findings.extend([
        ('Revive multiplayer setup','The real second client reached the local LAN host but Minecraft rejected its login as an invalid session. The downed-player loot interface remains unobserved; no simulated player or menu was substituted.'),
        ('Fixture provenance','Prominent Base lottery and trader use two original parent-provided data definitions in an identical, code-free support JAR on both sides. The outer GLOBAL initializer was excluded.'),
        ('Runtime comparison','Some named-development pairs used Creative for native and Survival for Core. Later production-runtime pairs generally matched game mode. Consult each case ledger before interpreting mode-sensitive controls.'),
    ])
    if findings:
        c.bookmarkPage('findings');c.addOutlineEntry('Findings and fixture notes','findings',level=0,closed=False)
        y=565
        for title,note in findings:
            lines=simpleSplit(str(note).encode('ascii','replace').decode('ascii'),'Helvetica',10,750)
            needed=25+14*len(lines)
            if y-needed<50:
                footer(c);c.showPage();y=565
            label(c,36,y,fit(title,750,13),13);y-=20
            for line in lines:label(c,42,y,line,10,'#52697a');y-=14
            y-=16
        footer(c);c.showPage()
    unresolved=[];other_releases=[]
    for row in data['appendix']:
        names=row.get('unobserved_screen_candidates_in_staged_releases',[]) if row.get('observed_original_releases') else row.get('unresolved_class_candidates',[])
        for name in names:
            unresolved.append((row['entry'],row.get('title',''),name))
        for name in row.get('other_retained_release_screen_candidates',[]):
            other_releases.append((row['entry'],row.get('title',''),name))
    if unresolved:
        c.bookmarkPage('unresolved');c.addOutlineEntry('Unresolved interface candidates','unresolved',level=0,closed=False)
        for start in range(0,len(unresolved),24):
            label(c,36,565,'Unresolved interface candidates',17)
            label(c,36,544,'Class candidates need source or runtime review; a class alone does not prove a distinct reachable UI.',9,'#52697a')
            for n,(entry,title,name) in enumerate(unresolved[start:start+24]):
                y=523-n*20
                label(c,36,y,fit(f'{entry} {title}',210,8),8)
                label(c,260,y,fit(name,535,8),8,'#52697a')
            footer(c);c.showPage()
    workflows=[]
    for row in data['appendix']:
        for item in row.get('unobserved_inventory_workflows',[]):
            workflows.append((row['entry'],row.get('title',''),item.get('description',item.get('id',''))))
    if workflows:
        c.bookmarkPage('unobserved-workflows');c.addOutlineEntry('Unobserved inventory workflows','unobserved-workflows',level=0,closed=False)
        for start in range(0,len(workflows),20):
            label(c,36,565,'Unobserved inventory workflows',17)
            label(c,36,544,'Source-supported workflows without a matched capture remain unverified.',9,'#52697a')
            for n,(entry,title,description) in enumerate(workflows[start:start+20]):
                y=523-n*23;label(c,36,y,fit(f'{entry} {title}',210,8),8);label(c,260,y,fit(description,535,8),8,'#52697a')
            footer(c);c.showPage()
    if other_releases:
        c.bookmarkPage('other-releases');c.addOutlineEntry('Other retained release candidates','other-releases',level=0,closed=False)
        for start in range(0,len(other_releases),24):
            label(c,36,565,'Other retained release candidates',17)
            label(c,36,544,'These classes belong to other retained releases outside the selected representative-release comparison.',9,'#52697a')
            for n,(entry,title,name) in enumerate(other_releases[start:start+24]):
                y=523-n*20;label(c,36,y,fit(f'{entry} {title}',210,8),8);label(c,260,y,fit(name,535,8),8,'#52697a')
            footer(c);c.showPage()
    dispositions=[]
    for row in data['appendix']:
        review=row.get('source_review') or {}
        for item in review.get('class_dispositions',[]):
            if item.get('note'): dispositions.append((row['entry'],row.get('title',''),item['note']))
        if not review.get('class_dispositions') and review.get('appendix_reason'):
            dispositions.append((row['entry'],row.get('title',''),review['appendix_reason']))
    if dispositions:
        c.bookmarkPage('source-dispositions');c.addOutlineEntry('Source-reviewed dispositions','source-dispositions',level=0,closed=False)
        label(c,36,565,'Source-reviewed dispositions',17);y=535
        for entry,title,note in dispositions:
            lines=simpleSplit(str(note).encode('ascii','replace').decode('ascii'),'Helvetica',9,750)
            needed=23+13*len(lines)
            if y-needed<50:
                footer(c);c.showPage();label(c,36,565,'Source-reviewed dispositions',17);y=535
            label(c,36,y,fit(f'{entry} {title}',750,10),10);y-=16
            for line in lines:label(c,42,y,line,9,'#52697a');y-=13
            y-=9
        footer(c);c.showPage()
    c.bookmarkPage('appendix');c.addOutlineEntry('Audit accounting and scope','appendix',level=0,closed=False)
    appendix=data['appendix'];per_page=25
    for start in range(0,len(appendix),per_page):
        label(c,36,565,'Audit accounting and scope',17)
        label(c,36,545,'Entries include shared libraries and non-container interfaces; pending entries are not completed.',9,'#52697a')
        for n,row in enumerate(appendix[start:start+per_page]):
            y=525-n*19; title=str(row.get('title',''));status=str(row.get('status',''));kind=str(row.get('audit_kind',''))
            label(c,36,y,fit(f"{row['entry']:>3}  {title}",325,8),8);label(c,375,y,fit(kind,125,8),8,'#52697a');label(c,510,y,fit(status,290,8),8,'#52697a')
        footer(c);c.showPage()
    c.save()

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('ledger',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();build(a.ledger,a.output)
