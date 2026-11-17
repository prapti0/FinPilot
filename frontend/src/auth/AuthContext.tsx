import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { login as apiLogin, type AuthResponse } from '../services/api';

type AuthState = {
  token: string | null;
  user: Omit<AuthResponse, 'accessToken'> | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  setSession: (res: AuthResponse) => void;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('finpilot_token'));
  const [user, setUser] = useState<Omit<AuthResponse, 'accessToken'> | null>(() => {
    const raw = localStorage.getItem('finpilot_user');
    if (!raw?.trim()) return null;
    try {
      return JSON.parse(raw) as Omit<AuthResponse, 'accessToken'>;
    } catch {
      localStorage.removeItem('finpilot_user');
      return null;
    }
  });

  const value = useMemo<AuthState>(
    () => ({
      token,
      user,
      async login(email, password) {
        const res = await apiLogin(email, password);
        localStorage.setItem('finpilot_token', res.accessToken);
        const { accessToken: _t, ...rest } = res;
        localStorage.setItem('finpilot_user', JSON.stringify(rest));
        setToken(res.accessToken);
        setUser(rest);
      },
      logout() {
        localStorage.removeItem('finpilot_token');
        localStorage.removeItem('finpilot_user');
        setToken(null);
        setUser(null);
      },
      setSession(res) {
        localStorage.setItem('finpilot_token', res.accessToken);
        const { accessToken: _t, ...rest } = res;
        localStorage.setItem('finpilot_user', JSON.stringify(rest));
        setToken(res.accessToken);
        setUser(rest);
      },
    }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('AuthProvider missing');
  return ctx;
}