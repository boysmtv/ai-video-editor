from __future__ import annotations

import os
from typing import Any
from typing_extensions import TypedDict

from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, Field


class HookStrategy(BaseModel):
    hook: str = Field(description="Short platform-appropriate hook text.")
    strategy_notes: list[str] = Field(default_factory=list)


def build_plan_artifacts(
    pipeline: Any,
    mode: str,
    variant_index: int,
    content_type: str,
    niche: str,
    analyses: list[Any],
    memory: Any,
    settings: Any,
    override: Any,
    target_duration: float,
    candidates: list[Any],
) -> dict[str, Any]:
    llm_enabled = bool(settings.langchain_llm_enabled and settings.langchain_model and os.getenv("OPENAI_API_KEY"))

    class WorkflowState(TypedDict, total=False):
        mode: str
        variant_index: int
        content_type: str
        niche: str
        analyses: list[Any]
        memory: Any
        settings: Any
        override: Any
        target_duration: float
        candidates: list[Any]
        ranked_candidates: list[Any]
        selected_segments: list[Any]
        hook: str
        strategy_notes: list[str]
        orchestrator_metadata: dict[str, Any]

    def rank_node(state: WorkflowState) -> dict[str, Any]:
        ranked = pipeline._rank_candidates_for_mode(
            state["candidates"], state["mode"], state["variant_index"], state["override"]
        )
        return {
            "ranked_candidates": ranked,
            "orchestrator_metadata": {
                "enabled": True,
                "backend": "langgraph",
                "llm_enabled": llm_enabled,
            },
        }

    def select_node(state: WorkflowState) -> dict[str, Any]:
        selected = pipeline._select_segments(
            state["ranked_candidates"],
            state["target_duration"],
            state["mode"],
            state["override"],
        )
        return {"selected_segments": selected}

    def hook_node(state: WorkflowState) -> dict[str, Any]:
        hook = pipeline._generate_hook(
            state["mode"],
            state["variant_index"],
            state["niche"],
            state["analyses"],
            state["memory"],
            state["selected_segments"],
            state["settings"],
            state["override"],
        )
        notes = pipeline._strategy_notes(
            state["mode"], state["variant_index"], state["niche"], state["selected_segments"]
        )
        return {"hook": hook, "strategy_notes": notes}

    def llm_node(state: WorkflowState) -> dict[str, Any]:
        if not llm_enabled or not state["selected_segments"]:
            return {}
        try:
            from langchain.chat_models import init_chat_model

            model = init_chat_model(state["settings"].langchain_model)
            structured = model.with_structured_output(HookStrategy)
            transcript_snippets = [
                segment.transcript_text for segment in state["selected_segments"][:4] if segment.transcript_text
            ]
            semantic_tags = sorted(
                {
                    tag
                    for segment in state["selected_segments"][:5]
                    for tag in getattr(segment, "semantic_tags", [])
                }
            )
            prompt = (
                "You are planning a short-form video edit.\n"
                f"Platform: {state['mode']}\n"
                f"Content type: {state['content_type']}\n"
                f"Niche: {state['niche']}\n"
                f"Current hook: {state['hook']}\n"
                f"Tags: {', '.join(semantic_tags) or 'none'}\n"
                f"Transcript snippets: {' | '.join(transcript_snippets) or 'none'}\n"
                "Return a stronger but natural hook plus 2-4 concise strategy notes. "
                "Keep it soft-sell for affiliate content unless the input clearly indicates otherwise."
            )
            result = structured.invoke(prompt)
            if state["override"].banned_phrases:
                hook = pipeline._sanitize_hook_text(result.hook, state["override"])
            else:
                hook = result.hook
            metadata = dict(state["orchestrator_metadata"])
            metadata["llm_refined"] = True
            return {
                "hook": hook,
                "strategy_notes": list(dict.fromkeys(state["strategy_notes"] + result.strategy_notes)),
                "orchestrator_metadata": metadata,
            }
        except Exception as exc:
            metadata = dict(state["orchestrator_metadata"])
            metadata["llm_refined"] = False
            metadata["llm_error"] = exc.__class__.__name__
            return {"orchestrator_metadata": metadata}

    graph = StateGraph(WorkflowState)
    graph.add_node("rank", rank_node)
    graph.add_node("select", select_node)
    graph.add_node("hook", hook_node)
    graph.add_node("llm", llm_node)
    graph.add_edge(START, "rank")
    graph.add_edge("rank", "select")
    graph.add_edge("select", "hook")
    graph.add_edge("hook", "llm")
    graph.add_edge("llm", END)
    app = graph.compile()

    initial_state: WorkflowState = {
        "mode": mode,
        "variant_index": variant_index,
        "content_type": content_type,
        "niche": niche,
        "analyses": analyses,
        "memory": memory,
        "settings": settings,
        "override": override,
        "target_duration": target_duration,
        "candidates": candidates,
    }
    result = app.invoke(initial_state)
    return {
        "ranked_candidates": result["ranked_candidates"],
        "selected_segments": result["selected_segments"],
        "hook": result["hook"],
        "strategy_notes": result["strategy_notes"],
        "orchestrator_metadata": result["orchestrator_metadata"],
    }
