import { useEffect, useState } from 'react';
import {
  getBalance,
  getPerformance,
  getPortfolios,
  getRiskAlerts,
  type Balance,
} from '../services/api';

export default function Dashboard() {
  const [balance, setBalance] = useState<Balance | null>(null);
  const [totalValue, setTotalValue] = useState(0);
  const [alerts, setAlerts] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const [b, p, a] = await Promise.all([getBalance(), getPortfolios(), getRiskAlerts()]);
        setBalance(b);
        setAlerts(a.length);
        if (p[0]) {
          const perf = await getPerformance(p[0].portfolioId);
          setTotalValue(Number(perf.markedValue));
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to load dashboard');
      }
    })();
  }, []);

  return (
    <div>
      <h2>Wealth overview</h2>
      <p className="muted">Live data from FinPilot APIs (simulated market).</p>
      {error && <p className="error">{error}</p>}
      <div className="card-grid">
        <div className="card">
          <div className="muted">Portfolio value</div>
          <strong>₹{totalValue.toLocaleString('en-IN')}</strong>
        </div>
        <div className="card">
          <div className="muted">Cash available</div>
          <strong>₹{Number(balance?.available ?? 0).toLocaleString('en-IN')}</strong>
        </div>
        <div className="card">
          <div className="muted">Reserved cash</div>
          <strong>₹{Number(balance?.reserved ?? 0).toLocaleString('en-IN')}</strong>
        </div>
        <div className="card">
          <div className="muted">Risk alerts</div>
          <strong>{alerts}</strong>
        </div>
      </div>
    </div>
  );
}