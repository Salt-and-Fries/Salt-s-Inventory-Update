"""Adapt one nested upstream @Invoker literal for the named Forge QA runtime only."""
from pathlib import Path
import hashlib,io,json,sys,zipfile
path=Path(sys.argv[1]);evidence=Path(sys.argv[2]);before=hashlib.sha256(path.read_bytes()).hexdigest()
nested="META-INF/jars/bundle-api-forge-1.3.0+1.20.1.jar"
klass="com/github/theredbrain/bundleapi/mixin/client/ModelPredicateProviderRegistryInvoker.class"
def rewrite(data,changes):
 src=zipfile.ZipFile(io.BytesIO(data));out=io.BytesIO()
 with zipfile.ZipFile(out,"w") as dst:
  for info in src.infolist():dst.writestr(info,changes.get(info.filename,src.read(info.filename)))
 return out.getvalue()
outer=path.read_bytes()
with zipfile.ZipFile(io.BytesIO(outer)) as z: child=z.read(nested)
with zipfile.ZipFile(io.BytesIO(child)) as z: code=z.read(klass)
old=b"\x00\x09m_174570_";new=b"\x00\x08register"
if code.count(old)!=1:raise ValueError("Unexpected BundleAPI invoker constant")
child2=rewrite(child,{klass:code.replace(old,new)})
path.write_bytes(rewrite(outer,{nested:child2}))
evidence.write_text(json.dumps({"purpose":"named Forge QA runtime only; map embedded BundleAPI @Invoker literal using intermediateToNamed methods.csv", "jar":str(path.resolve()),"before_sha256":before,"after_sha256":hashlib.sha256(path.read_bytes()).hexdigest(),"embedded_jar":nested,"class":klass,"srg":"m_174570_","named":"register"},indent=2))
