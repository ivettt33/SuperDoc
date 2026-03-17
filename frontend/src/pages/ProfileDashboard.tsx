import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { PatientApi, DoctorApi, PatientProfile, DoctorProfile, FileApi } from "../api";
import { HiPencil, HiCamera, HiCheckCircle, HiXCircle } from "react-icons/hi";
import { getImageUrl } from "../utils/imageUtils";

export default function ProfileDashboard() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [profile, setProfile] = useState<PatientProfile | DoctorProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user) return;

      try {
        setLoading(true);
        let data;
        
        if (user.role === "PATIENT") {
          data = await PatientApi.getMyProfile();
        } else {
          data = await DoctorApi.getMyProfile();
        }
        
        setProfile(data);
      } catch (err) {
        console.error("Failed to fetch profile:", err);
        setError("Failed to load profile information");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [user]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (error && !profile) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900">
        <div className="text-red-500">{error}</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <h1 className="text-3xl font-bold text-white mb-8">My Profile</h1>
        
        {!profile ? (
          <div className="bg-gray-800 rounded-lg shadow-sm p-8 text-center border border-gray-700">
            <p className="text-gray-400 mb-4">No profile information available. Please complete onboarding.</p>
            <button
              onClick={() => navigate(user?.role === "DOCTOR" ? "/onboarding/doctor" : "/onboarding/patient")}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Complete Onboarding
            </button>
          </div>
        ) : user?.role === "PATIENT" ? (
          <PatientProfileView profile={profile as PatientProfile} user={user} navigate={navigate} setProfile={setProfile} />
        ) : (
          <DoctorProfileView profile={profile as DoctorProfile} user={user} navigate={navigate} setProfile={setProfile} />
        )}
      </div>
    </div>
  );
}

