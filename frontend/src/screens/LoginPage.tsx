import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import AuthLayout from "../components/auth/AuthLayout";
import SubmitButton from "../components/auth/SubmitButton";
import FormDivider from "../components/auth/FormDivider";
import GoogleAuthButton from "../components/auth/GoogleAuthButton";
import { validators } from "../utils/validation";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const validateForm = (): boolean => {
    const emailError = validators.email(email);
    const passwordError = validators.password(password);
    
    if (emailError) {
      setError(emailError);
      return false;
    }
    if (passwordError) {
      setError(passwordError);
      return false;
    }
    return true;
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    if (!validateForm()) return;
    
    try {
      setLoading(true);
      const response = await login({ email, password });
      // Check if onboarding is required and redirect accordingly
      if (response?.onboardingRequired) {
        if (response.role === "DOCTOR") {
          navigate("/onboarding/doctor", { replace: true });
        } else if (response.role === "PATIENT") {
          navigate("/onboarding/patient", { replace: true });
        } else {
          navigate("/", { replace: true });
        }
      } else {
        navigate("/", { replace: true });
      }
    } catch (e: any) {
      const errorData = e?.response?.data;
      const errorMessage = errorData?.message || (typeof errorData === 'string' ? errorData : "Login failed. Check credentials.");
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout variant="login">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">Sign in</h1>
        <p className="text-gray-400">
          Don't have an account?{" "}
          <Link to="/register" className="link">
            Sign up
          </Link>
        </p>
      </div>
      
      <form onSubmit={onSubmit} className="space-y-6">
        <div>
          <Input
            id="email"
            type="email"
            className="input-field"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        
        <div>
          <Input
            id="password"
            type="password"
            className="input-field"
            placeholder="Enter your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}
        
        <SubmitButton loading={loading} loadingText="Signing in...">
          Sign in
        </SubmitButton>
        
        <FormDivider text="Or sign in with" />
        
        <GoogleAuthButton
          onClick={() => {
            alert("Google login integration coming soon! Please use email/password for now.");
          }}
        />
      </form>
      
      <div className="mt-4 text-center text-sm text-gray-300">
        <Link to="/forgot-password" className="underline underline-offset-2">
          Forgot your password?
        </Link>
      </div>
    </AuthLayout>
  );
}


