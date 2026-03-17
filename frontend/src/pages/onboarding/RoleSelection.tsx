import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../../api";

export default function RoleSelection() {
  const navigate = useNavigate();
  const [selectedRole, setSelectedRole] = useState<"DOCTOR" | "PATIENT" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRole) {
      setError("Please select a role");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await API.post("/onboarding/role", { role: selectedRole });
      localStorage.setItem("onboardingRole", selectedRole);
      navigate("/onboarding/details");
    } catch (error) {
      console.error("Failed to update role:", error);
      setError("Failed to update role. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center p-4">
      <div className="card max-w-2xl w-full">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">Choose Your Role</h1>
          <p className="text-gray-400">Tell us how you'll be using SuperDoc</p>
        </div>

        <form onSubmit={onSubmit} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Doctor Option */}
            <label className="relative cursor-pointer">
              <input
                type="radio"
                value="DOCTOR"
                checked={selectedRole === "DOCTOR"}
                onChange={(e) => setSelectedRole(e.target.value as "DOCTOR")}
                className="sr-only"
              />
              <div className={`card transition-colors duration-200 border-2 ${
                selectedRole === "DOCTOR" 
                  ? "border-blue-500 bg-blue-50/10" 
                  : "border-gray-700 hover:border-blue-400"
              }`}>
                <div className="text-center">
                  <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <h3 className="text-xl font-semibold text-white mb-2">Doctor</h3>
                  <p className="text-gray-400 text-sm">
                    Provide medical care, manage patients, and access medical tools
                  </p>
                </div>
              </div>
            </label>

            {/* Patient Option */}
            <label className="relative cursor-pointer">
              <input
                type="radio"
                value="PATIENT"
                checked={selectedRole === "PATIENT"}
                onChange={(e) => setSelectedRole(e.target.value as "PATIENT")}
                className="sr-only"
              />
              <div className={`card transition-colors duration-200 border-2 ${
                selectedRole === "PATIENT" 
                  ? "border-blue-500 bg-blue-50/10" 
                  : "border-gray-700 hover:border-blue-400"
              }`}>
                <div className="text-center">
                  <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                  </div>
                  <h3 className="text-xl font-semibold text-white mb-2">Patient</h3>
                  <p className="text-gray-400 text-sm">
                    Access medical care, book appointments, and manage your health
                  </p>
                </div>
              </div>
            </label>
          </div>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? "Updating..." : "Continue"}
          </button>
        </form>
      </div>
    </div>
  );
}
