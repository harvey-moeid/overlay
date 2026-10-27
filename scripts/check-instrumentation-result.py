"""Fail CI on an Android test failure even when `am instrument` exits zero."""
import re
import sys
from pathlib import Path


def passed(text: str) -> bool:
    match = re.search(r"\bOK \((\d+) tests?\)", text)
    return bool(match and int(match.group(1)) >= 9 and not any(
        marker in text for marker in ("FAILURES!!!", "INSTRUMENTATION_FAILED", "Process crashed", "shortMsg=Process")
    ))


if __name__ == "__main__":
    report = Path(sys.argv[1]).read_text()
    print(report)
    if not passed(report):
        sys.exit("Android runtime suite did not pass all expected tests")
