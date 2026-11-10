"""Publish a fresh agent's complete PDF-only critique without omitting findings."""
from pathlib import Path
from collections import Counter
from xml.sax.saxutils import escape
import argparse,hashlib,json,re
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.platypus import BaseDocTemplate,PageTemplate,Frame,Paragraph,Spacer,PageBreak,KeepTogether,Table,TableStyle,Flowable
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.colors import HexColor,white
from reportlab.lib.pagesizes import A4,landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'output/create-window-independent-review'
INPUT=BASE/'agent/review.json'
PDF=ROOT/'output/pdf/siu-create-1.20.1-independent-visual-review.pdf'
SOURCE=ROOT/'output/pdf/siu-create-1.20.1-implemented-windows.pdf'
TEAL=HexColor('#23777c');INK=HexColor('#263a40');MUTED=HexColor('#61747a');PALE=HexColor('#edf3f3');LINE=HexColor('#d4dfe0')
SEVERITY={'major':'#ae4141','moderate':'#92611f','minor':'#546b79'}

def clean(v):
    if v is None:return ''
    if isinstance(v,(dict,list)):return json.dumps(v,ensure_ascii=False)
    return str(v).replace('\u2011','-').replace('\u2013','-').replace('\u2014',' - ')
def label(s):return s.replace('_',' ').strip().capitalize()
def norm(s):return re.sub('[^a-z0-9]','',str(s).lower())
def field(obj,*keys,default=''):
    for k in keys:
        if k in obj:return obj[k]
    return default

class EvidencePair(Flowable):
    def __init__(self,paths,width,max_height=235,callouts=None):
        super().__init__();self.paths=paths;self.width=width;self.height=max_height+27;self.max_height=max_height;self.callouts=callouts or []
    def draw(self):
        c=self.canv;gap=16;cw=(self.width-gap)/2
        for i,(loader,path) in enumerate(self.paths):
            x=i*(cw+gap);c.setFillColor(PALE);c.roundRect(x,0,cw,self.height,6,fill=1,stroke=0)
            c.setFillColor(TEAL);c.setFont('ReviewBold',9);c.drawString(x+11,self.height-17,loader)
            im=Image.open(path);scale=min((cw-20)/im.width,(self.max_height-8)/im.height)
            w,h=im.width*scale,im.height*scale
            ix=x+(cw-w)/2;iy=8+(self.max_height-8-h)/2
            c.drawImage(ImageReader(im),ix,iy,w,h,mask='auto')
            for finding in self.callouts:
                nx,ny,nw,nh=finding['normalized_rectangle'];rx=ix+nx*w;ry=iy+(1-ny-nh)*h
                c.setStrokeColor(HexColor('#bd4141'));c.setLineWidth(1.2);c.rect(rx,ry,nw*w,nh*h,fill=0,stroke=1)
                c.setFillColor(HexColor('#bd4141'));c.setFont('ReviewBold',7);c.drawString(rx,ry+nh*h+3,finding['id'])

class ReviewDoc(BaseDocTemplate):
    def afterFlowable(self,f):
        if getattr(f,'bookmark',None):
            self.canv.bookmarkPage(f.bookmark);self.canv.addOutlineEntry(f.outline,f.bookmark,level=0,closed=False)

