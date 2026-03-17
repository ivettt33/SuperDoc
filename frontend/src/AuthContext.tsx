import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { AuthApi, JwtResponse, LoginRequest, RegisterRequest } from "./api";
import API from "./api";

type User = { 
  id?: number;
  email: string; 
  role: "DOCTOR" | "PATIENT";
  onboardingRequired?: boolean;
} | null;

type AuthContextValue = {
  user: User;
  token: string | null;
  isAuthenticated: boolean;
  login: (body: LoginRequest) => Promise<JwtResponse>;
  register: (body: RegisterRequest) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  completeOnboarding: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("token"));
  const [user, setUser] = useState<User>(() => {
    const storedUser = localStorage.getItem("user");
    return storedUser ? JSON.parse(storedUser) : null;
  });

  // Restore user data from token if user is null but token exists (handles refresh scenarios)
  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }
    
    // If user is already set, don't restore
    if (user) {
      return;
    }
    
    // Try to restore from localStorage first
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser);
        setUser(parsedUser);
        return;
      } catch (e) {
        // Invalid stored data, clear it
        localStorage.removeItem("user");
      }
    }
    
    // If no stored user data, try to decode JWT token to get basic info
    try {
      const tokenParts = token.split('.');
      if (tokenParts.length === 3) {
        const payload = JSON.parse(atob(tokenParts[1]));
        const role = payload.role as "DOCTOR" | "PATIENT";
        const email = payload.sub as string;
        // We can't determine onboardingRequired from token alone,
        // so we'll default to true to be safe (will be corrected on next login)
        const fallbackUser = {
          email,
          role,
          onboardingRequired: true
        };
        setUser(fallbackUser);
        localStorage.setItem("user", JSON.stringify(fallbackUser));
      }
    } catch (e) {
      // Token is invalid or can't be decoded
      setToken(null);
      setUser(null);
      localStorage.removeItem("token");
      localStorage.removeItem("user");
    }
  }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

  const refreshUser = useCallback(async () => {
    if (!token) {
      setUser(null);
      return;
    }
    
    // If user data exists in localStorage, use it
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser);
        setUser(parsedUser);
        return;
      } catch (e) {
        localStorage.removeItem("user");
      }
    }
    
    // Decode token as fallback
    try {
      const tokenParts = token.split('.');
      if (tokenParts.length === 3) {
        const payload = JSON.parse(atob(tokenParts[1]));
        const role = payload.role as "DOCTOR" | "PATIENT";
        const email = payload.sub as string;
        const fallbackUser = {
          email,
          role,
          onboardingRequired: true
        };
        setUser(fallbackUser);
        localStorage.setItem("user", JSON.stringify(fallbackUser));
      }
    } catch (e) {
      setToken(null);
      setUser(null);
      localStorage.removeItem("token");
      localStorage.removeItem("user");
    }
  }, [token]);
  
  useEffect(() => {
    if (!token) {
      setUser(null);
      localStorage.removeItem("user");
      return;
    }
    localStorage.setItem("token", token);
    // If user data exists but token changed, keep user data in sync
    if (user && !localStorage.getItem("user")) {
      localStorage.setItem("user", JSON.stringify(user));
    }
  }, [token, user]);

  const login = useCallback(async (body: LoginRequest): Promise<JwtResponse> => {
    const data: JwtResponse = await AuthApi.login(body);
    setToken(data.token);
    // Set user data from login response
    const userData = {
      id: 0,
      email: data.email,
      role: data.role,
      onboardingRequired: data.onboardingRequired
    };
    setUser(userData);
    // Persist user data to localStorage
    localStorage.setItem("user", JSON.stringify(userData));
    return data;
  }, []);

  const register = useCallback(async (body: RegisterRequest) => {
    await AuthApi.register(body);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  }, []);

  const completeOnboarding = useCallback(() => {
    if (user) {
      const updatedUser = {
        ...user,
        onboardingRequired: false
      };
      setUser(updatedUser);
      localStorage.setItem("user", JSON.stringify(updatedUser));
    }
  }, [user]);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    isAuthenticated: Boolean(token),
    login,
    register,
    logout,
    refreshUser,
    completeOnboarding,
  }), [user, token, login, register, logout, refreshUser, completeOnboarding]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

