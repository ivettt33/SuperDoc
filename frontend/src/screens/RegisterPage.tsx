import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Select from "../components/ui/select";
import AuthLayout from "../components/auth/AuthLayout";
import SubmitButton from "../components/auth/SubmitButton";
import FormDivider from "../components/auth/FormDivider";
import GoogleAuthButton from "../components/auth/GoogleAuthButton";
import { validators } from "../utils/validation";

type Role = "DOCTOR" | "PATIENT";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [role, setRole] = useState<Role>("PATIENT");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const validateForm = (): boolean => {
    const emailError = validators.email(email);
    const passwordError = validators.password(password);
    const matchError = validators.passwordMatch(password, confirm);
    
    if (emailError) {
      setError(emailError);
      return false;
    }
    if (passwordError) {
      setError(passwordError);
      return false;
    }
    if (matchError) {
      setError(matchError);
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
      await register({ email, password, role });
      navigate("/login", { replace: true });
    } catch (e: any) {
      const errorData = e?.response?.data;
      const errorMessage = errorData?.message || (typeof errorData === 'string' ? errorData : "Registration failed.");
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout variant="register">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">Create an account</h1>
        <p className="text-gray-400">
          Already have an account?{" "}
          <Link to="/login" className="link">
            Sign in
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
        
        <div>
          <Input
            id="confirm"
            type="password"
            className="input-field"
            placeholder="Confirm your password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
          />
        </div>
        
        <div>
          <Select
            id="role"
            className="input-field"
            value={role}
            onChange={(e) => setRole(e.target.value as Role)}
          >
            <option value="PATIENT">Patient</option>
            <option value="DOCTOR">Doctor</option>
          </Select>
        </div>
        
        <div className="flex items-center">
          <input
            id="terms"
            type="checkbox"
            className="w-4 h-4 text-blue-600 bg-gray-800 border-gray-600 rounded focus:ring-blue-500 focus:ring-2"
            required
          />
          <label htmlFor="terms" className="ml-2 text-sm text-gray-300">
            I agree to the Terms & Conditions
          </label>
        </div>
        
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}
        
        <SubmitButton loading={loading} loadingText="Creating account...">
          Create account
        </SubmitButton>
        
        <FormDivider text="Or register with" />
        
        <GoogleAuthButton
          onClick={() => {
            alert("Google registration integration coming soon! Please use the form above for now.");
          }}
        />
      </form>
    </AuthLayout>
  );
}


