import { useEffect, useState } from 'react';
import { getHoldings, getPortfolioDetail, getPortfolios, type Holding } from '../services/api';

export default function PortfolioPage() {
  const [title, setTitle] = useState('Portfolio');
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const ps = await getPortfolios();
        const primary = ps[0];
        if (!primary) return;
        const detail = await getPortfolioDetail(primary.portfolioId);
        setTitle(detail.name);
        setHoldings(await getHoldings(primary.portfolioId));
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Load failed');
      }
    })();
  }, []);

  return (
    <div>
      <h2>{title}</h2>
      {error && <p className="error">{error}</p>}
      <table className="card table">
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Qty</th>
            <th>Avg</th>
            <th>Sector</th>
          </tr>
        </thead>
        <tbody>
          {holdings.map((h) => (
            <tr key={h.symbol}>
              <td>{h.symbol}</td>
              <td>{h.quantity}</td>
              <td>₹{h.avgPrice}</td>
              <td>{h.sector ?? '—'}</td>
            </tr>
          ))}
          {!holdings.length && (
            <tr>
              <td colSpan={5} className="muted">
                No holdings yet — place a simulated BUY order.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}