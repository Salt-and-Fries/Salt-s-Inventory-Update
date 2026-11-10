"""A single-click scenario must not accidentally activate native pickup-all."""
import unittest
from single_clicks import SingleClickCadence


class Clock:
    def __init__(self):self.now=0;self.delays=[];self.events=[]
    def sleep(self,delay):self.delays.append(delay);self.now+=delay
    def dispatch(self,command_type,**kw):self.events.append((self.now,command_type,kw));self.now+=.2
    def cadence(self):return SingleClickCadence(self.dispatch,lambda:self.now,self.sleep)


class SingleClickTests(unittest.TestCase):
    def test_repeated_native_cell_clicks_cannot_become_pickup_all(self):
        clock=Clock();clicks=clock.cadence()
        clicks.send('regression-slot-click',session=4,slot=0)
        clicks.send('regression-slot-click',session=4,slot=0)
        self.assertGreater(clock.events[1][0]-clock.events[0][0],.250)
    def test_equal_indices_in_different_sessions_do_not_share_a_click_target(self):
        clock=Clock();clicks=clock.cadence()
        clicks.send('regression-slot-click',session=4,slot=0)
        clicks.send('regression-slot-click',session=5,slot=0)
        self.assertEqual([],clock.delays)
    def test_right_click_splitting_is_not_retimed_as_a_left_double_click(self):
        clock=Clock();clicks=clock.cadence()
        clicks.send('regression-slot-click',session=4,slot=0,button=1)
        clicks.send('regression-slot-click',session=4,slot=0,button=1)
        self.assertEqual([],clock.delays)
    def test_existing_elapsed_time_and_observations_do_not_add_another_delay(self):
        clock=Clock();clicks=clock.cadence()
        clicks.send('regression-player-click',slot=12)
        clock.now+=.400
        clicks.send('regression-state')
        clicks.send('regression-player-click',slot=12)
        self.assertEqual([],clock.delays)
    def test_window_kind_payload_reaches_observer_without_argument_collision(self):
        clock=Clock();clicks=clock.cadence()
        clicks.send('regression-builtin-info',kind='CHARACTER',ticks=2)
        self.assertEqual(('regression-builtin-info',{'kind':'CHARACTER','ticks':2}),clock.events[0][1:])
        self.assertEqual([],clock.delays)


if __name__=='__main__':unittest.main()
