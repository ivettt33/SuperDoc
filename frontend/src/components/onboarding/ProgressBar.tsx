import React from "react";

interface Step {
  id: number;
  title: string;
}

interface ProgressBarProps {
  currentStep: number;
  totalSteps: number;
  steps: Step[];
}

export default function ProgressBar({ currentStep, totalSteps, steps }: ProgressBarProps) {
  return (
    <div className="w-full mb-8">
      <div className="flex items-center justify-center">
        {steps.map((step, index) => (
          <React.Fragment key={step.id}>
            {/* Step item */}
            <div className="flex flex-col items-center relative">
              {/* Step number/checkmark */}
              <div
                className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-semibold transition-all duration-300 transform ${
                  currentStep > step.id
                    ? "bg-gradient-to-br from-blue-500 to-cyan-400 text-white scale-100 shadow-lg shadow-blue-500/50"
                    : currentStep === step.id
                    ? "bg-gradient-to-br from-blue-600 to-blue-400 text-white scale-110 shadow-xl shadow-blue-600/50"
                    : "bg-gray-700 text-gray-400 scale-90"
                }`}
              >
                {currentStep > step.id ? (
                  <svg className="w-5 h-5 animate-in zoom-in duration-300" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fillRule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                ) : (
                  <span className="transition-all duration-300">{step.id}</span>
                )}
              </div>
              
              {/* Step title */}
              <span
                className={`mt-2 text-xs md:text-sm font-medium transition-all duration-300 whitespace-nowrap text-center ${
                  currentStep >= step.id ? "text-white" : "text-gray-500"
                }`}
              >
                {step.title}
              </span>
            </div>
            
            {/* Connecting line */}
            {index < steps.length - 1 && (
              <div className="w-16 md:w-24 h-1 mx-2 md:mx-4 bg-gray-700 rounded-full overflow-hidden relative">
                <div
                  className={`h-full bg-gradient-to-r from-blue-500 to-cyan-400 transition-all duration-500 ease-out ${
                    currentStep > step.id ? "w-full" : "w-0"
                  }`}
                  style={{
                    boxShadow: currentStep > step.id ? "0 0 10px rgba(59, 130, 246, 0.5)" : "none"
                  }}
                />
              </div>
            )}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}