function PatientProfileView({ profile, user, navigate, setProfile }: { profile: PatientProfile; user: any; navigate: any; setProfile: (profile: PatientProfile | DoctorProfile) => void }) {
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    setUploadingPhoto(true);
    try {
      const result = await FileApi.uploadFile(file);
      const fileName = result.fileName || result.filePath;
      // Store just the filename, not the full URL
      
      const updatedProfile = await PatientApi.updateProfile({
        ...profile,
        profilePicture: fileName,
      });
      
      // Update the profile state instead of reloading
      setProfile(updatedProfile);
    } catch (err: any) {
      console.error("Failed to upload photo:", err);
      alert("Failed to upload profile picture");
    } finally {
      setUploadingPhoto(false);
    }
  };

  const profilePicture = profile.profilePicture;
  const profilePictureUrl = getImageUrl(profilePicture);
  const fullName = (profile.firstName || profile.lastName)
    ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim()
    : user?.email?.split('@')[0] || '';

  return (
    <div className="space-y-6">
      <div className="bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-700">
        <div className="flex items-start gap-6">
          <div className="relative flex-shrink-0">
            {profilePictureUrl ? (
              <>
                <img 
                  src={profilePictureUrl} 
                  alt="Profile" 
                  className="w-24 h-24 rounded-full object-cover"
                  onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    target.style.display = 'none';
                    const fallback = target.parentElement?.querySelector('.fallback-initial') as HTMLElement;
                    if (fallback) fallback.style.display = 'flex';
                  }}
                />
                <div className="fallback-initial hidden w-24 h-24 rounded-full bg-blue-500 items-center justify-center">
                  <span className="text-white font-bold text-2xl">
                    {profile.firstName ? profile.firstName.charAt(0).toUpperCase() : user?.email?.charAt(0).toUpperCase()}
                  </span>
                </div>
              </>
            ) : (
              <div className="w-24 h-24 rounded-full bg-blue-500 flex items-center justify-center">
                <span className="text-white font-bold text-2xl">
                  {profile.firstName ? profile.firstName.charAt(0).toUpperCase() : user?.email?.charAt(0).toUpperCase()}
                </span>
              </div>
            )}
            {/* Camera Icon Overlay */}
            <label className="absolute bottom-0 right-0 bg-blue-600 text-white p-2 rounded-full cursor-pointer hover:bg-blue-700 transition-colors shadow-lg">
              <HiCamera className="w-4 h-4" />
              <input
                type="file"
                accept="image/*"
                onChange={handlePhotoUpload}
                disabled={uploadingPhoto}
                className="hidden"
              />
            </label>
          </div>

          <div className="flex-1">
            <h2 className="text-2xl font-bold text-white mb-1">{fullName}</h2>
            <p className="text-gray-400 mb-1 capitalize">{user?.role?.toLowerCase() || 'Patient'}</p>
            <p className="text-gray-500 text-sm">{user?.email || ''}</p>
          </div>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg shadow-sm p-6 border border-gray-700">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-white">Personal Information</h2>
          <button
            onClick={() => navigate("/profile/edit")}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <HiPencil className="w-4 h-4" />
            <span>Edit</span>
          </button>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <InfoField label="First Name" value={profile.firstName || "Not provided"} />
          <InfoField label="Last Name" value={profile.lastName || "Not provided"} />
          <InfoField label="Date of Birth" value={profile.dateOfBirth ? new Date(profile.dateOfBirth).toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\//g, '-') : "Not provided"} />
          <InfoField label="Gender" value={profile.gender || "Not specified"} />
          <InfoField label="Email Address" value={user?.email || "Not provided"} />
          <InfoField label="User Role" value={user?.role || "Not specified"} />
        </div>
      </div>

      {(profile.conditions || profile.insuranceNumber) && (
        <div className="bg-gray-800 rounded-lg shadow-sm p-6 border border-gray-700">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-white">Medical Information</h2>
            <button
              onClick={() => navigate("/profile/edit")}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <HiPencil className="w-4 h-4" />
              <span>Edit</span>
            </button>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {profile.insuranceNumber && (
              <InfoField label="Insurance Number" value={profile.insuranceNumber} />
            )}
            {profile.conditions && (
              <div className="md:col-span-2">
                <InfoField label="Medical Conditions" value={profile.conditions} />
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function DoctorProfileView({ profile, user, navigate, setProfile }: { profile: DoctorProfile; user: any; navigate: any; setProfile: (profile: PatientProfile | DoctorProfile) => void }) {
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);
  
  const handleAbsenceToggle = async () => {
    setUpdatingStatus(true);
    try {
      await DoctorApi.updateProfile({
        firstName: profile.firstName,
        lastName: profile.lastName,
        specialization: profile.specialization || "",
        bio: profile.bio || "",
        licenseNumber: profile.licenseNumber || "",
        clinicName: profile.clinicName || "",
        yearsOfExperience: profile.yearsOfExperience || 0,
        profilePhotoUrl: profile.profilePhotoUrl || "",
        openingHours: profile.openingHours || "09:00",
        closingHours: profile.closingHours || "17:00",
        isAbsent: !profile.isAbsent,
      });
      window.location.reload();
    } catch (err: any) {
      console.error("Failed to update status:", err);
      alert("Failed to update absence status");
    } finally {
      setUpdatingStatus(false);
    }
  };
  
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    setUploadingPhoto(true);
    try {
      const result = await FileApi.uploadFile(file);
      const fileName = result.fileName || result.filePath;
      // Store just the filename, not the full URL
      
      const updatedProfile = await DoctorApi.updateProfile({
        firstName: profile.firstName,
        lastName: profile.lastName,
        specialization: profile.specialization || "",
        bio: profile.bio || "",
        licenseNumber: profile.licenseNumber || "",
        clinicName: profile.clinicName || "",
        yearsOfExperience: profile.yearsOfExperience || 0,
        profilePhotoUrl: fileName,
        openingHours: profile.openingHours || "09:00",
        closingHours: profile.closingHours || "17:00",
        isAbsent: profile.isAbsent || false,
      });
      
      // Update the profile state instead of reloading
      setProfile(updatedProfile);
    } catch (err: any) {
      console.error("Failed to upload photo:", err);
      alert("Failed to upload profile picture");
    } finally {
      setUploadingPhoto(false);
    }
  };

  const profilePicture = profile.profilePhotoUrl;
  const profilePictureUrl = getImageUrl(profilePicture);
  const fullName = (profile.firstName || profile.lastName)
    ? `Dr. ${profile.firstName || ''} ${profile.lastName || ''}`.trim()
    : user?.email?.split('@')[0] || '';

  return (
    <div className="space-y-6">
      <div className="bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-700">
        {profile.isAbsent && (
          <div className="mb-4 p-3 bg-yellow-500/20 border border-yellow-500/50 rounded-lg flex items-center gap-2">
            <HiXCircle className="w-5 h-5 text-yellow-400" />
            <span className="text-yellow-400 font-medium">You are currently marked as out of office</span>
          </div>
        )}
        <div className="flex items-start gap-6">
          <div className="relative flex-shrink-0">
            {profilePictureUrl ? (
              <>
                <img 
                  src={profilePictureUrl} 
                  alt="Profile" 
                  className="w-24 h-24 rounded-full object-cover"
                  onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    target.style.display = 'none';
                    const fallback = target.parentElement?.querySelector('.fallback-initial') as HTMLElement;
                    if (fallback) fallback.style.display = 'flex';
                  }}
                />
                <div className="fallback-initial hidden w-24 h-24 rounded-full bg-blue-500 items-center justify-center">
                  <span className="text-white font-bold text-2xl">
                    {profile.firstName ? profile.firstName.charAt(0).toUpperCase() : user?.email?.charAt(0).toUpperCase()}
                  </span>
                </div>
              </>
            ) : (
              <div className="w-24 h-24 rounded-full bg-blue-500 flex items-center justify-center">
                <span className="text-white font-bold text-2xl">
                  {profile.firstName ? profile.firstName.charAt(0).toUpperCase() : user?.email?.charAt(0).toUpperCase()}
                </span>
              </div>
            )}
            {/* Camera Icon Overlay */}
            <label className="absolute bottom-0 right-0 bg-blue-600 text-white p-2 rounded-full cursor-pointer hover:bg-blue-700 transition-colors shadow-lg">
              <HiCamera className="w-4 h-4" />
              <input
                type="file"
                accept="image/*"
                onChange={handlePhotoUpload}
                disabled={uploadingPhoto}
                className="hidden"
              />
            </label>
          </div>

          <div className="flex-1">
            <h2 className="text-2xl font-bold text-white mb-1">{fullName}</h2>
            <p className="text-gray-400 mb-1 capitalize">{user?.role?.toLowerCase() || 'Doctor'}</p>
            {profile.bio && (
              <p className="text-gray-400 text-sm mt-2">{profile.bio}</p>
            )}
          </div>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg shadow-sm p-6 border border-gray-700">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-white">Personal Information</h2>
          <button
            onClick={() => navigate("/profile/edit")}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <HiPencil className="w-4 h-4" />
            <span>Edit</span>
          </button>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <InfoField label="First Name" value={profile.firstName || "Not provided"} />
          <InfoField label="Last Name" value={profile.lastName || "Not provided"} />
          <InfoField label="Email Address" value={user?.email || "Not provided"} />
          <InfoField label="User Role" value={user?.role || "Not specified"} />
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg shadow-sm p-6 border border-gray-700">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-white">Professional Information</h2>
          <button
            onClick={() => navigate("/profile/edit")}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <HiPencil className="w-4 h-4" />
            <span>Edit</span>
          </button>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <InfoField label="Specialization" value={profile.specialization || "Not provided"} />
          <InfoField label="Years of Experience" value={profile.yearsOfExperience !== undefined && profile.yearsOfExperience > 0 ? `${profile.yearsOfExperience} years` : "Not provided"} />
          <InfoField label="Place of Work" value={profile.clinicName || "Not provided"} />
          {profile.licenseNumber && <InfoField label="License Number" value={profile.licenseNumber} />}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-400 mb-2">Availability Status</label>
            <div className="flex items-center gap-3">
              <button
                onClick={handleAbsenceToggle}
                disabled={updatingStatus}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors font-medium ${
                  profile.isAbsent
                    ? "bg-red-600 hover:bg-red-700 text-white"
                    : "bg-green-600 hover:bg-green-700 text-white"
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                {profile.isAbsent ? (
                  <>
                    <HiXCircle className="w-5 h-5" />
                    <span>Mark as Available</span>
                  </>
                ) : (
                  <>
                    <HiCheckCircle className="w-5 h-5" />
                    <span>Mark as Out of Office</span>
                  </>
                )}
              </button>
              <span className="text-gray-400 text-sm">
                {profile.isAbsent ? "All time slots are currently unavailable" : "You are available for appointments"}
              </span>
            </div>
          </div>
        </div>
        {profile.bio && (
          <div className="mt-6">
            <InfoField label="Bio" value={profile.bio} />
          </div>
        )}
      </div>
    </div>
  );
}

function InfoField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-400 mb-1">{label}</label>
      <p className="text-white font-medium">{value}</p>
    </div>
  );
}
