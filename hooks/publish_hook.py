import os
import sys

# Ensure hooks/ directory is in sys.path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
import base_hook

if __name__ == "__main__":
    base_hook.run_hook("PublishAgent")
