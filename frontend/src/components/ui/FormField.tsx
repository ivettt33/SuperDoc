import React from "react";

interface FormFieldProps {
  label: string;
  name?: string;
  required?: boolean;
  error?: string;
  children: React.ReactNode;
  className?: string;
  helpText?: string;
}

export default function FormField({
  label,
  name,
  required = false,
  error,
  children,
  className = "",
  helpText,
}: FormFieldProps) {
  return (
    <div className={className}>
      <label
        htmlFor={name}
        className="block text-sm font-medium text-gray-300 mb-2"
      >
        {label} {required && <span className="text-red-400">*</span>}
      </label>
      {children}
      {error && (
        <p className="text-red-400 text-sm mt-1 animate-in slide-in-from-top duration-300">
          {error}
        </p>
      )}
      {helpText && !error && (
        <p className="text-gray-500 text-sm mt-1">{helpText}</p>
      )}
    </div>
  );
}

