// Bootstrap-visible, excluded QA recorder. Never mutates menus or retries input.
package siu.qa.clickprobe;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class UiClickProbeRuntime {
    private static final int LIMIT = 16384;
    private static final List<Map<String,Object>> EVENTS = new ArrayList<>();
    private static final ThreadLocal<Deque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);
    private static Path output;
    private static int overflow;
    private static final Set<Integer> SENT=new HashSet<>(),PROCESSED=new HashSet<>();
    private static final class Frame {
        final Object player,sessions,envelope;
        final Map<String,Object> row=new LinkedHashMap<>();
        Object payload;
        Frame(Object player,Object sessions,Object envelope){this.player=player;this.sessions=sessions;this.envelope=envelope;}
    }
    public static void configure(String path){
        output=Path.of(path);
        Runtime.getRuntime().addShutdownHook(new Thread(UiClickProbeRuntime::flush,"SIU-click-probe-flush"));
    }
    private static Object invoke(Object owner,String name,Object...args) throws Exception {
        Class<?> start=owner instanceof Class<?> c?c:owner.getClass();
        for(Class<?> c=start;c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods()){
            if(!m.getName().equals(name)||m.getParameterCount()!=args.length)continue;
            try{m.setAccessible(true);return m.invoke(owner instanceof Class<?>?null:owner,args);}
            catch(IllegalArgumentException mismatch){}
        }
        throw new NoSuchMethodException(start.getName()+"."+name);
    }
    private static Object field(Object owner,String...names) throws Exception {
        for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())for(String name:names)try{
            Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(owner);
        }catch(NoSuchFieldException absent){}
        throw new NoSuchFieldException(Arrays.toString(names));
    }
    private static Object playerMenu(Object player) throws Exception {
        try{return field(player,"inventoryMenu","f_36095_");}catch(NoSuchFieldException remapped){}
        for(Class<?> c=player.getClass();c!=null;c=c.getSuperclass())for(Field f:c.getDeclaredFields()){
            if(Modifier.isStatic(f.getModifiers()))continue;
            f.setAccessible(true);Object value=f.get(player);
            if(value==null)continue;
            try{if(((Number)field(value,"containerId","field_7763","f_38840_")).intValue()==0){field(value,"stateId","field_34024","f_182405_");return value;}}
            catch(Exception notMenu){}
        }
        throw new IllegalStateException("Player inventory menu not found");
    }
    private static Map<String,Object> menu(Frame f,int session,int slot) {
        Map<String,Object> result=new LinkedHashMap<>();
        try{
            Object menu=session==0?playerMenu(f.player):field(((Map<?,?>)field(f.sessions,"sessions")).get(session),"menu");
            result.put("revision",field(menu,"stateId","field_34024","f_182405_"));
            result.put("class",menu.getClass().getName());
            if(slot>=0){
                Class<?> bindings=Class.forName("com.salts_inventory_update.api.desktop.DesktopMenuBindings",false,menu.getClass().getClassLoader());
                // Mod-defined menu classes may have a different loader from vanilla.
                Object nativeSlot=invoke(bindings,"slot",menu,slot);
                Object stack;
                try{stack=invoke(nativeSlot,"getItem");}catch(Exception remapped){try{stack=invoke(nativeSlot,"method_7677");}catch(Exception forge){stack=invoke(nativeSlot,"m_7993_");}}
                result.put("stack",String.valueOf(stack));
            }
            Object carried;
            try{carried=invoke(menu,"getCarried");}catch(Exception remapped){try{carried=invoke(menu,"method_34255");}catch(Exception forge){carried=invoke(menu,"m_142621_");}}
            result.put("carried",String.valueOf(carried));
        }catch(Throwable error){result.put("read_error",error.toString());}
        return result;
    }
    private static void store(Map<String,Object> row){synchronized(EVENTS){if(EVENTS.size()<LIMIT)EVENTS.add(row);else overflow++;}}
    public static void client(int id,int session,int slot,int button,Object input,Object carried,Object owner){
        try{
            Class<?> source=(Class<?>)owner;
            boolean modern=false;
            try{Field collected=source.getDeclaredField("collectedClicks");collected.setAccessible(true);
                if(collected.get(null)!=null)return; // A batched gesture has a separate acknowledgement.
            }catch(NoSuchFieldException queuedProtocol){modern=true;}
            Map<String,Object> row=new LinkedHashMap<>();row.put("event","client-click");row.put("at",System.nanoTime());
            row.put("click",id);row.put("session",session);row.put("slot",slot);row.put("button",button);
            row.put("input",String.valueOf(input));row.put("carried",String.valueOf(carried));
            if(modern){Object auth=invoke(owner,"sessionAuth",session);row.put("revision",auth==null?-1:invoke(auth,"stateId"));row.put("queued_input",true);}
            else row.put("revision",invoke(owner,"stateIdFor",session));store(row);
            if(!modern)synchronized(SENT){SENT.add(id);}
        }catch(Throwable error){error("client",error);}
    }
    public static void packet(Object payload){
        if(!payload.getClass().getSimpleName().equals("DesktopClickPayload"))return;
        try{payload.getClass().getDeclaredMethod("mutationId");}catch(NoSuchMethodException legacyEnvelope){return;}
        try{
            int id=((Number)invoke(payload,"debugId")).intValue();
            Map<String,Object> row=new LinkedHashMap<>();row.put("event","client-click-packet");row.put("at",System.nanoTime());row.put("click",id);
            row.put("mutation",invoke(payload,"mutationId"));row.put("revision",invoke(payload,"stateId"));
            row.put("session",invoke(payload,"sessionId"));row.put("slot",invoke(payload,"slotIndex"));row.put("carried",String.valueOf(invoke(payload,"clientCarried")));
            store(row);synchronized(SENT){SENT.add(id);}
        }catch(Throwable error){error("packet",error);}
    }
    public static void clientLog(String format,Object[] args){
        if(format.contains("dropped")||format.contains("blocked")||format.contains("rejected"))
            store(new LinkedHashMap<>(Map.of("event","client-decision","at",System.nanoTime(),"format",format,"args",Arrays.stream(args).map(String::valueOf).toList())));
    }
    public static void modernEnter(Object player,Object payload,Object owner){
        Object sessions=null;
        try{Field f=((Class<?>)owner).getDeclaredField("PLAYERS");f.setAccessible(true);sessions=((Map<?,?>)f.get(null)).get(player);}
        catch(Throwable error){error("modern-enter",error);}
        Frame frame=new Frame(player,sessions,payload);FRAMES.get().push(frame);
        try{frame.row.put("event","server-mutation");frame.row.put("at",System.nanoTime());frame.row.put("type","desktop_click");
            frame.row.put("expected_primary_revision",invoke(payload,"stateId"));frame.row.put("mutation",invoke(payload,"mutationId"));}
        catch(Throwable error){frame.row.put("read_error",error.toString());}
        decoded(payload);
    }
    public static void enter(Object registration,Object player,Object sessions,Object envelope){
        Frame f=new Frame(player,sessions,envelope);FRAMES.get().push(f);
        try{f.row.put("event","server-mutation");f.row.put("at",System.nanoTime());f.row.put("type",String.valueOf(invoke(envelope,"innerType")));
            f.row.put("expected_primary_revision",invoke(envelope,"primaryStateId"));
            f.row.put("expected_secondary_revision",invoke(envelope,"secondaryStateId"));
        }catch(Throwable error){f.row.put("read_error",error.toString());}
    }
    public static void decoded(Object payload){
        Frame f=FRAMES.get().peek();if(f==null)return;f.payload=payload;
        try{
            if(!payload.getClass().getSimpleName().equals("DesktopClickPayload"))return;
            int session=((Number)invoke(payload,"sessionId")).intValue(),slot=((Number)invoke(payload,"slotIndex")).intValue();
            f.row.put("click",invoke(payload,"debugId"));f.row.put("session",session);f.row.put("slot",slot);
            f.row.put("button",invoke(payload,"button"));f.row.put("input",invoke(payload,"inputName"));
            f.row.put("client_carried",String.valueOf(invoke(payload,"clientCarried")));
            f.row.put("before",menu(f,session,slot));f.row.put("player_before",menu(f,0,-1));
            try{
                var snapshots=(Map<?,?>)field(field(f.sessions,"playerClickSnapshots"),"snapshots");
                int revision=((Number)f.row.get("expected_primary_revision")).intValue();
                Object published=snapshots.get(revision);
                f.row.put("published_player_snapshot",published==null?Map.of("missing",true,"known_revisions",String.valueOf(snapshots.keySet())):Map.of(
                    "revision",revision,"carried",String.valueOf(field(published,"carried")),
                    "age_nanos",System.nanoTime()-((Number)field(published,"publishedAt")).longValue()));
            }catch(NoSuchFieldException modernProtocol){ }
        }catch(Throwable error){f.row.put("read_error",error.toString());}
    }
    public static void accepted(){Frame f=FRAMES.get().peek();if(f!=null)f.row.put("handler_entered",true);}
    public static void rejected(String format,Object[] args){
        Frame f=FRAMES.get().peek();if(f==null||!(format.contains("rejected")||format.contains("dropped")||format.contains("mismatch")))return;
        f.row.put("rejection_format",format);f.row.put("rejection_args",Arrays.stream(args).map(String::valueOf).toList());
    }
    public static void exit(){
        Deque<Frame> frames=FRAMES.get();if(frames.isEmpty())return;Frame f=frames.pop();
        if(f.payload==null||!f.payload.getClass().getSimpleName().equals("DesktopClickPayload"))return;
        try{
            f.row.putIfAbsent("handler_entered",false);
            f.row.put("after",menu(f,((Number)f.row.get("session")).intValue(),((Number)f.row.get("slot")).intValue()));
            f.row.put("player_after",menu(f,0,-1));f.row.put("finished_at",System.nanoTime());store(f.row);
            synchronized(SENT){PROCESSED.add(((Number)f.row.get("click")).intValue());}
            if(!Boolean.TRUE.equals(f.row.get("handler_entered")))System.err.println("SIU_CLICK_PROBE_REJECT "+json(f.row));
        }catch(Throwable error){error("exit",error);}
    }
    public static Map<String,Object> fence(){
        synchronized(SENT){Set<Integer> outstanding=new TreeSet<>(SENT);outstanding.removeAll(PROCESSED);
            return Map.of("submitted",SENT.size(),"processed",PROCESSED.size(),"outstanding",new ArrayList<>(outstanding));}
    }
    private static void error(String where,Throwable error){store(new LinkedHashMap<>(Map.of("event","probe-error","where",where,"error",error.toString())));}
    private static String json(Object value){
        if(value==null)return "null";
        if(value instanceof Number||value instanceof Boolean)return value.toString();
        if(value instanceof Map<?,?> map){List<String> parts=new ArrayList<>();map.forEach((k,v)->parts.add(json(String.valueOf(k))+":"+json(v)));return "{"+String.join(",",parts)+"}";}
        if(value instanceof Iterable<?> list){List<String> parts=new ArrayList<>();for(Object v:list)parts.add(json(v));return "["+String.join(",",parts)+"]";}
        return "\""+value.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";
    }
    public static void flush(){
        if(output==null)return;
        try{synchronized(EVENTS){List<String> lines=new ArrayList<>();for(var e:EVENTS)lines.add(json(e));lines.add(json(Map.of("event","probe-footer","overflow",overflow)));Files.write(output,lines);}}
        catch(Exception error){System.err.println("SIU_CLICK_PROBE_FLUSH_ERROR "+error);}
    }
}
