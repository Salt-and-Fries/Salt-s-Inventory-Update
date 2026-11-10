package probe.logging;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.*;
import org.slf4j.helpers.*;
import org.slf4j.spi.*;
/** Captures real plugin SLF4J calls; it does not suppress or replace the plugin's decisions. */
public final class ProbeProvider implements SLF4JServiceProvider {
    public static final List<String> EVENTS=new ArrayList<>();
    private final ILoggerFactory factory=name->(Logger)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Logger.class},(p,m,a)->{
        if(m.getName().equals("getName"))return name;
        if(m.getReturnType()==boolean.class)return true;
        if(a!=null&&a.length>0&&a[0]instanceof String){
            String text=m.getName()+":"+a[0];
            for(Object v:a)if(v instanceof Throwable t)text+=" | "+t.getClass().getName()+":"+t.getMessage();
            EVENTS.add(text);
        }
        return null;
    });
    public ILoggerFactory getLoggerFactory(){return factory;}
    public IMarkerFactory getMarkerFactory(){return new BasicMarkerFactory();}
    public MDCAdapter getMDCAdapter(){return new BasicMDCAdapter();}
    public String getRequestedApiVersion(){return "2.0.1";}
    public void initialize(){}
}
