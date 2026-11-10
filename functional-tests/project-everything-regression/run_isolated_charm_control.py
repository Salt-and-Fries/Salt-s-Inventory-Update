"""Current SIU jars with the recorded isolated Charm/Trinkets publisher fixture.

This is a separate control. It must not replace the failing mixed-mod cohort.
"""
import argparse,sys
import run_production_matrix as driver

def main():
    p=argparse.ArgumentParser();p.add_argument('--run',required=True);a=p.parse_args()
    driver.FIXTURES={'1.21.1/fabric':['1211-fabric-charm-trinkets-tail-v1']}
    sys.argv=[sys.argv[0],'--run',a.run,'--target','1.21.1/fabric','--cohort','0']
    driver.main()

if __name__=='__main__':main()
