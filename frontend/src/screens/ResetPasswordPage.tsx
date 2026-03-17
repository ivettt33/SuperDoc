import React, { useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { AuthApi } from "../api";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import SubmitButton from "../components/auth/SubmitButton";
import { validators } from "../utils/validation";


export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token") || "";
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const validateForm = (): boolean => {
    if (!token) {
      setError("Missing token");
      return false;
    }
    const passwordError = validators.password(password);
    const matchError = validators.passwordMatch(password, confirm);
    
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
      await AuthApi.resetPassword({ token, newPassword: password });
      navigate("/login", { replace: true });
    } catch (e: any) {
      const errorData = e?.response?.data;
      const errorMessage = errorData?.message || (typeof errorData === 'string' ? errorData : "Could not reset password");
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-8 bg-gray-900">
      <div className="w-full max-w-md">
        <h1 className="text-2xl font-bold text-white mb-2">Set a new password</h1>
        <form onSubmit={onSubmit} className="space-y-4">
          <Input
            type="password"
            className="input-field"
            placeholder="New password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <Input
            type="password"
            className="input-field"
            placeholder="Confirm password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
          />
          {error && <div className="error-message">{error}</div>}
          <SubmitButton loading={loading} loadingText="Updating...">
            Update password
          </SubmitButton>
        </form>
      </div>
    </div>
  );
}


