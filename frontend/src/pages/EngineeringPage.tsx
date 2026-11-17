import { FormEvent, useState } from 'react';
import { engineeringReview } from '../services/api';

export default function EngineeringPage() {
  const [title, setTitle] = useState('feat: add reserve saga');
  const [diff, setDiff] = useState('+ if (reserved < amount) reject;\n+ releaseOnFailure(orderId);');
  const [repo, setRepo] = useState('finpilot/order-service');
  const [summary, setSummary] = useState('');
  const [findings, setFindings] = useState<{ severity: string; title: string; detail: string }[]>([]);

  async function onReview(e: FormEvent) {
    e.preventDefault();
    const res = await engineeringReview(title, diff, repo);
    setSummary(res.summary);
    setFindings(res.findings);
  }

  return (
    <div>
      <h2>AI Engineering Agent</h2>
      <p className="muted">Sandboxed review — no auto-merge.</p>
      <form className="card" onSubmit={onReview}>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="PR title" />
        <textarea rows={5} value={diff} onChange={(e) => setDiff(e.target.value)} />
        <input value={repo} onChange={(e) => setRepo(e.target.value)} placeholder="repo" />
        <button type="submit">Run review</button>
      </form>
      {summary && <p className="card">{summary}</p>}
      <ul className="findings">
        {findings.map((f, i) => (
          <li key={i}>
            <strong>{f.severity}</strong> {f.title}: {f.detail}
          </li>
        ))}
      </ul>
    </div>
  );
}