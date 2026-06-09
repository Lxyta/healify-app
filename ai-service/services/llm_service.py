"""
LLM Service — 统一封装 Claude 和 DeepSeek 调用。
"""
import json
from config import settings
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_core.output_parsers import StrOutputParser


def get_llm(temperature: float = 0.7):
    """根据配置返回对应的 LLM 实例。"""
    if settings.LLM_PROVIDER == "claude":
        try:
            from langchain_anthropic import ChatAnthropic
        except ImportError:
            raise ImportError(
                "使用 Claude 需要安装 langchain-anthropic: "
                "pip install langchain-anthropic"
            )
        return ChatAnthropic(
            model=settings.CLAUDE_MODEL,
            api_key=settings.ANTHROPIC_API_KEY,
            temperature=temperature,
            max_tokens=4096,
        )
    elif settings.LLM_PROVIDER == "deepseek":
        from langchain_openai import ChatOpenAI
        return ChatOpenAI(
            model=settings.DEEPSEEK_MODEL,
            api_key=settings.DEEPSEEK_API_KEY,
            base_url="https://api.deepseek.com/v1",
            temperature=temperature,
            max_tokens=4096,
        )
    else:
        raise ValueError(f"Unsupported LLM provider: {settings.LLM_PROVIDER}")


def generate_structured_json(system_prompt: str, user_prompt: str, temperature: float = 0.7) -> dict:
    """
    调用 LLM 并强制返回 JSON 结构。
    使用 JSON mode 或在 prompt 中明确要求。
    """
    llm = get_llm(temperature)

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_prompt),
    ]

    # 使用 StrOutputParser 获取文本后再解析 JSON
    parser = StrOutputParser()
    chain = llm | parser
    response_text = chain.invoke(messages)

    # 清理可能的 markdown 代码块标记
    text = response_text.strip()
    if text.startswith("```json"):
        text = text[7:]
    elif text.startswith("```"):
        text = text[3:]
    if text.endswith("```"):
        text = text[:-3]

    try:
        return json.loads(text.strip())
    except json.JSONDecodeError:
        # 重试：更严格地要求 JSON
        retry_messages = messages + [
            HumanMessage(content="Please respond with ONLY valid JSON, no markdown formatting, no explanations. The previous response was not valid JSON.")
        ]
        retry_response = chain.invoke(retry_messages)
        text2 = retry_response.strip()
        if text2.startswith("```"):
            text2 = text2.split("\n", 1)[1].rsplit("\n```", 1)[0]
        return json.loads(text2.strip())
