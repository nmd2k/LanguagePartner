"""pytest configuration — add server/ to sys.path so imports work without install."""
import sys
import os

# Ensure server/ is on sys.path so `import backend`, `import vad`, etc. work.
sys.path.insert(0, os.path.dirname(__file__))
