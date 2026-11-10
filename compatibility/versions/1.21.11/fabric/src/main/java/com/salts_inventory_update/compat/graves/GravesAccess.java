package com.salts_inventory_update.compat.graves;

final class GravesAccess {
    private GravesAccess() { }
    static Object call(Object owner,String name,Object...args){
        for(var m:owner.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{return m.invoke(owner,args);}
        catch(IllegalArgumentException mismatch){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name+": "+failure.getCause(),failure);}
        throw new IllegalStateException("Universal Graves method missing: "+name);
    }
    static Object field(Object owner,String name){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException next){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Universal Graves field missing: "+name);
    }
    static void set(Object owner,String name,Object value){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);return;}
        catch(NoSuchFieldException next){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Universal Graves field missing: "+name);
    }
}
