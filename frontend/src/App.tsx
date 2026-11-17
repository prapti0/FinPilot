import { BrowserRouter, NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import Dashboard from './dashboard/Dashboard';
import PortfolioPage from './portfolio/PortfolioPage';
import TradingPage from './trading/TradingPage';
import LoginPage from './pages/LoginPage';
import AiAnalystPage from './pages/AiAnalystPage';
import EngineeringPage from './pages/EngineeringPage';
import MonitoringPage from './pages/MonitoringPage';

function Shell() {
  const { token, user, logout } = useAuth();

  if (!token) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1 className="brand">FinPilot</h1>
        <p className="tagline">{user?.name}</p>
        <nav>
          <NavLink to="/">Dashboard</NavLink>
          <NavLink to="/portfolio">Portfolio</NavLink>
          <NavLink to="/trading">Trading</NavLink>
          <NavLink to="/ai">AI Analyst</NavLink>
          <NavLink to="/engineering">Eng. Agent</NavLink>
          <NavLink to="/monitoring">Monitoring</NavLink>
        </nav>
        <button type="button" className="logout" onClick={logout}>
          Logout
        </button>
      </aside>
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/portfolio" element={<PortfolioPage />} />
          <Route path="/trading" element={<TradingPage />} />
          <Route path="/ai" element={<AiAnalystPage />} />
          <Route path="/engineering" element={<EngineeringPage />} />
          <Route path="/monitoring" element={<MonitoringPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Shell />
      </BrowserRouter>
    </AuthProvider>
  );
}