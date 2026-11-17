import { useEffect, useState } from 'react';
import { getPortfolios, getQuote, placeOrder } from '../services/api';

export default function TradingPage() {
  const [portfolioId, setPortfolioId] = useState<number | null>(null);
  const [symbol, setSymbol] = useState('RELIANCE');
  const [qty, setQty] = useState(10);
  const [price, setPrice] = useState(1000);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      const ps = await getPortfolios();
      if (ps[0]) setPortfolioId(ps[0].portfolioId);
      const q = await getQuote(symbol);
      setPrice(q.price);
    })().catch(() => undefined);
  }, [symbol]);

  async function buy() {
    if (!portfolioId) return;
    setError('');
    setMsg('');
    try {
      const key = crypto.randomUUID();
      const order = await placeOrder(portfolioId, symbol, 'BUY', qty, price, key);
      setMsg(`Order ${order.id}: ${order.status}${order.rejectionReason ? ` (${order.rejectionReason})` : ''}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Order failed');
    }
  }

  return (
    <div>
      <h2>Simulated trade</h2>
      <div className="card trade-card">
        <label>
          Symbol
          <select value={symbol} onChange={(e) => setSymbol(e.target.value)}>
            <option value="RELIANCE">RELIANCE</option>
            <option value="HDFCBANK">HDFCBANK</option>
            <option value="NIFTYBEES">NIFTYBEES</option>
          </select>
        </label>
        <label>
          Quantity
          <input type="number" value={qty} min={1} onChange={(e) => setQty(Number(e.target.value))} />
        </label>
        <label>
          Limit price (₹)
          <input type="number" value={price} min={1} onChange={(e) => setPrice(Number(e.target.value))} />
        </label>
        <button type="button" onClick={buy}>
          Buy
        </button>
        {msg && <p className="success">{msg}</p>}
        {error && <p className="error">{error}</p>}
      </div>
    </div>
  );
}