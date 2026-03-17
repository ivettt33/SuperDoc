import React from "react";
import Logo from "../Logo";

interface AuthLayoutProps {
  children: React.ReactNode;
  variant?: "login" | "register";
  mobileTitle?: string;
}

export default function AuthLayout({ children, variant = "login", mobileTitle }: AuthLayoutProps) {
  const gradientClass = variant === "login" ? "gradient-bg" : "gradient-bg-register";

  return (
    <div className="min-h-screen flex">
      {/* Left Panel - Visual Section */}
      <div className={`hidden lg:flex lg:w-1/2 ${gradientClass} relative overflow-hidden`}>
        <div className="absolute inset-0 bg-black/20"></div>
        <div className="relative z-10 flex flex-col justify-between p-8 w-full">
          {/* Logo */}
          <div className="flex items-center">
            <Logo className="w-8 h-8 mr-3" />
            <div className="text-2xl font-bold text-white">SuperDoc</div>
          </div>
          
          {/* Tagline */}
          <div className="text-white">
            <h2 className="text-2xl font-light leading-relaxed">
              Your Health Journey<br />
              Starts Here
            </h2>
          </div>
        </div>
      </div>
      
      {/* Right Panel - Form Section */}
      <div className="w-full lg:w-1/2 bg-gray-900 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Logo for mobile */}
          <div className="flex items-center mb-8 lg:hidden">
            <Logo className="w-6 h-6 mr-2" />
            <div className="text-xl font-bold text-white">{mobileTitle || "SuperDoc"}</div>
          </div>
          
          {children}
        </div>
      </div>
    </div>
  );
}

