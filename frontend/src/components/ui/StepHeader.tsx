import React from "react";

interface StepHeaderProps {
  title: string;
  description: string;
}

export default function StepHeader({ title, description }: StepHeaderProps) {
  return (
    <div className="text-center mb-6">
      <h2 className="text-2xl font-semibold text-white mb-2 animate-in slide-in-from-top duration-500">
        {title}
      </h2>
      <p className="text-gray-400 animate-in slide-in-from-top duration-700">
        {description}
      </p>
    </div>
  );
}

