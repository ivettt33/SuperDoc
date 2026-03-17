import React from "react";

interface NavigationButtonsProps {
  onPrev?: () => void;
  onNext?: () => void;
  onSubmit?: () => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  loading?: boolean;
  prevLabel?: string;
  nextLabel?: string;
  submitLabel?: string;
  showPrev?: boolean;
}

export default function NavigationButtons({
  onPrev,
  onNext,
  onSubmit,
  isFirstStep,
  isLastStep,
  loading = false,
  prevLabel = "← Previous",
  nextLabel = "Next Step →",
  submitLabel = "Complete Profile ✓",
  showPrev = true,
}: NavigationButtonsProps) {
  const handleAction = () => {
    if (isLastStep && onSubmit) {
      onSubmit();
    } else if (onNext) {
      onNext();
    }
  };

  const actionLabel = loading
    ? "Saving..."
    : isLastStep
    ? submitLabel
    : nextLabel;

  return (
    <div className="flex justify-between pt-4 animate-in fade-in duration-700">
      {showPrev && !isFirstStep && (
        <button
          type="button"
          onClick={onPrev}
          disabled={loading}
          className="btn-secondary transform transition-all duration-200 hover:scale-105 active:scale-95"
        >
          {prevLabel}
        </button>
      )}
      <div className={showPrev && !isFirstStep ? "" : "ml-auto"}>
        <button
          type="button"
          onClick={handleAction}
          disabled={loading}
          className="btn-primary transform transition-all duration-200 hover:scale-105 active:scale-95 shadow-lg hover:shadow-blue-500/50 disabled:hover:shadow-none"
        >
          {loading ? (
            <>
              <svg
                className="animate-spin -ml-1 mr-3 h-5 w-5 text-white inline"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                ></circle>
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                ></path>
              </svg>
              {actionLabel}
            </>
          ) : (
            actionLabel
          )}
        </button>
      </div>
    </div>
  );
}

