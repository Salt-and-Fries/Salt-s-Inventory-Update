"""Publish the fresh agent's native-vs-SIU comparison, preserving all analysis."""
from pathlib import Path
from collections import Counter
from xml.sax.saxutils import escape
import json,re,hashlib
from PIL import Image
from reportlab.platypus import BaseDocTemplate,PageTemplate,Frame,Paragraph,Spacer,PageBreak,KeepTogether,Table,TableStyle,Flowable
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import landscape,A4
from reportlab.lib.colors import HexColor,white
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'output/create-native-comparison-review'
PDF=ROOT/'output/pdf/siu-create-original-versus-window-review.pdf'
TEAL=HexColor('#23797c');INK=HexColor('#243b40');MUTED=HexColor('#64767c');PALE=HexColor('#edf3f4')
def text(v):
    if v is None:return ''
    if isinstance(v,list) and all(not isinstance(item,(dict,list)) for item in v):return ', '.join(text(item) for item in v)
    if isinstance(v,(list,dict)):return json.dumps(v,ensure_ascii=False)
    return str(v).replace('\u2011','-').replace('\u2013','-').replace('\u2014',' - ')
def get(o,*keys,default=''):
    for k in keys:
        if k in o:return o[k]
    return default
def label(s):return s.replace('_',' ').strip().capitalize()
def norm(s):return re.sub('[^a-z0-9]','',str(s).lower())

class Comparison(Flowable):
    def __init__(self,path,width,max_height=344):
        super().__init__();self.path=path;im=Image.open(path);self.scale=min(width/im.width,max_height/im.height);self.width=im.width*self.scale;self.height=im.height*self.scale;self.hAlign='CENTER'
    def draw(self):self.canv.drawImage(str(self.path),0,0,self.width,self.height,mask='auto')

class ReviewDoc(BaseDocTemplate):
    def afterFlowable(self,f):
        if getattr(f,'bookmark',None):self.canv.bookmarkPage(f.bookmark);self.canv.addOutlineEntry(f.outline,f.bookmark,level=0,closed=False)

