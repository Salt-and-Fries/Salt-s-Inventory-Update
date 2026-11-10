"""Additional real resource and framebuffer checks; preserve all original steps."""
from pathlib import Path
import re

BASE_JAVA = 'functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java'
RESOURCE = 'minecraft:textures/entity/enchanting_table_book.png'
NATIVE_SHA = '8ea11fb9590b0d6e7858ad20adcc196c832af4466601764d6e290d611b0959a0'
TARGETS = ['1.21.11/fabric', '1.21.11/neoforge']
PHASES = ['enchanting_table', 'book-after-reload']

METHOD = r'''    private static void registerNativeBookChecks(String label) {
        Object texture=field(InventoryDesktopScreen.class,"ENCHANTING_BOOK_TEXTURE");
        check(label+"-book-version-correct-identifier",texture.toString().equals("RESOURCE"));
        Object manager=call(mc,"getResourceManager");
        var resource=(java.util.Optional<?>)call(manager,"getResource",texture);
        check(label+"-book-resource-available",resource.isPresent());
        try(var stream=(java.io.InputStream)call(resource.orElseThrow(),"open")) {
            String digest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
            check(label+"-book-native-resource-hash",digest.equals("NATIVE_SHA"));
            record(label+"-book-resource-observed","INFO",texture+" sha256="+digest);
        }catch(Exception failure){throw new IllegalStateException("Cannot inspect actual book resource",failure);}
        int x=((Number)call(InventoryDesktopScreen.class,"enchantmentContentX",activeWindow)).intValue();
        int y=((Number)call(InventoryDesktopScreen.class,"enchantmentContentY",activeWindow)).intValue();
        int width=((Number)field(InventoryDesktopScreen.class,"ENCHANTMENT_BOOK_WIDTH")).intValue();
        int height=((Number)field(InventoryDesktopScreen.class,"ENCHANTMENT_BOOK_HEIGHT")).intValue();
        int guiWidth=((Number)call(mc.getWindow(),"getGuiScaledWidth")).intValue();
        CAPTURE_CHECKS.put(label,path->{
            try {
                var image=javax.imageio.ImageIO.read(path.toFile());double scale=image.getWidth()/(double)guiWidth;
                int coloured=0,black=0,total=0;
                for(int py=(int)Math.floor(y*scale);py<(int)Math.ceil((y+height)*scale);py++)
                    for(int px=(int)Math.floor(x*scale);px<(int)Math.ceil((x+width)*scale);px++) {
                        int rgb=image.getRGB(px,py),red=(rgb>>16)&255,green=(rgb>>8)&255,blue=rgb&255;
                        if(red>70&&red>green+10&&green>blue+6)coloured++;
                        if(red<35&&green<35&&blue<35)black++;
                        total++;
                    }
                String detail="textured="+coloured+" black="+black+" total="+total+" scale="+scale+" bounds="+x+","+y+","+width+","+height;
                record(label+"-book-textured-model-visible",coloured>=8*scale*scale?"PASS":"FAIL",detail);
                record(label+"-book-not-missing-texture-rectangle",black<total*0.75?"PASS":"FAIL",detail);
            }catch(java.io.IOException failure){throw new IllegalStateException("Cannot inspect actual book framebuffer",failure);}
        });
    }
'''.replace('RESOURCE', RESOURCE).replace('NATIVE_SHA', NATIVE_SHA)


def substitutions():
    return [
        ('        add("inventory-after-resource-reload", () -> InventoryDesktopScreen.openOrToggleInventory(mc),',
         '        addContainer("book-after-reload", "minecraft:enchanting_table", 0);\n        add("inventory-after-resource-reload", () -> InventoryDesktopScreen.openOrToggleInventory(mc),'),
        ('            capture(label);\n        }, label.equals("toms-inventory_cable_connector")?100:12);',
         '            if(label.equals("enchanting_table")||label.equals("book-after-reload"))registerNativeBookChecks(label);\n            capture(label);\n        }, label.equals("toms-inventory_cable_connector")?100:12);'),
        ('    private static void registerEmitterPixelChecks(Object context,String label){',
         METHOD + '    private static void registerEmitterPixelChecks(Object context,String label){')]


def derive_java(original):
    text = original
    for before, after in substitutions():
        assert text.count(before) == 1, 'Original ordinary Core harness changed'
        text = text.replace(before, after)
    restored = text
    for before, after in reversed(substitutions()):
        assert restored.count(after) == 1
        restored = restored.replace(after, before)
    assert restored == original, 'An original step/assertion changed'
    return text


def init_source(root, folder):
    root, folder = Path(root).resolve(), Path(folder).resolve()
    assert folder.is_relative_to(root)
    relative = (folder / 'java').relative_to(root).as_posix()
    return '''gradle.projectsEvaluated {
    def root=gradle.rootProject
    def mc=root.providers.gradleProperty('qaMinecraft').get()
    def loader=root.providers.gradleProperty('qaLoader').get()
    if(mc!='1.21.11'||!['fabric','neoforge'].contains(loader)||root.providers.gradleProperty('qaScenario').get()!='core')
        throw new GradleException('Only the two 1.21.11 standalone Core book checks may use this override')
    def target=root.project(":${mc}:${loader}")
    def qa=target.sourceSets.getByName(loader=='fabric'?'gameplayQa':'chestCloseQa')
    def original=root.file(''' + repr(BASE_JAVA) + ''')
    qa.java.srcDir(root.file(''' + repr(relative) + '''))
    target.tasks.named(qa.compileJavaTaskName).configure {
        setSource(source.filter { it.canonicalPath!=original.canonicalPath })
        inputs.file(original)
        outputs.upToDateWhen { false }
    }
}
'''


def verify_report(report):
    assert report['minecraft'] == '1.21.11' and report['loader'] in ['fabric', 'neoforge']
    assert report['scenario'] == 'core' and report['failures'] == 0
    assert report.get('completeCoreFeatureSuite', False) is False
    values = report['results']
    assert not any(v['status'] in ['FAIL', 'FAILED', 'ERROR'] for v in values)
    index = lambda name: [i for i, v in enumerate(values) if v['name'] == name]
    for phase in PHASES:
        for name in ['version-correct-identifier', 'resource-available', 'native-resource-hash',
                     'textured-model-visible', 'not-missing-texture-rectangle']:
            matches = [v for v in values if v['name'] == phase + '-book-' + name]
            assert len(matches) == 1 and matches[0]['status'] == 'PASS', 'Missing actual book observation: ' + phase + '/' + name
        captures = [v for v in values if v['name'] == phase + '-framebuffer']
        assert len(captures) == 1 and captures[0]['status'] == 'CAPTURED'
        observed = [v for v in values if v['name'] == phase + '-book-resource-observed']
        assert len(observed) == 1 and observed[0]['status'] == 'INFO'
        assert observed[0]['details'] == RESOURCE + ' sha256=' + NATIVE_SHA
    assert len(index('resource-pack-reload')) == len(index('desktop-ready-after-resource-reload')) == 1
    assert index('enchanting_table-book-native-resource-hash')[0] < index('resource-pack-reload')[0]
    assert index('desktop-ready-after-resource-reload')[0] < index('book-after-reload-book-native-resource-hash')[0]
    return True


def verify_log(text):
    assert not re.search(r'Missing resource[^\r\n]*enchanting_table_book', text), 'Missing native book texture remains'
    return True
