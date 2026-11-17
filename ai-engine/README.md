# AI engine modules

- **financial-analyst/** — Portfolio Q&A orchestration (structured data + Gemini)
- **engineering-agent/** — PR review, test suggestions (sandboxed, human approval)
- **rag/** — Document chunking, pgvector retrieval
- **prompts/** — Versioned prompt templates
- **evaluation/** — Golden-set scoring for analyst responses

AI services run in `backend/ai-service` and call modules here as the implementation grows.