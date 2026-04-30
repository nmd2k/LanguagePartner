"""Smoke test for LanguagePartner server.

This test verifies the server can:
1. Start without crashing
2. Accept WebSocket connections
3. Receive config message
4. Receive audio frames (silence)
5. Respond without errors

Runs WITHOUT loading actual models (mocks backends).
"""
import os
import subprocess
import sys
import time
import json
from threading import Thread
from typing import Optional

import pytest

# Try to import websockets; skip if not available
try:
    import websockets
    import asyncio
except ImportError:
    pytest.skip("websockets not installed; run: pip install websockets", allow_module_level=True)


SERVER_HOST = "127.0.0.1"
SERVER_PORT = 8765  # Use non-default port to avoid conflicts
SERVER_URL = f"ws://{SERVER_HOST}:{SERVER_PORT}"

# Path to server.py relative to this test file
SERVER_SCRIPT = os.path.join(
    os.path.dirname(__file__),
    "..",
    "server.py",
)


class ServerProcess:
    """Context manager for starting/stopping the server subprocess."""

    def __init__(self, port: int = SERVER_PORT):
        self.port = port
        self.process: Optional[subprocess.Popen] = None

    def start(self) -> None:
        """Start server with MOCK_MODE=1 to skip model loading."""
        env = os.environ.copy()
        env["MOCK_MODE"] = "1"  # Tell server to use mock backends

        self.process = subprocess.Popen(
            [
                sys.executable,
                SERVER_SCRIPT,
                "--port",
                str(self.port),
                "--model",
                "tiny",
                "--device",
                "cpu",
            ],
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )

        # Wait for server to be ready (max 10 seconds)
        start_time = time.time()
        while time.time() - start_time < 10:
            if self.process.poll() is not None:
                # Process exited early
                output = self.process.stdout.read()
                raise RuntimeError(f"Server exited early:\n{output}")

            # Check if server is listening by trying to connect
            try:
                asyncio.get_event_loop().run_until_complete(
                    self._check_connection()
                )
                return  # Server is ready
            except ConnectionRefusedError:
                time.sleep(0.5)

        raise TimeoutError("Server did not start within 10 seconds")

    async def _check_connection(self) -> None:
        """Try to connect to the server; raises if refused."""
        try:
            async with websockets.connect(
                SERVER_URL, open_timeout=1
            ):
                pass
        except Exception:
            raise ConnectionRefusedError()

    def stop(self) -> None:
        """Stop the server process."""
        if self.process is not None:
            self.process.terminate()
            try:
                self.process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait()

            # Print any output for debugging
            output = self.process.stdout.read()
            if output:
                print(f"Server output:\n{output}")

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.stop()


class TestSmoke:
    """Smoke tests for the LanguagePartner server."""

    def test_server_starts_and_accepts_connection(self):
        """Verify server starts and accepts a WebSocket connection."""
        with ServerProcess() as server:
            # If we got here, server started successfully
            assert server.process is not None
            assert server.process.poll() is None  # Still running

    def test_server_receives_config_and_audio(self):
        """Verify server can receive config + audio frames without crashing."""

        async def run_test():
            async with websockets.connect(SERVER_URL) as ws:
                # Send config message
                config = {
                    "type": "config",
                    "sample_rate": 16000,
                    "mode": "speak",
                }
                await ws.send(json.dumps(config))

                # Give server time to process config
                await asyncio.sleep(0.1)

                # Send silence frames (10 chunks of 1024 bytes each = ~0.32 seconds)
                silence_chunk = bytes(1024)  # All zeros = silence
                for _ in range(10):
                    await ws.send(silence_chunk)
                    await asyncio.sleep(0.05)

                # Wait for any response (or timeout)
                try:
                    response = await asyncio.wait_for(ws.recv(), timeout=2.0)
                    # Server may send translation (empty) or nothing
                    data = json.loads(response)
                    # If we get a response, it should be valid JSON
                    assert "type" in data
                except asyncio.TimeoutError:
                    # No response is also OK for silence
                    pass

                # Server should still be running
                assert ws.open

        with ServerProcess():
            asyncio.get_event_loop().run_until_complete(run_test())

    def test_server_handles_invalid_config(self):
        """Verify server handles invalid config message gracefully."""

        async def run_test():
            async with websockets.connect(SERVER_URL) as ws:
                # Send invalid config (missing "type" field)
                invalid_config = {"sample_rate": 16000}
                await ws.send(json.dumps(invalid_config))

                # Connection should close or send error
                try:
                    response = await asyncio.wait_for(ws.recv(), timeout=2.0)
                    data = json.loads(response)
                    # If server responds, should be error type
                    assert data.get("type") == "error" or ws.closed
                except asyncio.TimeoutError:
                    # Server may just close the connection
                    pass

                # Connection should be closed
                assert ws.closed

        with ServerProcess():
            asyncio.get_event_loop().run_until_complete(run_test())

    def test_server_handles_disconnect(self):
        """Verify server handles client disconnect gracefully."""

        async def run_test():
            async with websockets.connect(SERVER_URL) as ws:
                # Send valid config
                config = {"type": "config", "sample_rate": 16000, "mode": "read"}
                await ws.send(json.dumps(config))
                await asyncio.sleep(0.1)

                # Send a few audio frames
                for _ in range(5):
                    await ws.send(bytes(1024))
                    await asyncio.sleep(0.05)

                # Disconnect without proper close
                await ws.close()

        with ServerProcess() as server:
            asyncio.get_event_loop().run_until_complete(run_test())
            # Give server time to handle disconnect
            time.sleep(0.5)
            # Server should still be running
            assert server.process.poll() is None

    def test_multiple_sequential_connections(self):
        """Verify server can handle multiple connections one after another."""

        async def connect_and_send():
            async with websockets.connect(SERVER_URL) as ws:
                config = {"type": "config", "sample_rate": 16000, "mode": "speak"}
                await ws.send(json.dumps(config))
                await asyncio.sleep(0.1)
                await ws.send(bytes(1024))
                await asyncio.sleep(0.1)

        with ServerProcess() as server:
            loop = asyncio.get_event_loop()

            # Three sequential connections
            for i in range(3):
                loop.run_until_complete(connect_and_send())
                # Verify server still running
                assert server.process.poll() is None


# ---------------------------------------------------------------------------
# Mock backend support
# ---------------------------------------------------------------------------

def pytest_configure(config):
    """Set up mock mode before tests run."""
    os.environ["MOCK_MODE"] = "1"


def pytest_unconfigure(config):
    """Clean up after tests."""
    os.environ.pop("MOCK_MODE", None)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
