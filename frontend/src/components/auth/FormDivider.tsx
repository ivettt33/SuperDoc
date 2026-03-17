import React from "react";

interface FormDividerProps {
  text?: string;
}

export default function FormDivider({ text = "Or" }: FormDividerProps) {
  return (
    <div className="relative">
      <div className="absolute inset-0 flex items-center">
        <div className="w-full border-t border-gray-600"></div>
      </div>
      <div className="relative flex justify-center text-sm">
        <span className="px-2 bg-gray-900 text-gray-400">{text}</span>
      </div>
    </div>
  );
}