def build(data):
    pdfmetrics.registerFont(TTFont('Review','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('ReviewBold','C:/Windows/Fonts/segoeuib.ttf'))
    pdfmetrics.registerFontFamily('Review',normal='Review',bold='ReviewBold',italic='Review',boldItalic='ReviewBold')
    w,h=landscape(A4);margin=38;bodyw=w-2*margin
    styles={
        'body':ParagraphStyle('body',fontName='Review',fontSize=10.2,leading=14.3,textColor=INK,spaceAfter=4),
        'small':ParagraphStyle('small',fontName='Review',fontSize=8.8,leading=12,textColor=MUTED,spaceAfter=5),
        'title':ParagraphStyle('title',fontName='ReviewBold',fontSize=24,leading=29,textColor=INK,spaceAfter=12),
        'heading':ParagraphStyle('heading',fontName='ReviewBold',fontSize=18,leading=23,textColor=INK,spaceAfter=10),
        'subheading':ParagraphStyle('subheading',fontName='ReviewBold',fontSize=12.3,leading=16.6,textColor=TEAL,spaceBefore=7,spaceAfter=6,keepWithNext=True),
        'finding':ParagraphStyle('finding',fontName='ReviewBold',fontSize=11.3,leading=15.5,textColor=INK,spaceBefore=10,spaceAfter=5),
        'cell':ParagraphStyle('cell',fontName='Review',fontSize=8.8,leading=11.8,textColor=INK),
    }
    def p(text,style='body'):return Paragraph(escape(clean(text)).replace('\n','<br/>'),styles[style])
    def rich(text,style='body'):return Paragraph(text,styles[style])
    def heading(text,bookmark=None,style='heading'):
        f=p(text,style)
        if bookmark:f.bookmark=bookmark;f.outline=clean(text)
        return f
    def structured(value):
        out=[]
        if isinstance(value,dict):
            if len(value)<=5 and all(not isinstance(v,(dict,list)) for v in value.values()):
                return [rich(' | '.join('<b>'+escape(label(k))+':</b> '+escape(clean(v)) for k,v in value.items()))]
            for k,v in value.items():
                if isinstance(v,(dict,list)):
                    out.append(p(label(k),'subheading'));out+=structured(v)
                else:out.append(rich('<b>'+escape(label(k))+':</b> '+escape(clean(v))))
        elif isinstance(value,list):
            if value and all(not isinstance(v,(dict,list)) and len(clean(v))<35 for v in value):
                return [p(', '.join(clean(v) for v in value))]
            for item in value:
                if isinstance(item,(dict,list)):out+=structured(item);out.append(Spacer(1,4))
                else:out.append(p('- '+clean(item)))
        else:out.append(p(value))
        return out
    finding_ids=[]
    def finding(f):
        identifier=clean(field(f,'id','finding_id'));assert identifier and identifier not in finding_ids,identifier;finding_ids.append(identifier)
        severity=clean(f.get('severity','minor')).lower();certainty=clean(field(f,'certainty','confidence',default='observed'))
        title=field(f,'title','summary',default=f.get('category','Finding'))
        color=SEVERITY.get(severity,'#546b79')
        head=rich('<font color="'+color+'">'+escape(identifier)+' | '+escape(severity.upper())+'</font> - '+escape(clean(title)),'finding')
        meta=p('Evidence: '+certainty,'small')
        out=[head,meta]
        used={'id','finding_id','severity','certainty','confidence','title','summary'}
        ordered=['location','observation','why_it_matters','recommendation']
        for key in ordered:
            if key in f:
                out.append(rich('<b>'+escape(label(key))+':</b> '+escape(clean(f[key])).replace('\n','<br/>')));used.add(key)
        for key,val in f.items():
            if key=='subjective_preference' and not val:continue
            if key not in used and key not in ('category','normalized_rectangle'):
                out.append(rich('<b>'+escape(label(key))+':</b> '+escape(clean(val)).replace('\n','<br/>')))
        out.append(Spacer(1,4));return [KeepTogether(out)]
    def on_page(c,doc):
        c.saveState();c.setFillColor(TEAL);c.setFont('ReviewBold',8.4);c.drawString(margin,h-26,'SIU / CREATE / INDEPENDENT VISUAL CRITIQUE')
        c.setStrokeColor(LINE);c.line(margin,29,w-margin,29);c.setFont('Review',8);c.setFillColor(MUTED)
        c.drawString(margin,17,'Fresh GPT-6.1 Sol / extra-high | PDF-only evidence | 29 October 2026');c.drawRightString(w-margin,17,str(doc.page));c.restoreState()
    PDF.parent.mkdir(parents=True,exist_ok=True)
    doc=ReviewDoc(str(PDF),pagesize=(w,h),leftMargin=margin,rightMargin=margin,topMargin=45,bottomMargin=42,title='Create windows - independent visual critique',author='Independent GPT-6.1 Sol review; compiled by Codex')
    doc.addPageTemplates([PageTemplate(id='review',frames=[Frame(margin,42,bodyw,h-87,id='body',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=on_page)])
    windows=data['windows'];global_findings=data.get('global_findings',[]);document_findings=data.get('document_findings',[])
    all_findings=global_findings+document_findings+[f for win in windows for f in win.get('findings',[])]
    counts=Counter(f.get('severity','minor') for f in all_findings)
    story=[heading('Independent visual critique',style='title'),p('Create windows for Minecraft 1.20.1','subheading'),p('A fresh GPT-6.1 Sol agent at extra-high reasoning reviewed the implementation PDF without the parent conversation history. This document carries the reviewer\'s complete findings, recommendations, and prioritized repair plan.'),p(str(len(windows))+' window states | '+str(len(all_findings))+' findings | '+str(counts.get('major',0))+' major / '+str(counts.get('moderate',0))+' moderate / '+str(counts.get('minor',0))+' minor','subheading'),p('Source: siu-create-1.20.1-implemented-windows.pdf, 16 pages. Window pictures below are original images extracted from that PDF. No implementation code, earlier mockups or test journals were supplied as review evidence.','small')]
    summary=data.get('executive_summary','')
    if isinstance(summary,dict):summary={k:v for k,v in summary.items() if k not in ('reviewed_states','reviewed_loader_images','total_findings','severity_counts','certainty_counts')}
    story+=structured(summary)
    story+=[PageBreak(),heading('How to read the criticism','reading-guide'),p('Observed findings describe visible screenshot evidence. Inferred concerns describe interpretation or design risk. Runtime-check findings are questions to verify in game; a still image alone does not establish a functional failure. Severity is the reviewer\'s prioritization, not a measured defect rate.'),p('All reviewer criticism is retained. Cross-cutting concerns and source-report limitations appear before the individual windows, so they are not mistaken for a confirmed local clipping defect. No implementation changes or gameplay tests were performed during this review.')]
    for key in ['reviewer','reviewer_metadata','scope','limitations','methodology']:
        if key in data:story.append(p(label(key),'subheading'));story+=structured(data[key])
    story+=[PageBreak(),heading('Window index','window-index')]
    rows=[[p('WINDOW / STATE','cell'),p('SOURCE PAGE','cell'),p('MAJOR','cell'),p('MODERATE','cell'),p('MINOR','cell')]]
    for i,win in enumerate(windows):
        title=field(win,'exact_title','title','name');s=Counter(f['severity'] for f in win.get('findings',[]));page=field(win,'source_pdf_page','source_page','page')
        rows.append([rich('<a href="#window-'+str(i+1)+'" color="#23777c">'+escape(clean(title))+'</a>','cell'),p(page,'cell'),p(s.get('major',0),'cell'),p(s.get('moderate',0),'cell'),p(s.get('minor',0),'cell')])
    table=Table(rows,colWidths=[bodyw-278,86,64,80,48],repeatRows=1,hAlign='LEFT');table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),PALE),('LINEBELOW',(0,0),(-1,0),0.7,LINE),('BOTTOMPADDING',(0,0),(-1,-1),2.5),('TOPPADDING',(0,0),(-1,-1),2.5),('VALIGN',(0,0),(-1,-1),'TOP'),('ROWBACKGROUNDS',(0,1),(-1,-1),[white,HexColor('#f7f9f9')])]))
    story+=[table]
    for name,items,bookmark in [('Cross-cutting visual problems',global_findings,'global-findings'),('Problems in the source report and evidence',document_findings,'document-findings')]:
        story+=[PageBreak(),heading(name,bookmark)]
        for f in items:story+=finding(f)
    gallery=json.loads((BASE/'source/gallery-map.json').read_text(encoding='utf-8'))
    row_uses={}
    for i,win in enumerate(windows):
        title=field(win,'exact_title','title','name');page=int(field(win,'source_pdf_page','source_page','page'));row=row_uses.get(page,0);row_uses[page]=row+1
        source=next((g for g in gallery if g['page']==page and norm(g['title'])==norm(title)),None)
        if source is None:source=[g for g in gallery if g['page']==page][row]
        paths=source['images']
        caption='Review '+clean(win.get('id',''))+' | '+clean(win.get('menu_id',''))+' | Source PDF page '+str(page)+', row '+str(win.get('gallery_row',row+1))
        callouts=[f for f in win.get('findings',[]) if f.get('certainty')=='observed' and 'normalized_rectangle' in f]
        if callouts:caption+=' | Red box: reviewer callout; original image pixels unchanged.'
        story+=[Spacer(1,14),KeepTogether([heading(title,'window-'+str(i+1)),p(caption+' | Fabric left, Forge right.','small'),EvidencePair([('Fabric',Path(paths['fabric'])),('Forge',Path(paths['forge']))],bodyw,275 if page==16 else 220,callouts),Spacer(1,10)])]
        if win.get('fabric_forge_differences'):story.append(rich('<b>Loader comparison:</b> '+escape(clean(win['fabric_forge_differences']))))
        if win.get('positive_observations'):story.append(rich('<b>What already reads well:</b> '+escape(' '.join(clean(x) for x in win['positive_observations']))))
        if win.get('related_findings'):story.append(rich('<b>Related shared findings:</b> '+escape(', '.join(win['related_findings']))))
        if win.get('coverage_limits'):story.append(rich('<b>Coverage limits:</b> '+escape(' '.join(clean(x) for x in win['coverage_limits']))))
        if win.get('no_further_observed_issues'):story.append(rich('<b>No further observed issues:</b> '+escape(clean(win['no_further_observed_issues']))))
        for f in win.get('findings',[]):story+=finding(f)
        if not win.get('findings') and not win.get('no_further_observed_issues'):story.append(p('The reviewer recorded no additional local finding for this state. Cross-cutting findings still apply.'))
        used={'id','gallery_row','menu_id','findings','exact_title','title','name','source_pdf_page','source_page','page','images','image_evidence','image_paths','image_evidence_paths','evidence','enlarged_pair_evidence','fabric_forge_differences','positive_observations','related_findings','coverage_limits','no_further_observed_issues'}
        for key,val in win.items():
            if key not in used:story.append(p(label(key),'subheading'));story+=structured(val)
    story+=[PageBreak(),heading('Prioritized repair plan','repair-plan')]
    plan=field(data,'prioritized_repair_plan','repair_plan',default=[])
    if isinstance(plan,list):
        for step in plan:story+=[KeepTogether(structured(step)),Spacer(1,7)]
    else:story+=structured(plan)
    checks=[]
    for key in ['focused_checks_run','checks_not_run']:
        if key in data:checks.append(p(label(key),'subheading'));checks+=structured(data[key])
    if checks:story+=[Spacer(1,7),KeepTogether(checks)]
    known={'windows','global_findings','document_findings','executive_summary','reviewer','reviewer_metadata','scope','limitations','methodology','prioritized_repair_plan','repair_plan','focused_checks_run','checks_not_run'}
    for key,val in data.items():
        if key not in known:story.append(p(label(key),'subheading'));story+=structured(val)
    story+=[p('The unabridged agent files, structured findings and PDF extraction manifest are retained under output/create-window-independent-review/agent/. Source PDF SHA-256: '+hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'small')]
    doc.build(story)
    return finding_ids

if __name__=='__main__':
    data=json.loads(INPUT.read_text(encoding='utf-8'));ids=build(data)
    (BASE/'published-finding-ids.json').write_text(json.dumps({'reviewer_model':'gpt-6.1-sol','reasoning_effort':'xhigh','fork_turns':'none','source_pdf_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'finding_ids':ids,'pdf':str(PDF)},indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'output':str(PDF),'published_findings':len(ids),'windows':len(data['windows'])}))
