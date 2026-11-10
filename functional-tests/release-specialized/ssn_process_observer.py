"""Reuse the strict read-only SSN observation transport for process configuration."""
from ssn_filters_observer import enhance as filters


def enhance(text,mc,named=False):
    assert mc in ['1.21.1','26.1.2']
    text=filters(text,mc,named)
    text=text.replace('SsnFilter','SsnProcess').replace('ssn-filters','ssn-process')
    old='List.of("com.lothrazar.storagenetwork.block.cable.linkfilter.ContainerCableFilter",\n            "com.lothrazar.storagenetwork.block.collection.ContainerCollectionFilter")'
    assert text.count(old)==1
    text=text.replace(old,'List.of("com.lothrazar.storagenetwork.block.cable.processing.ContainerCableProcess")')
    old='if(nativeMenu.getClass().getSimpleName().equals("ContainerCableFilter"))for(String name:List.of("btnPlus","btnMinus","btnAllowIgn","btnImport"))'
    assert text.count(old)==1
    text=text.replace(old,'for(String name:List.of("btnPlus","btnMinus","btnRedstone","btnReset","btnAlways","btnInputFace","btnOutputFace","btnImport"))')
    start=text.index('    static Map<String,Object> releaseSsnProcessSettings(')
    end=text.index('    static String releaseSsnProcessStack(',start)
    code=r'''
    static Map<String,Object> releaseSsnProcessSettings(AbstractContainerMenu menu){
        Object cap=field(menu,"cap"),model=call(field(menu,"tile"),"getProcessModel");var result=new LinkedHashMap<String,Object>();
        for(String method:List.of("getFilters","getFiltersOut")){
            Object filter=call(cap,method);var ghosts=new ArrayList<String>();int size=((Number)call(filter,"getSlots")).intValue();
            for(int i=0;i<size;i++)ghosts.add(releaseSsnProcessStack((ItemStack)call(filter,"getStackInSlot",i)));
            result.put(method,ghosts);
        }
        result.put("needsRedstone",call(cap,"needsRedstone"));result.put("count",call(model,"getCount"));result.put("always",call(model,"isAlwaysActive"));
        result.put("inputFace",((Enum<?>)call(model,"getInputFace")).name());result.put("outputFace",((Enum<?>)call(model,"getOutputFace")).name());
        result.put("status",String.valueOf(call(model,"getStatus")));return result;
    }
'''
    if not named:code=code.replace('call(', 'coreCall(')
    return text[:start]+code+text[end:]
