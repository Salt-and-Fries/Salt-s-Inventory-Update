"""Read the original MI energy component's synchronized data; never write it."""
import re


def enhance(text,mc,named=False):
    assert mc in {'1.20.1','1.21.1'}
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-mi-energy-state")){
            var values=new ArrayList<Object>();Object desktop=h("screen");
            if(desktop==null)throw new IllegalStateException("No desktop for native MI status");
            for(Object window:(List<?>)field(desktop,"windows")){
                Object session=field(window,"session");if(session==null||field(window,"apiState")==null)continue;
                Object screen=field(field(window,"apiState"),"screen");
                var menu=(AbstractContainerMenu)call(screen,"getMenu");
                if(!menu.getClass().getName().startsWith("aztech.modern_industrialization."))continue;
                var energy=new ArrayList<Object>();
                for(Object component:(Iterable<?>)field(menu,"components")){
                    if(!component.getClass().getSimpleName().equals("EnergyBarClient"))continue;
                    Object data=component;
                    if(component.getClass().getName().contains(".client.machines."))data=field(component,"data");
                    energy.add(Map.of("type",component.getClass().getName(),"eu",field(data,"eu"),"maxEu",field(data,"maxEu")));
                }
                values.add(Map.of("menu",menu.getClass().getName(),"screen",screen.getClass().getName(),"energy",energy));
            }
            if(values.size()!=1)throw new IllegalStateException("Expected one native MI menu, got "+values.size());
            write("release-mi-energy-state.json",values.get(0));return true;
        }
'''
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
    return text.replace(anchor,anchor+command,1)
