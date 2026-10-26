import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("result", Path(__file__).with_name("check-instrumentation-result.py"))
result = importlib.util.module_from_spec(spec)
spec.loader.exec_module(result)


class InstrumentationResultTest(unittest.TestCase):
    def test_complete_suite(self):
        self.assertTrue(result.passed("INSTRUMENTATION_RESULT: stream=\nOK (9 tests)\nINSTRUMENTATION_CODE: -1"))

    def test_zero_or_partial_tests_are_not_success(self):
        self.assertFalse(result.passed("OK (0 tests)"))
        self.assertFalse(result.passed("OK (1 test)"))

    def test_failure_even_if_ok_is_present(self):
        self.assertFalse(result.passed("OK (9 tests)\nFAILURES!!!"))

    def test_crash_or_timeout_is_not_success(self):
        self.assertFalse(result.passed("INSTRUMENTATION_FAILED: Process crashed"))
        self.assertFalse(result.passed("INSTRUMENTATION_CODE: 0"))
