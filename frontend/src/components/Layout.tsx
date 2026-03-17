import React, { useState, useEffect } from "react";
import Sidebar from "./Sidebar";
import ChatBot from "./ChatBot";
import { MdMedicalServices } from "react-icons/md";

interface LayoutProps {
  children: React.ReactNode;
  currentPage?: string;
}

export default function Layout({ children, currentPage = "dashboard" }: LayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const checkMobile = () => setIsMobile(window.innerWidth < 768);
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  useEffect(() => {
    if (!isMobile) {
      setSidebarOpen(true);
    }
  }, [isMobile]);

  const toggleSidebar = () => setSidebarOpen(!sidebarOpen);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
      <Sidebar isOpen={sidebarOpen} onToggle={toggleSidebar} currentPage={currentPage} />
      
      <div className={`transition-all duration-300 ${sidebarOpen ? 'md:ml-64' : ''}`}>
        <nav className="bg-gray-800/50 backdrop-blur-xl border-b border-gray-700/50 sticky top-0 z-30">
          <div className="flex items-center justify-between px-3 py-2">
            <button
              onClick={toggleSidebar}
              className="text-gray-400 hover:text-white transition-colors p-1.5 hover:bg-gray-700 rounded-lg"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>

            <div className="flex items-center space-x-2">
              <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-lg flex items-center justify-center">
                <MdMedicalServices className="w-4 h-4 text-white" />
              </div>
              <span className="text-gray-300 font-semibold text-sm">SuperDoc</span>
            </div>
          </div>
        </nav>

        <main>{children}</main>
      </div>
      <ChatBot />
    </div>
  );
}
