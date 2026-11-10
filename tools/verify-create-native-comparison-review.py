"""Check source integrity and complete publication of the fresh visual review."""
from pathlib import Path
from collections import Counter
import hashlib, json, re
import pdfplumber
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'output/create-native-comparison-review'
PDF=ROOT/'output/pdf/siu-create-original-versus-window-review.pdf'

def normalized(value):
    return re.sub(r'[^a-z0-9]', '', str(value).lower())

def leaves(value, path=()):
    if isinstance(value,dict):
        for key,item in value.items():yield from leaves(item,path+(key,))
    elif isinstance(value,list):
        for index,item in enumerate(value):yield from leaves(item,path+(str(index),))
    elif isinstance(value,str):yield path,value

def main():
    manifest=json.loads((BASE/'comparison-manifest.json').read_text(encoding='utf-8'))
    review=json.loads((BASE/'agent/review.json').read_text(encoding='utf-8'))
    publication=json.loads((BASE/'published-analysis.json').read_text(encoding='utf-8'))
    expected=[pair['id'] for pair in manifest['pairs']]
    actual=[window.get('pair_id',window.get('id')) for window in review['windows']]
    assert actual==expected,(actual,expected)
    assert len(actual)==25 and len(set(actual))==25
    verified_screenshots=0
    for pair in manifest['pairs']:
        for side in ['original','siu']:
            source=pair[side]
            assert hashlib.sha256(Path(source['full_image']).read_bytes()).hexdigest()==source['full_image_sha256']
            evidence=json.loads(Path(source['evidence']).read_text(encoding='utf-8'))['client']
            assert evidence['siu_loaded']==(side=='siu')
            if side=='original':assert evidence['screen']==pair['native_screen']
            verified_screenshots+=1
    extracted=[];page_issues=[]
    with pdfplumber.open(PDF) as document:
        for index,page in enumerate(document.pages,1):
            body=page.crop((36,42,page.width-36,page.height-40))
            extracted.append(body.extract_text(layout=False) or '')
            for char in body.chars:
                if char.get('text','').strip() and (char['x0']<37 or char['x1']>page.width-37 or char['top']<43 or char['bottom']>page.height-41):
                    page_issues.append({'page':index,'character':char['text'],'bounds':[char['x0'],char['top'],char['x1'],char['bottom']]})
    corpus=normalized('\n'.join(extracted));missing=[];checked=0
    for path,value in leaves(review):
        # Evidence filenames and normalized annotation rectangles are retained
        # in JSON; the PDF publishes the actual pictures and all prose analysis.
        if len(value)<45 or re.search(r'[A-Z]:[\\/]|\.(png|json|md)$',value,re.I):continue
        if any(key in {'source_image_paths','images','source_images','image_paths','evidence_paths','menu_id','native_screen'} for key in path):continue
        checked+=1
        if normalized(value) not in corpus:missing.append({'path':'.'.join(path),'text':value})
    all_findings=review.get('cross_cutting_findings',review.get('global_findings',[]))+review.get('window_findings',[finding for window in review['windows'] for finding in window.get('findings',[])])
    finding_ids=[finding.get('id',finding.get('finding_id')) for finding in all_findings]
    assert len(finding_ids)==len(set(finding_ids))
    for identifier in finding_ids:
        if normalized(identifier) not in corpus:missing.append({'id':identifier})
    assert set(publication['finding_ids'])==set(finding_ids)
    reader=PdfReader(PDF);image_objects=set();link_count=0
    for page in reader.pages:
        for ref in page['/Resources'].get('/XObject',{}).values():
            if ref.get_object().get('/Subtype')=='/Image':image_objects.add((ref.idnum,ref.generation))
        for ref in page.get('/Annots',[]):
            if ref.get_object().get('/Subtype')=='/Link':link_count+=1
    result={'pdf':str(PDF),'page_count':len(reader.pages),'matched_comparisons':len(actual),'full_screenshots_verified':verified_screenshots,'unique_comparison_images':len(image_objects),'navigation_links':link_count,'review_findings':len(finding_ids),'severity_counts':dict(Counter(f.get('severity') for f in all_findings)),'narrative_fields_checked':checked,'missing_narrative_fields':missing,'page_body_bounds_issues':page_issues,'complete':not missing and not page_issues and len(image_objects)==25 and link_count>=25}
    (BASE/'publication-verification.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps({key:value for key,value in result.items() if key not in ['missing_narrative_fields','page_body_bounds_issues']},ensure_ascii=False))
    if missing:print(json.dumps({'missing':missing},ensure_ascii=False))
    if page_issues:print(json.dumps({'bounds_issues':page_issues[:20]},ensure_ascii=False))
    assert result['complete'],'Publication validation failed; see publication-verification.json'

if __name__=='__main__':main()
