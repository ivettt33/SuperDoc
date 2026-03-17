import React from "react";

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  error?: boolean;
}

export default function Select({ error, className = "", children, ...props }: SelectProps) {
  return (
    <select
      {...props}
      className={`input transform transition-all duration-200 hover:scale-[1.02] focus:scale-[1.02] ${
        error ? "border-red-500 ring-2 ring-red-500/20" : ""
      } ${className}`}
    >
      {children}
    </select>
  );
}

