import { useEffect, useState } from 'react';
import { serviceHealth } from '../services/api';

export default function MonitoringPage() {
  const [rows, setRows] = useState<{ name: string; status: string }[]>([]);

  useEffect(() => {
    serviceHealth().then(setRows);
    const id = setInterval(() => serviceHealth().then(setRows), 10000);
    return () => clearInterval(id);
  }, []);

  return (
    <div>
      <h2>Observability</h2>
      <p className="muted">
        Prometheus metrics: <code>/actuator/prometheus</code> on each service · Grafana optional in Docker
      </p>
      <table className="card table">
        <thead>
          <tr>
            <th>Service</th>
            <th>Health</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.name}>
              <td>{r.name}</td>
              <td className={r.status === 'UP' ? 'up' : 'down'}>{r.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}