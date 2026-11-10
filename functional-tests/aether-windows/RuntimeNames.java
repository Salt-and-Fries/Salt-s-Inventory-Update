package com.siu_reference;
import java.lang.reflect.Field;
import java.util.Set;
/** This excluded Neo runtime already uses the official named namespace. */
final class RuntimeNames {
    static String className(String name){return name;}
    static Set<String> names(Class<?> type,String name,String kind){return Set.of(name);}
    static Field field(Class<?> type,String name)throws NoSuchFieldException{return type.getDeclaredField(name);}
}
