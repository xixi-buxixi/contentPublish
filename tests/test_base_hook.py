import contextlib
import importlib.util
import io
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BASE_HOOK_PATH = ROOT / "hooks" / "base_hook.py"


def load_base_hook():
    spec = importlib.util.spec_from_file_location("base_hook_under_test", BASE_HOOK_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class BaseHookTests(unittest.TestCase):
    def make_agent(self, workspace: Path, name: str = "DemoAgent") -> Path:
        hooks_dir = workspace / "hooks"
        hooks_dir.mkdir()
        prompt_root = workspace / "agentsPrompt"
        prompt_root.mkdir()
        (workspace / "agent.md").write_text("# Project Agent\nRoot nav", encoding="utf-8")
        agent_dir = workspace / "agentsPrompt" / name
        agent_dir.mkdir(parents=True)
        (agent_dir / "agent.md").write_text("# Module Agent\nModule nav", encoding="utf-8")
        return agent_dir

    def run_hook(self, module, workspace: Path, agent_name: str = "DemoAgent"):
        module.__file__ = str(workspace / "hooks" / "base_hook.py")
        stream = io.StringIO()
        with contextlib.redirect_stdout(stream):
            result = module.run_hook(agent_name)
        return result, stream.getvalue()

    def test_successful_run_records_codex_state(self):
        module = load_base_hook()
        with tempfile.TemporaryDirectory() as temp:
            workspace = Path(temp)
            agent_dir = self.make_agent(workspace)

            result, output = self.run_hook(module, workspace)

            state = json.loads((agent_dir / "state.json").read_text(encoding="utf-8"))
            self.assertEqual(result, 0)
            self.assertEqual(state["cycle"], 1)
            self.assertEqual(state["max_cycles"], 3)
            self.assertEqual(state["agent"], "DemoAgent")
            self.assertIn("Codex agent context", output)
            self.assertIn("Cycle 1/3", output)
            self.assertIn("Project Agent", output)
            self.assertIn("Module Agent", output)
            self.assertNotIn("Requirements Document", output)
            self.assertNotIn("API Document", output)

    def test_run_loads_workspace_dotenv_without_overriding_existing_env(self):
        module = load_base_hook()
        with tempfile.TemporaryDirectory() as temp:
            workspace = Path(temp)
            agent_dir = self.make_agent(workspace)
            (workspace / "agent.md").write_text("# Project Agent\n" + ("Root nav " * 20), encoding="utf-8")
            (workspace / ".env").write_text(
                "CODEX_HOOK_MAX_CHARS=20\n"
                "CODEX_HOOK_OUTPUT=brief\n"
                "PULSE_EXISTING_VALUE=from_file\n",
                encoding="utf-8",
            )

            old_max_chars = os.environ.pop("CODEX_HOOK_MAX_CHARS", None)
            old_output = os.environ.pop("CODEX_HOOK_OUTPUT", None)
            old_existing = os.environ.get("PULSE_EXISTING_VALUE")
            os.environ["PULSE_EXISTING_VALUE"] = "from_env"
            try:
                result, output = self.run_hook(module, workspace)
                state = json.loads((agent_dir / "state.json").read_text(encoding="utf-8"))

                self.assertEqual(result, 0)
                self.assertEqual(state["cycle"], 1)
                self.assertEqual(os.environ["PULSE_EXISTING_VALUE"], "from_env")
                self.assertEqual(os.environ["CODEX_HOOK_MAX_CHARS"], "20")
                self.assertIn("Truncated Project Agent Overview", output)
            finally:
                if old_max_chars is None:
                    os.environ.pop("CODEX_HOOK_MAX_CHARS", None)
                else:
                    os.environ["CODEX_HOOK_MAX_CHARS"] = old_max_chars
                if old_output is None:
                    os.environ.pop("CODEX_HOOK_OUTPUT", None)
                else:
                    os.environ["CODEX_HOOK_OUTPUT"] = old_output
                if old_existing is None:
                    os.environ.pop("PULSE_EXISTING_VALUE", None)
                else:
                    os.environ["PULSE_EXISTING_VALUE"] = old_existing

    def test_third_cycle_requires_fresh_agent_docs(self):
        module = load_base_hook()
        with tempfile.TemporaryDirectory() as temp:
            workspace = Path(temp)
            agent_dir = self.make_agent(workspace)
            root_agent = workspace / "agent.md"
            module_agent = agent_dir / "agent.md"
            (agent_dir / "state.json").write_text(
                json.dumps(
                    {
                        "agent": "DemoAgent",
                        "cycle": 3,
                        "agent_doc_mtimes": {
                            "project": root_agent.stat().st_mtime,
                            "module": module_agent.stat().st_mtime,
                        },
                    }
                ),
                encoding="utf-8",
            )

            with self.assertRaises(SystemExit) as ctx:
                self.run_hook(module, workspace)

            self.assertEqual(ctx.exception.code, 1)

    def test_fresh_agent_docs_reset_cycle(self):
        module = load_base_hook()
        with tempfile.TemporaryDirectory() as temp:
            workspace = Path(temp)
            agent_dir = self.make_agent(workspace)
            root_agent = workspace / "agent.md"
            module_agent = agent_dir / "agent.md"
            root_mtime = root_agent.stat().st_mtime
            module_mtime = module_agent.stat().st_mtime
            (agent_dir / "state.json").write_text(
                json.dumps(
                    {
                        "agent": "DemoAgent",
                        "cycle": 3,
                        "agent_doc_mtimes": {
                            "project": root_mtime - 100,
                            "module": module_mtime - 100,
                        },
                    }
                ),
                encoding="utf-8",
            )

            result, output = self.run_hook(module, workspace)

            state = json.loads((agent_dir / "state.json").read_text(encoding="utf-8"))
            self.assertEqual(result, 0)
            self.assertEqual(state["cycle"], 1)
            self.assertEqual(state["agent_doc_mtimes"]["project"], root_mtime)
            self.assertEqual(state["agent_doc_mtimes"]["module"], module_mtime)
            self.assertIn("Fresh agent.md files detected", output)


if __name__ == "__main__":
    unittest.main()
