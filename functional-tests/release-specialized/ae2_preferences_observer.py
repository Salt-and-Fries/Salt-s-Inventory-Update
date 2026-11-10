"""Observe original local AE2 terminal preferences and their real config object."""
import re
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    text=child_observations(text,mc,named);anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-preferences-state")){
            Object desktop=h("screen");var clients=new ArrayList<Object>();if(desktop==null)throw new IllegalStateException("No native terminal desktop");
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!List.of("appeng.client.gui.me.items.TerminalScreen","appeng.client.gui.me.common.TerminalSettingsScreen").contains(content.getClass().getName()))continue;
                Object menu=call(content,"getMenu");boolean child=content.getClass().getName().endsWith(".TerminalSettingsScreen");Object parent=child?call(content,"getParent"):content;
                var row=new LinkedHashMap<String,Object>();row.put("session",call(field(window,"session"),"sessionId"));row.put("screen",content.getClass().getName());row.put("menu",menu.getClass().getName());row.put("parentScreen",parent.getClass().getName());row.put("sameParentMenu",call(parent,"getMenu")==menu);
                var values=new LinkedHashMap<String,Object>();Object config=field(content,"config");
                for(String getter:List.of("isPinAutoCraftedItems","isNotifyForFinishedCraftingJobs","isClearGridOnClose","isUseExternalSearch","isRememberLastSearch","isAutoFocusSearch","isSyncWithExternalSearch","isClearExternalSearchOnOpen")){
                    Object value=call(config,getter);values.put(getter,Map.of("value",value,"javaType",value.getClass().getName()));
                }
                row.put("values",values);var controls=new LinkedHashMap<String,Object>();
                if(child)for(String name:List.of("pinAutoCraftedItemsCheckbox","notifyForFinishedCraftingJobsCheckbox","clearGridOnCloseCheckbox","useInternalSearchRadio","useExternalSearchRadio","rememberCheckbox","autoFocusCheckbox","syncWithExternalCheckbox","clearExternalCheckbox")){
                    Object value=field(content,name);var widget=(net.minecraft.client.gui.components.AbstractWidget)value;
                    controls.put(name,Map.of("selected",call(value,"isSelected"),"visible",widget.visible,"active",widget.active,"x",widget.getX(),"y",widget.getY(),"width",widget.getWidth(),"height",widget.getHeight(),"message",widget.getMessage().getString()));
                }
                row.put("controls",controls);row.put("browserEnabled",releasePreferencesBrowserEnabled());clients.add(row);
            }
            if(clients.size()!=1)throw new IllegalStateException("Expected one original terminal/preferences surface");
            write("release-ae2-preferences-state.json",Map.of("client",clients));return true;
        }
'''
    helpers=r'''
    static boolean releasePreferencesBrowserEnabled(){
        try{return(Boolean)Class.forName("appeng.integration.abstraction.ItemListMod").getMethod("isEnabled").invoke(null);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot read original item-list integration availability",error);}
    }
'''
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
    text=text.replace(anchor,anchor+command,1);return text[:text.rfind('}')]+helpers+'\n}\n'
