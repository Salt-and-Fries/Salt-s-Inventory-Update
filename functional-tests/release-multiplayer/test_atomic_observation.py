"""Exercise the actual excluded Java recorder against a Windows shared-file reader."""
from pathlib import Path
import os,subprocess,tempfile,time,unittest

ROOT=Path(__file__).resolve().parents[2]
JAVA=Path('C:/Program Files/Java/jdk-26/bin/java.exe')

@unittest.skipUnless(os.name=='nt','Windows sharing semantics are the regression subject')
class AtomicPublication(unittest.TestCase):
    def test_transient_reader_releases_without_reexecuting_the_observation_and_persistent_lock_fails(self):
        with tempfile.TemporaryDirectory() as folder:
            directory=Path(folder);source=directory/'ObservationCheck.java';classes=directory/'classes';classes.mkdir()
            source.write_text('''import java.nio.file.*;
import com.siu_release_remote.AtomicObservation;
public class ObservationCheck {public static void main(String[] args)throws Exception {
 AtomicObservation.write(Path.of(args[0]),"new-observation");
 System.out.println("published-once"); }}''')
            subprocess.run([str(JAVA.with_name('javac.exe')),'--release','17','-d',str(classes),str(source),
                str(Path(__file__).with_name('AtomicObservation.java'))],check=True,capture_output=True)
            target=directory/'reply.json';target.write_text('previous-observation')
            with target.open('rb') as reader:
                process=subprocess.Popen([str(JAVA),'-cp',str(classes),'ObservationCheck',str(target)],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                time.sleep(.25);self.assertIsNone(process.poll());self.assertEqual(b'previous-observation',reader.read())
            out,error=process.communicate(timeout=5);self.assertEqual(0,process.returncode,error.decode());self.assertEqual(b'published-once\r\n',out)
            self.assertEqual('new-observation',target.read_text())
            target.write_text('unchanged-on-permanent-failure')
            with target.open('rb'):
                failed=subprocess.run([str(JAVA),'-cp',str(classes),'ObservationCheck',str(target)],capture_output=True,timeout=5)
            self.assertNotEqual(0,failed.returncode);self.assertIn(b'FileSystemException',failed.stderr)
            self.assertEqual('unchanged-on-permanent-failure',target.read_text())

if __name__=='__main__':unittest.main()
