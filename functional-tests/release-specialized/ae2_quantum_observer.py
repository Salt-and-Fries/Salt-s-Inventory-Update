"""Read the actual quantum chamber, its formation, frequency and full item."""
import re
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    text=child_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-quantum-state")){
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");var host=level.getBlockEntity(POS);
                if(host==null||!host.getClass().getName().equals("appeng.blockentity.qnb.QuantumBridgeBlockEntity"))throw new IllegalStateException("Original quantum chamber missing");
                Object inventory=call(host,"getInternalInventory"),frequency=call(host,"getQEFrequency");var item=(ItemStack)call(inventory,"getStackInSlot",0);
                write("release-ae2-quantum-state.json",Map.of("position",POS.toShortString(),"host",host.getClass().getName(),
                    "formed",call(host,"isFormed"),"frequency",frequency,"frequencyJavaType",frequency.getClass().getName(),"item",regressionStack(item)));return null;
            });return true;
        }
'''
    if named:command=re.sub(r'\bPOS\b','origin()',command)
    else:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
    return text.replace(anchor,anchor+command,1)
