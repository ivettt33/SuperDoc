import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import API from "../api";

interface OnboardingSummary {
  fullName: string;
  role: string;
  profileType: string;
  profileData: any;
}

export default function Dashboard() {
  const { user } = useAuth();
  const [onboardingData, setOnboardingData] = useState<OnboardingSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchOnboardingData = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await API.get("/onboarding/summary");
      setOnboardingData(res.data);
    } catch (e: any) {
      const errorMessage = e?.response?.data?.message || 
                          (typeof e?.response?.data === 'string' ? e.response.data : null) ||
                          "Failed to fetch profile data";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOnboardingData();
  }, []);

  return (
    <div className="p-6" data-cy="dashboard">
      {/* Main Content */}
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="flex items-center space-x-3">
                <svg className="animate-spin h-8 w-8 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span className="text-white text-lg">Loading profile...</span>
              </div>
            </div>
          ) : error ? (
            <div className="card">
              <div className="flex items-center mb-4">
                <div className="flex-shrink-0">
                  <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
                    <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                    </svg>
                  </div>
                </div>
                <div className="ml-4">
                  <h2 className="text-lg font-semibold text-white">Error Loading Profile</h2>
                  <p className="text-sm text-gray-400">Unable to fetch your profile data</p>
                </div>
              </div>
              <div className="bg-red-900/20 border border-red-700 rounded-lg p-4">
                <p className="text-red-300 text-sm">{error}</p>
                <button
                  onClick={fetchOnboardingData}
                  className="mt-3 btn-primary"
                >
                  Try Again
                </button>
              </div>
            </div>
          ) : onboardingData ? (
            <div className="space-y-6">
              {/* Profile Header */}
              <div className="card">
                <div className="flex items-center space-x-6">
                  {/* Profile Picture */}
                  <div className="flex-shrink-0">
                    {onboardingData.profileData?.profilePicture ? (
                      <img
                        src={onboardingData.profileData.profilePicture}
                        alt="Profile"
                        className="w-20 h-20 rounded-full object-cover border-2 border-gray-600"
                      />
                    ) : (
                      <div className="w-20 h-20 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-full flex items-center justify-center">
                        <span className="text-white font-bold text-2xl">
                          {onboardingData.fullName?.charAt(0) || user?.email?.charAt(0)?.toUpperCase()}
                        </span>
                      </div>
                    )}
                  </div>
                  
                  {/* Profile Info */}
                  <div className="flex-1">
                    <h1 className="text-2xl font-bold text-white">
                      {onboardingData.fullName || "Complete your profile"}
                    </h1>
                    <div className="flex items-center space-x-4 mt-2">
                      <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                        {onboardingData.role}
                      </span>
                    </div>
                    <p className="text-gray-400 text-sm mt-1">
                      {user?.email}
                    </p>
                  </div>
                </div>
              </div>

              {/* Profile Details */}
              {onboardingData.profileData && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Personal Information */}
                  <div className="card">
                    <div className="flex items-center mb-4">
                      <div className="flex-shrink-0">
                        <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                          <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                          </svg>
                        </div>
                      </div>
                      <div className="ml-4">
                        <h2 className="text-lg font-semibold text-white">Personal Information</h2>
                        <p className="text-sm text-gray-400">Your basic details</p>
                      </div>
                    </div>
                    
                    <div className="space-y-3">
                      {onboardingData.profileData.firstName && (
                        <div className="flex justify-between">
                          <span className="text-sm font-medium text-gray-400">First Name:</span>
                          <span className="text-sm text-white">{onboardingData.profileData.firstName}</span>
                        </div>
                      )}
                      {onboardingData.profileData.lastName && (
                        <div className="flex justify-between">
                          <span className="text-sm font-medium text-gray-400">Last Name:</span>
                          <span className="text-sm text-white">{onboardingData.profileData.lastName}</span>
                        </div>
                      )}
                      {onboardingData.profileData.dateOfBirth && (
                        <div className="flex justify-between">
                          <span className="text-sm font-medium text-gray-400">Date of Birth:</span>
                          <span className="text-sm text-white">{new Date(onboardingData.profileData.dateOfBirth).toLocaleDateString()}</span>
                        </div>
                      )}
                      {onboardingData.profileData.gender && (
                        <div className="flex justify-between">
                          <span className="text-sm font-medium text-gray-400">Gender:</span>
                          <span className="text-sm text-white capitalize">{onboardingData.profileData.gender}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Medical Information */}
                  <div className="card">
                    <div className="flex items-center mb-4">
                      <div className="flex-shrink-0">
                        <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
                          <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                        </div>
                      </div>
                      <div className="ml-4">
                        <h2 className="text-lg font-semibold text-white">Medical Information</h2>
                        <p className="text-sm text-gray-400">Health details</p>
                      </div>
                    </div>
                    
                    <div className="space-y-3">
                      {onboardingData.profileData.conditions && (
                        <div>
                          <span className="text-sm font-medium text-gray-400 block mb-2">Medical Conditions:</span>
                          <span className="text-sm text-white">{onboardingData.profileData.conditions}</span>
                        </div>
                      )}
                      {onboardingData.profileData.insuranceNumber && (
                        <div className="flex justify-between">
                          <span className="text-sm font-medium text-gray-400">Insurance Number:</span>
                          <span className="text-sm text-white">{onboardingData.profileData.insuranceNumber}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}

            </div>
          ) : (
            <div className="card">
              <div className="text-center py-8">
                <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <svg className="w-8 h-8 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
                <h3 className="text-xl font-semibold text-white mb-2">No Profile Data</h3>
                <p className="text-gray-400 mb-6">Complete your onboarding to see your profile information here</p>
                <button
                  onClick={() => window.location.href = '/onboarding'}
                  className="btn-primary"
                >
                  Start Onboarding
                </button>
              </div>
            </div>
          )}
    </div>
  );
}


