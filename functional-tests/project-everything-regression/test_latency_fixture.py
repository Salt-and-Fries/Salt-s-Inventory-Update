"""Exercise the exact excluded Netty handler, including decreasing jitter FIFO."""
from pathlib import Path
import json, subprocess

ROOT=Path(__file__).resolve().parents[2]


def main():
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--run',required=True);args=parser.parse_args()
    folder=ROOT/'build/ae2-click-investigation'/args.run
    assert not folder.exists(),'Use a fresh output directory'
    folder.mkdir(parents=True)
    snippet=Path(__file__).with_name('latency_fixture.java.inc').read_text()
    start=snippet.index('    static final class QaPacketDelay')
    end=snippet.index('    static void qaNetworkDelay',start)
    handler=snippet[start:end]
    source='''public class QaLatencySelfTest {
HANDLER
public static void main(String[] args){
 var delay=new QaPacketDelay();delay.millis=75;delay.jitter=25;
 var channel=new io.netty.channel.embedded.EmbeddedChannel(delay);
 var a=channel.newPromise();var b=channel.newPromise();var c=channel.newPromise();var d=channel.newPromise();
 channel.pipeline().writeAndFlush("a",a);channel.pipeline().writeAndFlush("b",b);channel.pipeline().writeAndFlush("c",c);channel.pipeline().writeAndFlush("d",d);
 if(channel.readOutbound()!=null||delay.pending.get()!=4)throw new AssertionError("packet escaped before delay");
 channel.advanceTimeBy(200,java.util.concurrent.TimeUnit.MILLISECONDS);channel.runScheduledPendingTasks();
 for(String expected:new String[]{"a","b","c","d"})if(!expected.equals(channel.readOutbound()))throw new AssertionError("packet reordered at jitter decrease");
 if(channel.readOutbound()!=null||delay.pending.get()!=0||!a.isSuccess()||!b.isSuccess()||!c.isSuccess()||!d.isSuccess())throw new AssertionError("delivery promises/counts changed");
 delay.millis=0;delay.jitter=0;channel.writeOutbound("immediate");
 if(!"immediate".equals(channel.readOutbound())||delay.pending.get()!=0)throw new AssertionError("zero delay path changed");
 channel.finishAndReleaseAll();System.out.println("FIFO, delayed delivery, native promises, pending counters and zero-delay path passed");
}}
'''.replace('HANDLER',handler)
    java=Path('C:/Program Files/Microsoft/jdk-21.0.10.7-hotspot/bin/java.exe')
    args=(ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-siu-legacy-probe-varied-02/java.args').read_text().splitlines()
    cp=args[args.index('"-cp"')+1].strip('"')
    netty=';'.join(p for p in cp.split(';') if '/io/netty/' in p.replace('\\','/'))
    assert netty
    path=folder/'QaLatencySelfTest.java';path.write_text(source)
    subprocess.run([str(java.with_name('javac.exe')),'--release','17','-cp',netty,'-d',str(folder),str(path)],check=True,capture_output=True)
    run=subprocess.run([str(java),'-cp',str(folder)+';'+netty,'QaLatencySelfTest'],capture_output=True,text=True)
    (folder/'stdout.log').write_text(run.stdout);(folder/'stderr.log').write_text(run.stderr)
    if run.returncode:print(run.stderr)
    run.check_returncode()
    (folder/'result.json').write_text(json.dumps({'passed':True,'scope':'Actual QA Netty handler in EmbeddedChannel; does not count as a Minecraft functional pass.','checks':5},indent=2)+'\n')
    print(run.stdout.strip())


if __name__=='__main__':main()
