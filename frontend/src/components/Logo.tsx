import React from "react";

export default function Logo({ className = "w-8 h-8" }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Heart shape background */}
      <path
        d="M50 85C50 85 15 60 15 35C15 25 25 15 35 15C40 15 45 17.5 50 25C55 17.5 60 15 65 15C75 15 85 25 85 35C85 60 50 85 50 85Z"
        fill="white"
        stroke="#1e40af"
        strokeWidth="2"
      />
      
      {/* Lab coat outline */}
      <path
        d="M30 25L70 25L70 35L65 35L65 45L60 45L60 55L55 55L55 65L45 65L45 55L40 55L40 45L35 45L35 35L30 35L30 25Z"
        fill="none"
        stroke="#1e40af"
        strokeWidth="2.5"
      />
      
      {/* Lab coat lapels (purple) */}
      <path
        d="M30 25L35 30L35 35L30 35L30 25Z"
        fill="#8b5cf6"
      />
      <path
        d="M70 25L65 30L65 35L70 35L70 25Z"
        fill="#8b5cf6"
      />
      
      {/* Stethoscope */}
      <path
        d="M25 40C25 40 20 35 20 30C20 25 25 20 30 20C32 20 34 21 35 23"
        fill="none"
        stroke="#1e40af"
        strokeWidth="2"
      />
      <circle
        cx="20"
        cy="30"
        r="3"
        fill="none"
        stroke="#1e40af"
        strokeWidth="2"
      />
      <circle
        cx="35"
        cy="23"
        r="2"
        fill="none"
        stroke="#1e40af"
        strokeWidth="2"
      />
      
      {/* Stethoscope earpieces */}
      <path
        d="M25 40C25 40 30 35 30 30"
        fill="none"
        stroke="#1e40af"
        strokeWidth="2"
      />
      <path
        d="M25 40C25 40 30 35 30 30"
        fill="none"
        stroke="#1e40af"
        strokeWidth="2"
      />
    </svg>
  );
}