def make(review,manifest):
    pdfmetrics.registerFont(TTFont('Compare','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('CompareBold','C:/Windows/Fonts/segoeuib.ttf'))
    pdfmetrics.registerFontFamily('Compare',normal='Compare',bold='CompareBold',italic='Compare',boldItalic='CompareBold')
    width,height=landscape(A4);m=38;body=width-2*m
    styles={
        'body':ParagraphStyle('body',fontName='Compare',fontSize=10.1,leading=14,textColor=INK,spaceAfter=4),
        'small':ParagraphStyle('small',fontName='Compare',fontSize=8.6,leading=11.6,textColor=MUTED,spaceAfter=6),
        'title':ParagraphStyle('title',fontName='CompareBold',fontSize=24,leading=29,textColor=INK,spaceAfter=11),
        'heading':ParagraphStyle('heading',fontName='CompareBold',fontSize=18,leading=23,textColor=INK,spaceAfter=9,keepWithNext=True),
        'sub':ParagraphStyle('sub',fontName='CompareBold',fontSize=12,leading=16,textColor=TEAL,spaceBefore=6,spaceAfter=5,keepWithNext=True),
        'finding':ParagraphStyle('finding',fontName='CompareBold',fontSize=11.2,leading=15.4,textColor=INK,spaceBefore=10,spaceAfter=4),
        'cell':ParagraphStyle('cell',fontName='Compare',fontSize=8.7,leading=11.5,textColor=INK),
    }
    def p(s,style='body'):return Paragraph(escape(text(s)).replace('\n','<br/>'),styles[style])
    def rich(s,style='body'):return Paragraph(s,styles[style])
    def head(s,key=None,style='heading'):
        f=p(s,style)
        if key:f.bookmark=key;f.outline=text(s)
        return f
    def content(value):
        result=[]
        if isinstance(value,dict):
            if len(value)<=4 and all(not isinstance(v,(dict,list)) and len(text(v))<110 for v in value.values()):return [rich(' | '.join('<b>'+escape(label(k))+':</b> '+escape(text(v)) for k,v in value.items()))]
            for k,v in value.items():
                if isinstance(v,(dict,list)):result.append(p(label(k),'sub'));result+=content(v)
                else:result.append(rich('<b>'+escape(label(k))+':</b> '+escape(text(v))))
        elif isinstance(value,list):
            if value and all(not isinstance(v,(dict,list)) and len(text(v))<36 for v in value):return [p(', '.join(text(v) for v in value))]
            for val in value:
                if isinstance(val,(dict,list)):result+=content(val);result.append(Spacer(1,5))
                else:result.append(p('- '+text(val)))
        else:result.append(p(value))
        return result
    ids=[]
    def finding(f):
        identifier=text(get(f,'id','finding_id'));assert identifier and identifier not in ids,identifier;ids.append(identifier)
        severity=text(f.get('severity','minor')).lower();color={'major':'#b4413f','moderate':'#956321','minor':'#586e7d'}.get(severity,'#586e7d')
        title=get(f,'title','summary','criterion','category',default='Comparative finding')
        heading=rich('<font color="'+color+'">'+escape(identifier)+' | '+escape(severity.upper())+'</font> - '+escape(text(title)),'finding')
        heading.bookmark='finding-'+identifier;heading.outline=identifier+' - '+text(title)
        parts=[heading]
        attribution=get(f,'attribution',default='not specified');certainty=get(f,'certainty','confidence',default='not specified')
        parts.append(p('Attribution: '+text(attribution)+' | Evidence: '+text(certainty),'small'))
        handled={'id','finding_id','title','summary','severity','attribution','certainty','confidence'}
        for key,title2 in [('criterion','Criterion'),('category','Category'),('location','Location'),('observation_original','Original UI'),('original_observation','Original UI'),('observation_siu','SIU result'),('siu_observation','SIU result'),('observation','Observation'),('why_it_matters','Why it matters'),('recommendation','Recommendation')]:
            if key in f:parts.append(rich('<b>'+title2+':</b> '+escape(text(f[key])).replace('\n','<br/>')));handled.add(key)
        for key,val in f.items():
            if key not in handled and key!='normalized_rectangle_siu':parts.append(rich('<b>'+escape(label(key))+':</b> '+escape(text(val))))
        return [KeepTogether(parts)]
    def page(c,doc):
        c.saveState();c.setFillColor(TEAL);c.setFont('CompareBold',8.4);c.drawString(m,height-25,'SIU / CREATE / ORIGINAL-VERSUS-WINDOW REVIEW')
        c.setStrokeColor(HexColor('#d5e1e2'));c.line(m,29,width-m,29);c.setFillColor(MUTED);c.setFont('Compare',8)
        c.drawString(m,17,'Original is the baseline | SIU is the review subject | Fresh GPT-6.1 Sol, extra-high');c.drawRightString(width-m,17,str(doc.page));c.restoreState()
    PDF.parent.mkdir(parents=True,exist_ok=True)
    doc=ReviewDoc(str(PDF),pagesize=(width,height),leftMargin=m,rightMargin=m,topMargin=45,bottomMargin=42,title='Create original UI versus SIU windows - independent comparative review',author='Fresh independent GPT-6.1 Sol review; compiled by Codex')
    doc.addPageTemplates([PageTemplate(id='comparison',frames=[Frame(m,42,body,height-87,id='body',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=page)])
    windows=review['windows'];global_findings=get(review,'cross_cutting_findings','global_findings',default=[])
    local_findings=review.get('window_findings',[f for w in windows for f in w.get('findings',[])])
    findings=global_findings+local_findings
    local_by_id={get(f,'id','finding_id'):f for f in local_findings}
    story=[head('Original UI versus SIU windows',style='title'),p('Independent review of preservation, completeness and layout fidelity','sub'),p('The original Create-family UI is the visual baseline. The SIU-windowed result is the subject being judged. This is a fresh comparison by GPT-6.1 Sol at extra-high reasoning, without inherited conversation history, previous reviews or implementation explanations.'),p('The three user rules','sub')]
    story+=[p(str(i+1)+'. '+v) for i,v in enumerate(manifest['rubric'])]
    story+=[p(str(len(windows))+' matched states across 20 native menu classes | Minecraft 1.20.1 Fabric | Same original releases and GUI scale 2','sub'),p('Left: ORIGINAL - SIU absent. Right: SIU WINDOW. The boards retain a common source-pixel scale. Both complete screenshots and crop coordinates are retained, so a detail crop cannot by itself establish clipping. The player-inventory separation and SIU management frame are expected platform changes.','small')]
    story+=content(review.get('executive_summary',''))
    story+=[PageBreak(),head('Scope, attribution and evidence','scope')]
    for key in ['reviewer_metadata','scope_and_limitations','scope','limitations']:
        if key in review:story.append(p(label(key),'sub'));story+=content(review[key])
    story+=[p('Evidence preparation','sub'),p(manifest['scope']),p(manifest['expected_platform_changes']),p(manifest['pairs'][0]['state_note']),p('Source equality: the original and SIU captures use identical upstream release SHA-256 sets. The original client has no SIU Core or Compatibility artifact. Fresh native screenshots were taken for this review; the existing implemented SIU screenshots are matched by exact original screen class and named state. No UI implementation changes were made for this review.')]
    for v in manifest['comparison_rules']:story.append(p('- '+v))
    pairs={p['id']:p for p in manifest['pairs']}
    def criterion(w,name):
        values=get(w,'criteria','criterion_verdicts','rubric_verdicts',default={})
        if not isinstance(values,dict):values={}
        for k,v in {**w,**values}.items():
            if name in str(k).lower():return text(get(v,'verdict','rating','status',default='uncertain')) if isinstance(v,dict) else text(v)
        return 'uncertain'
    story+=[PageBreak(),head('Comparison index','comparison-index')]
    rows=[[p('SIU WINDOW / STATE','cell'),p('SPRITES','cell'),p('BORDERS','cell'),p('LAYOUT','cell')]]
    for i,w in enumerate(windows):
        pairid=get(w,'pair_id','id');pair=pairs[pairid];name=get(w,'title','exact_title',default=pair['title'])
        rows.append([rich('<a href="#pair-'+str(i+1)+'" color="#23797c">'+escape(text(name))+'</a>','cell'),p(criterion(w,'sprite'),'cell'),p(criterion(w,'border'),'cell'),p(criterion(w,'layout'),'cell')])
    table=Table(rows,colWidths=[body-252,84,84,84],repeatRows=1,hAlign='LEFT');table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),PALE),('ROWBACKGROUNDS',(0,1),(-1,-1),[white,HexColor('#f7f9f9')]),('TOPPADDING',(0,0),(-1,-1),2.5),('BOTTOMPADDING',(0,0),(-1,-1),2.5),('VALIGN',(0,0),(-1,-1),'TOP')]))
    story+=[table,p('Ratings are the independent reviewer\'s visual judgments for the pictured states, not functional test results. An inherited native detail is not automatically a SIU regression.','small')]
    if global_findings:
        story+=[PageBreak(),head('Cross-cutting comparative findings','shared-findings')]
        for f in global_findings:story+=finding(f)
    for i,w in enumerate(windows):
        pairid=get(w,'pair_id','id');pair=pairs[pairid];name=get(w,'title','exact_title',default=pair['title'])
        caption=pairid+' | Original left / SIU right | Same source pixel scale; GUI 2 | Both full screenshots retained.'
        story+=[PageBreak(),KeepTogether([head(name,'pair-'+str(i+1)),p(caption,'small'),Comparison(Path(pair['comparison_image']),body),Spacer(1,8)])]
        for key,val in w.items():
            if key in ('id','pair_id','title','exact_title','findings','source_image_paths','images','original_image','siu_image','source_images','menu_id','native_screen','state_note','finding_ids'):continue
            if key in ('evidence','evidence_paths','image_paths'):continue
            if not val:continue
            if key=='comparison_verdict':
                story+=[p('Comparison verdict','sub')]+content(val)+[PageBreak(),head(text(name)+' - detailed analysis')]
                continue
            if key in ('sprite_integrity','border_integration','layout_fidelity') and isinstance(val,dict):
                story.append(p(label(key)+' - '+text(val.get('verdict','uncertain')).upper(),'sub'))
                for field,value in val.items():
                    if field!='verdict':story+=content(value)
                continue
            section=[p(label(key),'sub')]+content(val)
            if key in ('preserved_well','inherited_original_details','intentional_platform_adaptations','evidence_qualifications'):story.append(KeepTogether(section))
            else:story+=section
        references=w.get('finding_ids',[])
        if references:
            story.append(rich('<b>Related findings:</b> '+', '.join('<a href="#finding-'+escape(identifier)+'" color="#23797c">'+escape(identifier)+'</a>' for identifier in references)))
        for identifier in references:
            if identifier in local_by_id and identifier not in ids:story+=finding(local_by_id[identifier])
        for f in w.get('findings',[]):
            if get(f,'id','finding_id') not in ids:story+=finding(f)
    story+=[PageBreak(),head('Prioritized repairs','repairs')]
    plan=get(review,'prioritized_repairs','prioritized_repair_plan','repair_plan',default=[])
    if isinstance(plan,list):
        for step in plan:story+=[KeepTogether(content(step)),Spacer(1,7)]
    else:story+=content(plan)
    handled={'reviewer_metadata','scope_and_limitations','scope','limitations','executive_summary','windows','window_findings','cross_cutting_findings','global_findings','prioritized_repairs','prioritized_repair_plan','repair_plan'}
    for k,v in review.items():
        if k not in handled:
            story+=[PageBreak(),head(label(k))]
            if isinstance(v,list):
                for item in v:story+=[KeepTogether(content(item)),Spacer(1,7)]
            else:story+=content(v)
    story+=[p('Unabridged review, full screenshots, crop coordinates, exact release hashes and comparison boards: output/create-native-comparison-review/. No previous review findings were imported into this assessment.','small')]
    doc.build(story)
    result={'pdf':str(PDF),'reviewer_model':'gpt-6.1-sol','reasoning_effort':'xhigh','fork_turns':'none','comparison_manifest_sha256':hashlib.sha256((BASE/'comparison-manifest.json').read_bytes()).hexdigest(),'window_ids':[get(w,'pair_id','id') for w in windows],'finding_ids':ids,'severity_counts':dict(Counter(f.get('severity','minor') for f in findings))}
    (BASE/'published-analysis.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    return result

if __name__=='__main__':
    review=json.loads((BASE/'agent/review.json').read_text(encoding='utf-8'));manifest=json.loads((BASE/'comparison-manifest.json').read_text(encoding='utf-8'));r=make(review,manifest);print(json.dumps({'pdf':r['pdf'],'pairs':len(r['window_ids']),'findings':len(r['finding_ids'])}))
