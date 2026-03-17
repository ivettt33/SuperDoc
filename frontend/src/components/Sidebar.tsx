import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { DoctorApi, PatientApi, DoctorProfile, PatientProfile } from "../api";
import { getImageUrl } from "../utils/imageUtils";
import { 
  HiHome, 
  HiUser, 
  HiCalendar, 
  HiCog,
  HiX,
  HiLogout
} from "react-icons/hi";
import { MdLocalPharmacy, MdMedicalServices } from "react-icons/md";

interface SidebarProps {
  isOpen: boolean;
  onToggle: () => void;
  currentPage?: string;
}

export default function Sidebar({ isOpen, onToggle, currentPage = "dashboard" }: SidebarProps) {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [profile, setProfile] = useState<DoctorProfile | PatientProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(true);

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user) {
        setProfile(null);
        setProfileLoading(false);
        return;
      }

      try {
        setProfileLoading(true);
        if (user.role === "DOCTOR") {
          const data = await DoctorApi.getMyProfile();
          setProfile(data);
        } else if (user.role === "PATIENT") {
          const data = await PatientApi.getMyProfile();
          setProfile(data);
        }
      } catch (error) {
        console.error("Failed to fetch profile for sidebar:", error);
        setProfile(null);
      } finally {
        setProfileLoading(false);
      }
    };

    fetchProfile();
  }, [user]);

  const menuItems = [
    { icon: HiHome, label: "Home", page: "dashboard", path: "/" },
    { icon: HiUser, label: "Account", page: "account", path: "/profile" },
    { icon: HiCalendar, label: "Appointments", page: "appointments", path: "/appointments" },
    { icon: MdLocalPharmacy, label: "Prescriptions", page: "prescriptions", path: "/prescriptions" },
    ...(user?.role === "DOCTOR" ? [{ icon: HiUser, label: "Users", page: "users", path: "/users" }] : []),
    { icon: HiCog, label: "Settings", page: "settings", path: "/settings" },
  ];

  return (
    <>
      {/* Overlay for mobile */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 md:hidden"
          onClick={onToggle}
        />
      )}

      {/* Sidebar */}
      <div
        className={`fixed left-0 top-0 h-full bg-gradient-to-b from-gray-900 via-gray-800 to-gray-900 z-50 transition-all duration-300 ${
          isOpen ? "translate-x-0" : "-translate-x-full"
        } w-64`}
      >
        <div className="flex flex-col h-full p-6">
          {/* Logo */}
          <div className="flex items-center justify-between mb-8">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-lg flex items-center justify-center">
                <MdMedicalServices className="w-5 h-5 text-white" />
              </div>
              <span className="text-white font-bold text-lg">SuperDoc</span>
            </div>
            <button
              onClick={onToggle}
              className="md:hidden text-gray-400 hover:text-white transition-colors"
            >
              <HiX className="w-6 h-6" />
            </button>
          </div>

          {/* Menu Items */}
          <nav className="flex-1 space-y-2">
            {menuItems.map((item) => {
              const IconComponent = item.icon;
              return (
                <button
                  key={item.page}
                  onClick={() => {
                    navigate(item.path);
                    if (window.innerWidth < 768) onToggle();
                  }}
                  className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                    currentPage === item.page
                      ? "bg-blue-500 text-white"
                      : "text-gray-300 hover:bg-gray-700 hover:text-white"
                  }`}
                >
                  <IconComponent className="w-5 h-5" />
                  <span className="font-medium">{item.label}</span>
                </button>
              );
            })}
          </nav>

          {/* User Profile Section */}
          {user && (
            <div className="mt-auto pt-6 border-t border-gray-700">
              <div className="flex items-center space-x-3 mb-4">
                {/* Profile Picture */}
                <button
                  onClick={() => {
                    navigate("/profile");
                    if (window.innerWidth < 768) onToggle();
                  }}
                  className="flex items-center space-x-3 flex-1 min-w-0 p-2 rounded-lg hover:bg-gray-700 transition-colors group"
                  title="Go to My Account"
                >
                  {(() => {
                    const profilePicture = user.role === "DOCTOR" 
                      ? (profile as DoctorProfile)?.profilePhotoUrl 
                      : (profile as PatientProfile)?.profilePicture;
                    const profilePictureUrl = getImageUrl(profilePicture);
                    return profilePictureUrl ? (
                      <img
                        src={profilePictureUrl}
                      alt="Profile"
                      className="w-10 h-10 rounded-full object-cover border-2 border-gray-600 group-hover:border-blue-500 transition-colors"
                      onError={(e) => {
                        // Fallback to initial if image fails to load
                        const target = e.target as HTMLImageElement;
                        target.style.display = 'none';
                        const fallback = target.nextElementSibling as HTMLElement;
                        if (fallback) fallback.style.display = 'flex';
                      }}
                      />
                    ) : null;
                  })()}
                  <div 
                    className={`w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-cyan-400 flex items-center justify-center ${
                      (() => {
                        const profilePicture = user.role === "DOCTOR" 
                          ? (profile as DoctorProfile)?.profilePhotoUrl 
                          : (profile as PatientProfile)?.profilePicture;
                        return getImageUrl(profilePicture) ? 'hidden' : '';
                      })()
                    }`}
                  >
                    <span className="text-white font-semibold">
                      {profile?.firstName ? profile.firstName.charAt(0).toUpperCase() : user.email.charAt(0).toUpperCase()}
                    </span>
                  </div>
                  
                  {/* User Info */}
                  <div className="flex-1 min-w-0 text-left">
                    <p className="text-white font-medium truncate group-hover:text-blue-400 transition-colors">
                      {profile?.firstName && profile?.lastName 
                        ? `${profile.firstName} ${profile.lastName}`
                        : user.email.split('@')[0]
                      }
                    </p>
                    <p className="text-gray-400 text-sm capitalize">{user.role.toLowerCase()}</p>
                  </div>
                </button>

                {/* Logout Button */}
                <button
                  onClick={logout}
                  className="text-gray-400 hover:text-white transition-colors p-2 hover:bg-gray-700 rounded-lg"
                  title="Logout"
                >
                  <HiLogout className="w-5 h-5" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
