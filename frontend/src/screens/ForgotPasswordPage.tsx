import React, { useState } from "react";
import { AuthApi } from "../api";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import SubmitButton from "../components/auth/SubmitButton";
import { validators } from "../utils/validation";


export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const validateForm = (): boolean => {
    const emailError = validators.email(email);
    if (emailError) {
      setError(emailError);
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
      await AuthApi.forgotPassword({ email });
      setSent(true);
    } catch (e: any) {
      const errorData = e?.response?.data;
      const errorMessage = errorData?.message || (typeof errorData === 'string' ? errorData : "Something went wrong. Try again.");
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-8 bg-gray-900">
      <div className="w-full max-w-md">
        <h1 className="text-2xl font-bold text-white mb-2">Forgot password</h1>
        <p className="text-gray-400 mb-6">Enter your email to receive a reset link.</p>
        {sent ? (
          <div className="text-green-400">If that email exists, a reset link was sent.</div>
        ) : (
          <form onSubmit={onSubmit} className="space-y-4">
            <Input
              type="email"
              className="input-field"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            {error && <div className="error-message">{error}</div>}
            <SubmitButton loading={loading} loadingText="Sending...">
              Send reset link
            </SubmitButton>
          </form>
        )}
      </div>
    </div>
  );
}


