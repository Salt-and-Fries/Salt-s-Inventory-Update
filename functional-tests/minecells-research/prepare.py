"""Native-only Mine Cells screenshot observations; no subject/UI patches."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('forge_prepare',ROOT/'functional-tests/prominent-forge/prepare.py')
forge=importlib.util.module_from_spec(spec);spec.loader.exec_module(forge)
prep=forge.prep;original=prep.qa_source


def source(side):
    s=original(side)
    helpers=r'''
    static void minecellsElements(Object component,List<Object> out,int depth){
        if(component==null||depth>12)return;
        var row=new LinkedHashMap<String,Object>();row.put("type",component.getClass().getName());
        for(String k:List.of("x","y","width","height"))try{row.put(k,coreCall(component,k));}catch(Exception absent){}
        try{row.put("text",String.valueOf(coreCall(component,"text")));}catch(Exception absent){}
        out.add(row);try{for(Object child:(List<?>)coreCall(component,"children"))minecellsElements(child,out,depth+1);}catch(Exception leaf){}
    }
    '''
    s=s.replace('static boolean coreMode()',helpers+'\n    static boolean coreMode()',1)
    command=r'''
            case "minecells-state"->{
                var result=new LinkedHashMap<String,Object>();result.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());
                if(mc.screen!=null){
                    Object list=field(mc.screen,"recipeList");
                    for(String k:List.of("isRecipeListVisible","selectedRecipe","blockPos"))result.put(k,String.valueOf(field(mc.screen,k)));
                    var recipes=new LinkedHashMap<String,Object>();
                    for(String k:List.of("allRecipes","currentRecipes","visibleRecipes")){Object v=field(list,k);recipes.put(k,v instanceof List<?> l?l.size():String.valueOf(v));}
                    for(String k:List.of("selectedCategory","scrollOffset","search","selectedRecipe"))recipes.put(k,String.valueOf(field(list,k)));
                    result.put("recipes",recipes);var elements=new ArrayList<Object>();minecellsElements(field(field(mc.screen,"uiAdapter"),"rootComponent"),elements,0);result.put("elements",elements);
                }
                write("minecells-state.json",result);
            }
    '''
    return s.replace('case "snapshot-server"',command+'\n            case "snapshot-server"',1)


prep.qa_source=source
if __name__=='__main__':prep.main()
