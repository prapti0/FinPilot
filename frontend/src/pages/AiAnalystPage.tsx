import { FormEvent, useEffect, useState } from 'react';
import { analyzePortfolio, getPortfolios } from '../services/api';

export default function AiAnalystPage() {
  const [portfolioId, setPortfolioId] = useState<number | null>(null);
  const [question, setQuestion] = useState('Where is my risk exposure high?');
  const [narrative, setNarrative] = useState('');
  const [meta, setMeta] = useState('');

  useEffect(() => {
    getPortfolios().then((p) => p[0] && setPortfolioId(p[0].portfolioId));
  }, []);

  async function onAsk(e: FormEvent) {
    e.preventDefault();
    if (!portfolioId) return;
    const res = await analyzePortfolio(portfolioId, question);
    setNarrative(res.narrative);
    setMeta(JSON.stringify(res.metadata, null, 2));
  }

  return (
    <div>
      <h2>AI Financial Analyst</h2>
      <p className="muted">Risk limits are enforced by Risk Service; AI explains only.</p>
      <form className="card" onSubmit={onAsk}>
        <textarea rows={3} value={question} onChange={(e) => setQuestion(e.target.value)} />
        <button type="submit">Analyze</button>
      </form>
      {meta && <pre className="card pre">{meta}</pre>}
      {narrative && <div className="card">{narrative}</div>}
    </div>
  );
}