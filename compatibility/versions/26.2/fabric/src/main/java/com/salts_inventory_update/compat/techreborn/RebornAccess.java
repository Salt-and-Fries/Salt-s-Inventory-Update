package com.salts_inventory_update.compat.techreborn;

import java.lang.reflect.*;

/** Optional publisher members only; Minecraft members use their mapped API. */
public final class RebornAccess {
    private RebornAccess() { }
    public static Object staticField(Class<?> type,String name){try{var f=type.getField(name);f.setAccessible(true);return f.get(null);}catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}}
    public static Object codec(Class<?> type){try{return staticField(type,"PACKET_CODEC");}catch(IllegalStateException alternate){return staticField(type,"CODEC");}}
    public static Object field(Object owner,String name){
        if(owner==null)return null;
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}
        return null;
    }
    public static Object call(Object owner,String name,Object...args){
        Class<?> root=owner instanceof Class<?> c?c:owner.getClass();
        var methods=new java.util.ArrayList<java.lang.reflect.Method>(java.util.Arrays.asList(root.getMethods()));
        for(Class<?> t=root;t!=null;t=t.getSuperclass())methods.addAll(java.util.Arrays.asList(t.getDeclaredMethods()));
        for(var m:methods)if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{
            m.setAccessible(true);return m.invoke(owner instanceof Class<?>?null:owner,args);
        }catch(IllegalArgumentException mismatch){}catch(InvocationTargetException e){throw new IllegalStateException(name,e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}
        throw new IllegalStateException("Missing Reborn member "+name);
    }
    public static Class<?> type(String name){try{return Class.forName(name);}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}
    static void set(Object owner,String name,Object value){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);return;}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}
        throw new IllegalStateException(name);
    }
}
