import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # LLM Provider: "claude" | "deepseek"
    LLM_PROVIDER: str = os.getenv("LLM_PROVIDER", "claude")

    # Claude / Anthropic
    ANTHROPIC_API_KEY: str = os.getenv("ANTHROPIC_API_KEY", "")
    CLAUDE_MODEL: str = os.getenv("CLAUDE_MODEL", "claude-sonnet-4-6")

    # DeepSeek
    DEEPSEEK_API_KEY: str = os.getenv("DEEPSEEK_API_KEY", "")
    DEEPSEEK_MODEL: str = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")

    # Redis (for response caching)
    REDIS_URL: str = os.getenv("REDIS_URL", "redis://:healify_redis@localhost:6379/0")

    # Planning defaults
    DEFAULT_WEEKS_TO_PLAN: int = 1
    MEALS_PER_DAY: int = 4  # breakfast, lunch, dinner, snack


settings = Settings()
