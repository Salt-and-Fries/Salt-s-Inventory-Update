"""Execute the shipping optional bridge with absent and present context APIs."""
from pathlib import Path
import hashlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
VERSIONS = ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']
PACKAGE = 'com/salts_inventory_update/compat/toms_storage/client'
SOURCES = [ROOT / 'compatibility/versions' / version / 'fabric/src/main/java' / PACKAGE / 'TerminalPacketContext.java'
           for version in VERSIONS]

class TerminalPacketContextTest(unittest.TestCase):
    def test_all_shipping_families_use_the_same_bridge_at_the_encoder(self):
        self.assertEqual(1, len({hashlib.sha256(p.read_bytes()).hexdigest() for p in SOURCES}))
        for path in SOURCES:
            self.assertIn('TerminalPacketContext.supply(context.minecraft().getConnection(), () -> TomsStoragePayloads.writeTerminalAction(',
                          path.with_name('TomsStorageDesktopWindows.java').read_text())

    def test_absent_and_present_api_execute_once_restore_scope_and_preserve_failures(self):
        javac = Path('C:/Program Files/Java/jdk-25.0.3/bin/javac.exe')
        java = javac.with_name('java.exe')
        with tempfile.TemporaryDirectory(prefix='siu-terminal-context-') as temporary:
            folder = Path(temporary)
            classes = folder / 'classes'; classes.mkdir()
            native = folder / 'native'; native.mkdir()
            provider = folder / 'PacketContextProvider.java'
            provider.write_text('package net.fabricmc.fabric.api.networking.v1.context; public interface PacketContextProvider {}')
            api = folder / 'PacketContext.java'
            api.write_text('''package net.fabricmc.fabric.api.networking.v1.context;
import java.util.function.Supplier;
public final class PacketContext {
  private static final ThreadLocal<Object> CURRENT = new ThreadLocal<>();
  public static Object get() { return CURRENT.get(); }
  public static <T> T supplyWithContext(PacketContextProvider provider, Supplier<T> encoder) {
    Object old = CURRENT.get(); CURRENT.set(provider);
    try { return encoder.get(); } finally { if(old == null) CURRENT.remove(); else CURRENT.set(old); }
  }
}''')
            present = folder / 'Present.java'
            present.write_text('''package com.salts_inventory_update.compat.toms_storage.client;
import net.fabricmc.fabric.api.networking.v1.context.*;
public final class Present {
  static final class Provider implements PacketContextProvider {}
  public static void main(String[] args) {
    Provider owner = new Provider(), nested = new Provider(); int[] calls = {0};
    int result = TerminalPacketContext.supply(owner, () -> {
      calls[0]++; if(PacketContext.get()!=owner)throw new AssertionError("owner context");
      TerminalPacketContext.supply(nested, () -> {if(PacketContext.get()!=nested)throw new AssertionError("nested"); return null;});
      if(PacketContext.get()!=owner)throw new AssertionError("outer restored"); return 42;
    });
    if(result!=42 || calls[0]!=1 || PacketContext.get()!=null)throw new AssertionError("result/scope");
    RuntimeException runtime = new IllegalArgumentException("native encoder failure");
    try {TerminalPacketContext.supply(owner, () -> {throw runtime;});throw new AssertionError("missing failure");}
    catch(RuntimeException actual){if(actual!=runtime)throw new AssertionError("wrapped runtime");}
    Error error = new AssertionError("native error");
    try {TerminalPacketContext.supply(owner, () -> {throw error;});throw new AssertionError("missing error");}
    catch(Error actual){if(actual!=error)throw new AssertionError("wrapped error");}
    try {TerminalPacketContext.supply(new Object(), () -> {throw new AssertionError("invalid provider executed");});throw new AssertionError("missing provider failure");}
    catch(IllegalStateException expected){}
    if(PacketContext.get()!=null)throw new AssertionError("leaked failure scope");
  }
}''')
            absent = folder / 'Absent.java'
            absent.write_text('''package com.salts_inventory_update.compat.toms_storage.client;
public final class Absent { public static void main(String[] args) {
  int[] calls={0};int value=TerminalPacketContext.supply(null,()->{calls[0]++;return 42;});
  if(value!=42||calls[0]!=1)throw new AssertionError("fallback");
  RuntimeException error=new IllegalStateException("same");
  try{TerminalPacketContext.supply(null,()->{throw error;});throw new AssertionError("missing");}
  catch(RuntimeException actual){if(actual!=error)throw new AssertionError("changed");}
}}''')
            subprocess.run([str(javac), '--release', '17', '-d', str(classes), str(SOURCES[0]), str(absent)], check=True)
            subprocess.run([str(java), '-cp', str(classes), PACKAGE.replace('/', '.')+'.Absent'], check=True)
            subprocess.run([str(javac), '--release', '17', '-cp', str(classes), '-d', str(native), str(provider), str(api), str(present)], check=True)
            subprocess.run([str(java), '-cp', str(classes)+';'+str(native), PACKAGE.replace('/', '.')+'.Present'], check=True)

if __name__ == '__main__': unittest.main()
