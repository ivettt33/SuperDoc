import React from "react";
import ProgressBar from "./ProgressBar";

interface Step {
  id: number;
  title: string;
  component: React.ComponentType<any>;
}

interface OnboardingLayoutProps {
  title: string;
  description: string;
  iconColor: "blue" | "green";
  currentStep: number;
  steps: Step[];
  error: string | null;
  children: React.ReactNode;
}

export default function OnboardingLayout({
  title,
  description,
  iconColor,
  currentStep,
  steps,
  error,
  children,
}: OnboardingLayoutProps) {
  const gradientColors =
    iconColor === "blue"
      ? "from-blue-500 to-cyan-400"
      : "from-green-500 to-emerald-400";
  const textGradient =
    iconColor === "blue"
      ? "from-blue-400 to-cyan-300"
      : "from-green-400 to-emerald-300";
  const shadowColor =
    iconColor === "blue" ? "shadow-blue-500/50" : "shadow-green-500/50";

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 flex items-center justify-center p-4">
      <div className="max-w-4xl w-full">
        {/* Header with animated gradient */}
        <div className="text-center mb-8 animate-in fade-in slide-in-from-top duration-700">
          <div className="inline-block mb-4">
            <div
              className={`w-16 h-16 bg-gradient-to-br ${gradientColors} rounded-2xl flex items-center justify-center shadow-lg ${shadowColor} animate-in zoom-in duration-500`}
            >
              <svg
                className="w-8 h-8 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d={
                    iconColor === "blue"
                      ? "M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                      : "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                  }
                />
              </svg>
            </div>
          </div>
          <h1
            className={`text-4xl font-bold text-white mb-3 bg-gradient-to-r ${textGradient} bg-clip-text text-transparent`}
          >
            {title}
          </h1>
          <p className="text-gray-400 text-lg">{description}</p>
        </div>

        {/* Main card with glass morphism effect */}
        <div className="bg-gray-800/50 backdrop-blur-xl rounded-2xl shadow-2xl p-8 border border-gray-700/50 animate-in fade-in zoom-in duration-500">
          <ProgressBar
            currentStep={currentStep}
            totalSteps={steps.length}
            steps={steps}
          />

          {error && (
            <div className="error-message mb-6 animate-in shake duration-500">
              {error}
            </div>
          )}

          <div className="mt-8">{children}</div>
        </div>

        {/* Footer */}
        <div className="text-center mt-6 animate-in fade-in duration-1000">
          <p className="text-gray-500 text-sm">
            Need help?{" "}
            <span
              className={`${
                iconColor === "blue"
                  ? "text-blue-400 hover:text-blue-300"
                  : "text-green-400 hover:text-green-300"
              } transition-colors cursor-pointer`}
            >
              Contact Support
            </span>
          </p>
        </div>
      </div>
    </div>
  );
}

