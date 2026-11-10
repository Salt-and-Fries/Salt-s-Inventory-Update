import unittest
from run_native_login_probe import bumblezone_login_failure


class NativeLoginFailureTests(unittest.TestCase):
    def test_requires_terminal_disconnect_and_the_exact_native_packet_cause(self):
        lines=['queen_main_trades_sync_packet','java.lang.IllegalStateException: Stack must be non-empty',
               'Client disconnected with reason: Disconnected']
        self.assertTrue(bumblezone_login_failure('\n'.join(lines)))
        for index in range(len(lines)):
            self.assertFalse(bumblezone_login_failure('\n'.join(v for i,v in enumerate(lines) if i!=index)))


if __name__=='__main__':unittest.main()
