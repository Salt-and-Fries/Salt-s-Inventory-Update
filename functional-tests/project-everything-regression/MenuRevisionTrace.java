// Excluded, read-only JDI diagnostic for the exact 1.20.1 Fabric namespace.
// Breakpoints briefly suspend only the server thread. This is not pass evidence.
import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.nio.file.*;
import java.util.*;

class MenuRevisionTrace {
    static String quote(String s) { return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\""; }
    public static void main(String[] args) throws Exception {
        var connector=Bootstrap.virtualMachineManager().attachingConnectors().stream()
            .filter(c->c.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();
        var options=connector.defaultArguments();options.get("hostname").setValue("127.0.0.1");options.get("port").setValue(args[0]);
        VirtualMachine vm=connector.attach(options);Path output=Path.of(args[1]);
        var traces=new LinkedHashMap<String,Integer>();var samples=new LinkedHashMap<String,String>();int events=0;
        try {
            ReferenceType type=vm.classesByName("net.minecraft.class_1703").stream().findFirst().orElseThrow();
            Method method=type.methodsByName("method_37422","()I").stream().findFirst().orElseThrow();
            ThreadReference server=vm.allThreads().stream().filter(t->t.name().equals("Server thread")).findFirst().orElseThrow();
            var request=vm.eventRequestManager().createBreakpointRequest(method.location());
            request.addThreadFilter(server);request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);request.enable();
            Files.writeString(Path.of(args[1]+".ready"),"attached to actual vanilla incrementStateId; no method invocation or state assignment\n");
            long deadline=System.nanoTime()+60_000_000_000L;
            while(System.nanoTime()<deadline&&events<400) {
                EventSet set=vm.eventQueue().remove(500);if(set==null)continue;
                try {
                    for(Event event:set)if(event instanceof BreakpointEvent hit) {
                        ObjectReference menu=hit.thread().frame(0).thisObject();
                        Field id=type.fieldByName("field_7763");
                        if(!(menu.getValue(id) instanceof IntegerValue number)||number.value()!=0)continue;
                        events++;var frames=new ArrayList<String>();
                        for(StackFrame frame:hit.thread().frames()) {
                            String arguments;
                            try{arguments=frame.getArgumentValues().toString();}catch(Exception e){arguments="unavailable";}
                            frames.add(frame.location().declaringType().name()+"."+frame.location().method().name()+":"+frame.location().lineNumber()+" "+arguments);
                        }
                        String key=String.join("\n",frames.stream().map(s->s.replaceAll("id=\\d+","id=*")).toList());
                        traces.merge(key,1,Integer::sum);
                        samples.putIfAbsent(key,"{\"menuClass\":"+quote(menu.referenceType().name())+",\"stateBefore\":"+menu.getValue(type.fieldByName("field_34024"))+",\"frames\":["+String.join(",",frames.stream().map(MenuRevisionTrace::quote).toList())+"]}");
                    }
                } finally {set.resume();}
            }
        } finally {
            try{vm.dispose();}catch(VMDisconnectedException ignored){}
            var rows=new ArrayList<String>();for(var entry:traces.entrySet())rows.add("{\"count\":"+entry.getValue()+",\"sample\":"+samples.get(entry.getKey())+"}");
            Files.writeString(output,"{\"scope\":\"read-only debugger diagnosis; thread breakpoints alter timing, so never functional pass evidence\",\"playerRevisionEvents\":"+events+",\"stacks\":["+String.join(",",rows)+"]}\n");
        }
    }
}
