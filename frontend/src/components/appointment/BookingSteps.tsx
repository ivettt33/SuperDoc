import React from "react";
import { HiCheck } from "react-icons/hi";

interface BookingStepsProps {
  currentStep: number;
  steps: string[];
}

export default function BookingSteps({ currentStep, steps }: BookingStepsProps) {
  return (
    <div className="mb-8">
      <div className="flex items-center justify-between">
        {steps.map((step, index) => {
          const stepNumber = index + 1;
          const isCompleted = stepNumber < currentStep;
          const isCurrent = stepNumber === currentStep;
          const isUpcoming = stepNumber > currentStep;

          return (
            <React.Fragment key={step}>
              <div className="flex flex-col items-center flex-1">
                <div className="flex items-center w-full">
                  {/* Step Circle */}
                  <div
                    className={`
                      w-10 h-10 rounded-full flex items-center justify-center font-semibold transition-all duration-200
                      ${isCompleted
                        ? "bg-blue-600 text-white"
                        : isCurrent
                        ? "bg-blue-600 text-white ring-4 ring-blue-500/30"
                        : "bg-gray-700 text-gray-400"
                      }
                    `}
                  >
                    {isCompleted ? (
                      <HiCheck className="w-6 h-6" />
                    ) : (
                      <span>{stepNumber}</span>
                    )}
                  </div>
                  {/* Step Label */}
                  <div className="ml-3 flex-1">
                    <p
                      className={`
                        text-sm font-medium
                        ${isCurrent ? "text-white" : isCompleted ? "text-gray-300" : "text-gray-500"}
                      `}
                    >
                      {step}
                    </p>
                  </div>
                </div>
              </div>
              {/* Connector Line */}
              {index < steps.length - 1 && (
                <div
                  className={`
                    h-0.5 flex-1 mx-2 transition-colors duration-200
                    ${isCompleted ? "bg-blue-600" : "bg-gray-700"}
                  `}
                />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}




