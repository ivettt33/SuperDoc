import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { DoctorApi, PatientApi, DoctorProfile, PatientProfile, DoctorOnboardRequest, PatientOnboardRequest, FileApi } from "../api";
import { validators } from "../utils/validation";
import Select from "../components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SPECIALIZATIONS } from "../constants/specializations";

export default function EditProfile() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const [doctorProfile, setDoctorProfile] = useState<DoctorProfile | null>(null);
  const [patientProfile, setPatientProfile] = useState<PatientProfile | null>(null);

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user) return;

      try {
        setLoading(true);
        if (user.role === "DOCTOR") {
          const profile = await DoctorApi.getMyProfile();
          setDoctorProfile(profile);
        } else {
          const profile = await PatientApi.getMyProfile();
          setPatientProfile(profile);
        }
      } catch (err: any) {
        setError(err.response?.data?.message || "Failed to load profile");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [user]);

  const handleSave = async () => {
    if (!user) return;

    setSaving(true);
    setError(null);
    setSuccess(false);

    try {
      if (user.role === "DOCTOR" && doctorForm) {
        await DoctorApi.updateProfile(doctorForm);
      } else if (user.role === "PATIENT" && patientForm) {
        await PatientApi.updateProfile(patientForm);
      }
      setSuccess(true);
      setTimeout(() => {
        navigate("/profile");
      }, 1500);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to update profile");
    } finally {
      setSaving(false);
    }
  };

  const handleProfilePictureUpload = async (file: File, isDoctor: boolean) => {
    try {
      const result = await FileApi.uploadFile(file);
      const fileName = result.fileName || result.filePath;
      // Store just the filename, not the full URL
      
      if (isDoctor) {
        setDoctorForm({ ...doctorForm!, profilePhotoUrl: fileName });
      } else {
        setPatientForm({ ...patientForm!, profilePicture: fileName });
      }
    } catch (err: any) {
      console.error("Failed to upload file:", err);
      setError(err.response?.data?.error || "Failed to upload profile picture");
    }
  };

  const [doctorForm, setDoctorForm] = useState<DoctorOnboardRequest | null>(null);
  const [patientForm, setPatientForm] = useState<PatientOnboardRequest | null>(null);

  useEffect(() => {
    if (doctorProfile) {
      setDoctorForm({
        firstName: doctorProfile.firstName,
        lastName: doctorProfile.lastName,
        specialization: doctorProfile.specialization || "",
        bio: doctorProfile.bio || "",
        licenseNumber: doctorProfile.licenseNumber || "",
        clinicName: doctorProfile.clinicName || "",
        yearsOfExperience: doctorProfile.yearsOfExperience || 0,
        profilePhotoUrl: doctorProfile.profilePhotoUrl || "",
        openingHours: doctorProfile.openingHours || "09:00",
        closingHours: doctorProfile.closingHours || "17:00",
        isAbsent: doctorProfile.isAbsent || false,
      });
    }
  }, [doctorProfile]);

  useEffect(() => {
    if (patientProfile) {
      setPatientForm({
        firstName: patientProfile.firstName,
        lastName: patientProfile.lastName,
        dateOfBirth: patientProfile.dateOfBirth || "",
        gender: patientProfile.gender || "",
        conditions: patientProfile.conditions || "",
        insuranceNumber: patientProfile.insuranceNumber || "",
        profilePicture: patientProfile.profilePicture || "",
      });
    }
  }, [patientProfile]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (error && !doctorForm && !patientForm) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-red-500">{error}</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 p-4">
      <div className="max-w-4xl mx-auto">
        <div className="bg-gray-800/50 backdrop-blur-xl rounded-xl p-8 border border-gray-700/50 shadow-2xl">
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-3xl font-bold text-white">Edit Profile</h1>
            <button
              onClick={() => navigate("/profile")}
              className="text-gray-400 hover:text-white transition-colors"
            >
              ← Back to Profile
            </button>
          </div>

          {success && (
            <div className="mb-6 p-4 bg-green-500/20 border border-green-500/50 rounded-lg text-green-400">
              Profile updated successfully!
            </div>
          )}

          {error && (
            <div className="mb-6 p-4 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400">
              {error}
            </div>
          )}

          {user?.role === "DOCTOR" && doctorForm && (
            <DoctorEditForm
              form={doctorForm}
              setForm={setDoctorForm}
              onSave={handleSave}
              saving={saving}
              onFileUpload={(file) => handleProfilePictureUpload(file, true)}
              currentPhotoUrl={doctorProfile?.profilePhotoUrl}
            />
          )}

          {user?.role === "PATIENT" && patientForm && (
            <PatientEditForm
              form={patientForm}
              setForm={setPatientForm}
              onSave={handleSave}
              saving={saving}
              onFileUpload={(file) => handleProfilePictureUpload(file, false)}
              currentPhotoUrl={patientProfile?.profilePicture}
            />
          )}
        </div>
      </div>
    </div>
  );
}

function DoctorEditForm({
  form,
  setForm,
  onSave,
  saving,
  onFileUpload,
  currentPhotoUrl,
}: {
  form: DoctorOnboardRequest;
  setForm: (form: DoctorOnboardRequest) => void;
  onSave: () => void;
  saving: boolean;
  onFileUpload: (file: File) => void;
  currentPhotoUrl?: string;
}) {
  const [uploading, setUploading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [showOtherInput, setShowOtherInput] = useState(false);
  const [customSpecialization, setCustomSpecialization] = useState("");

  useEffect(() => {
    if (form.specialization && !SPECIALIZATIONS.includes(form.specialization)) {
      setShowOtherInput(true);
      setCustomSpecialization(form.specialization);
    }
  }, [form.specialization]);

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    const firstNameError = validators.name(form.firstName, "First name");
    if (firstNameError) newErrors.firstName = firstNameError;

    const lastNameError = validators.name(form.lastName, "Last name");
    if (lastNameError) newErrors.lastName = lastNameError;

    const specializationValue = showOtherInput ? customSpecialization : form.specialization;
    const specializationError = validators.specialization(specializationValue);
    if (specializationError) newErrors.specialization = specializationError;

    const licenseError = validators.licenseNumber(form.licenseNumber);
    if (licenseError) newErrors.licenseNumber = licenseError;

    const clinicError = validators.clinicName(form.clinicName);
    if (clinicError) newErrors.clinicName = clinicError;

    const experienceError = validators.experience(form.yearsOfExperience);
    if (experienceError) newErrors.yearsOfExperience = experienceError;

    const bioError = validators.textLength(form.bio, "Bio", 1000);
    if (bioError) newErrors.bio = bioError;

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (validateForm()) {
      const specialization = showOtherInput ? customSpecialization.trim() : (form.specialization ?? "").trim();
      setForm({ ...form, specialization });
      onSave();
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setUploading(true);
      try {
        await onFileUpload(file);
      } finally {
        setUploading(false);
      }
    }
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-semibold text-white mb-4">Personal Information</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">First Name *</label>
          <input
            type="text"
            value={form.firstName}
            onChange={(e) => setForm({ ...form, firstName: e.target.value })}
            className="w-full px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Last Name *</label>
          <input
            type="text"
            value={form.lastName}
            onChange={(e) => setForm({ ...form, lastName: e.target.value })}
            className="w-full px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none"
            required
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Specialization *</label>
        <Select
          id="specialization"
          value={showOtherInput ? "Other" : (form.specialization || "")}
          onChange={(e) => {
            const value = e.target.value;
            if (value === "Other") {
              setShowOtherInput(true);
              setCustomSpecialization("");
              setForm({ ...form, specialization: "" });
            } else {
              setShowOtherInput(false);
              setCustomSpecialization("");
              setForm({ ...form, specialization: value });
            }
            if (errors.specialization) setErrors({ ...errors, specialization: "" });
          }}
          error={!!errors.specialization}
          className="w-full"
        >
          <option value="">Select your specialization</option>
          {SPECIALIZATIONS.map((spec) => (
            <option key={spec} value={spec}>
              {spec}
            </option>
          ))}
        </Select>

        {showOtherInput && (
          <div className="mt-3">
            <Input
              type="text"
              value={customSpecialization}
              onChange={(e) => {
                setCustomSpecialization(e.target.value);
                setForm({ ...form, specialization: e.target.value });
                if (errors.specialization) setErrors({ ...errors, specialization: "" });
              }}
              placeholder="Please specify your specialization"
              maxLength={100}
              autoFocus
            />
          </div>
        )}
        {errors.specialization && <p className="text-red-400 text-sm mt-1">{errors.specialization}</p>}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Bio (max 1000 characters)</label>
        <textarea
          value={form.bio}
          onChange={(e) => {
            setForm({ ...form, bio: e.target.value });
            if (errors.bio) setErrors({ ...errors, bio: "" });
          }}
          rows={4}
          maxLength={1000}
          className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
            errors.bio ? "border-red-500" : "border-gray-600"
          } focus:border-blue-500 focus:outline-none`}
        />
        {errors.bio && <p className="text-red-400 text-sm mt-1">{errors.bio}</p>}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">License Number *</label>
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            value={form.licenseNumber}
            onChange={(e) => {
              setForm({ ...form, licenseNumber: e.target.value });
              if (errors.licenseNumber) setErrors({ ...errors, licenseNumber: "" });
            }}
            maxLength={50}
            className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
              errors.licenseNumber ? "border-red-500" : "border-gray-600"
            } focus:border-blue-500 focus:outline-none`}
          />
          {errors.licenseNumber && <p className="text-red-400 text-sm mt-1">{errors.licenseNumber}</p>}
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Clinic Name *</label>
          <input
            type="text"
            value={form.clinicName}
            onChange={(e) => {
              setForm({ ...form, clinicName: e.target.value });
              if (errors.clinicName) setErrors({ ...errors, clinicName: "" });
            }}
            maxLength={100}
            className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
              errors.clinicName ? "border-red-500" : "border-gray-600"
            } focus:border-blue-500 focus:outline-none`}
          />
          {errors.clinicName && <p className="text-red-400 text-sm mt-1">{errors.clinicName}</p>}
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Years of Experience *</label>
        <input
          type="number"
          min="0"
          max="50"
          value={form.yearsOfExperience || 0}
          onChange={(e) => {
            setForm({ ...form, yearsOfExperience: parseInt(e.target.value) || 0 });
            if (errors.yearsOfExperience) setErrors({ ...errors, yearsOfExperience: "" });
          }}
          className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
            errors.yearsOfExperience ? "border-red-500" : "border-gray-600"
          } focus:border-blue-500 focus:outline-none`}
        />
        {errors.yearsOfExperience && <p className="text-red-400 text-sm mt-1">{errors.yearsOfExperience}</p>}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Opening Hours</label>
          <input
            type="time"
            value={form.openingHours || "09:00"}
            onChange={(e) => setForm({ ...form, openingHours: e.target.value })}
            className="w-full px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Closing Hours</label>
          <input
            type="time"
            value={form.closingHours || "17:00"}
            onChange={(e) => setForm({ ...form, closingHours: e.target.value })}
            className="w-full px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Availability Status</label>
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={form.isAbsent || false}
              onChange={(e) => setForm({ ...form, isAbsent: e.target.checked })}
              className="w-5 h-5 rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-blue-500"
            />
            <span className="text-white">Mark as out of office</span>
          </label>
          {form.isAbsent && (
            <span className="text-yellow-400 text-sm">All time slots will be unavailable</span>
          )}
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Profile Picture</label>
        {currentPhotoUrl && (
          <div className="mb-4">
            <img src={currentPhotoUrl} alt="Current profile" className="w-32 h-32 rounded-full object-cover" />
          </div>
        )}
        <input
          type="file"
          accept="image/*"
          onChange={handleFileChange}
          disabled={uploading}
          className="w-full px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none"
        />
        {uploading && <p className="text-gray-400 text-sm mt-2">Uploading...</p>}
      </div>

      <div className="flex justify-end space-x-4 pt-4">
        <button
          onClick={() => window.history.back()}
          className="px-6 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
        >
          Cancel
        </button>
        <button
          onClick={handleSave}
          disabled={saving || uploading}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
        >
          {saving ? "Saving..." : "Save Changes"}
        </button>
      </div>
    </div>
  );
}

function PatientEditForm({
  form,
  setForm,
  onSave,
  saving,
  onFileUpload,
  currentPhotoUrl,
}: {
  form: PatientOnboardRequest;
  setForm: (form: PatientOnboardRequest) => void;
  onSave: () => void;
  saving: boolean;
  onFileUpload: (file: File) => void;
  currentPhotoUrl?: string;
}) {
  const [uploading, setUploading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    const firstNameError = validators.name(form.firstName, "First name");
    if (firstNameError) newErrors.firstName = firstNameError;

    const lastNameError = validators.name(form.lastName, "Last name");
    if (lastNameError) newErrors.lastName = lastNameError;

    const dateError = validators.dateOfBirth(form.dateOfBirth);
    if (dateError) newErrors.dateOfBirth = dateError;

    const conditionsError = validators.textLength(form.conditions, "Medical conditions", 1000);
    if (conditionsError) newErrors.conditions = conditionsError;

    const insuranceError = validators.insuranceNumber(form.insuranceNumber);
    if (insuranceError) newErrors.insuranceNumber = insuranceError;

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (validateForm()) {
      onSave();
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setUploading(true);
      try {
        await onFileUpload(file);
      } finally {
        setUploading(false);
      }
    }
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-semibold text-white mb-4">Personal Information</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">First Name *</label>
          <input
            type="text"
            value={form.firstName}
            onChange={(e) => {
              setForm({ ...form, firstName: e.target.value });
              if (errors.firstName) setErrors({ ...errors, firstName: "" });
            }}
            maxLength={50}
            className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
              errors.firstName ? "border-red-500" : "border-gray-600"
            } focus:border-blue-500 focus:outline-none`}
            required
          />
          {errors.firstName && <p className="text-red-400 text-sm mt-1">{errors.firstName}</p>}
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Last Name *</label>
          <input
            type="text"
            value={form.lastName}
            onChange={(e) => {
              setForm({ ...form, lastName: e.target.value });
              if (errors.lastName) setErrors({ ...errors, lastName: "" });
            }}
            maxLength={50}
            className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
              errors.lastName ? "border-red-500" : "border-gray-600"
            } focus:border-blue-500 focus:outline-none`}
            required
          />
          {errors.lastName && <p className="text-red-400 text-sm mt-1">{errors.lastName}</p>}
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Date of Birth</label>
        <input
          type="date"
          value={form.dateOfBirth}
          onChange={(e) => {
            setForm({ ...form, dateOfBirth: e.target.value });
            if (errors.dateOfBirth) setErrors({ ...errors, dateOfBirth: "" });
          }}
          className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
            errors.dateOfBirth ? "border-red-500" : "border-gray-600"
          } focus:border-blue-500 focus:outline-none`}
        />
        {errors.dateOfBirth && <p className="text-red-400 text-sm mt-1">{errors.dateOfBirth}</p>}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Gender</label>
        <select
          value={form.gender}
          onChange={(e) => setForm({ ...form, gender: e.target.value })}
          className="w-full px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none"
        >
          <option value="">Select Gender</option>
          <option value="Male">Male</option>
          <option value="Female">Female</option>
          <option value="Other">Other</option>
        </select>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Medical Conditions (max 1000 characters)</label>
        <textarea
          value={form.conditions}
          onChange={(e) => {
            setForm({ ...form, conditions: e.target.value });
            if (errors.conditions) setErrors({ ...errors, conditions: "" });
          }}
          rows={3}
          maxLength={1000}
          className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
            errors.conditions ? "border-red-500" : "border-gray-600"
          } focus:border-blue-500 focus:outline-none`}
          placeholder="Any existing medical conditions..."
        />
        {errors.conditions && <p className="text-red-400 text-sm mt-1">{errors.conditions}</p>}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Insurance Number</label>
        <input
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          value={form.insuranceNumber}
          onChange={(e) => {
            setForm({ ...form, insuranceNumber: e.target.value });
            if (errors.insuranceNumber) setErrors({ ...errors, insuranceNumber: "" });
          }}
          maxLength={50}
          className={`w-full px-4 py-2 bg-gray-700 text-white rounded-lg border ${
            errors.insuranceNumber ? "border-red-500" : "border-gray-600"
          } focus:border-blue-500 focus:outline-none`}
        />
        {errors.insuranceNumber && <p className="text-red-400 text-sm mt-1">{errors.insuranceNumber}</p>}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Profile Picture</label>
        {currentPhotoUrl && (
          <div className="mb-4">
            <img src={currentPhotoUrl} alt="Current profile" className="w-32 h-32 rounded-full object-cover" />
          </div>
        )}
        <input
          type="file"
          accept="image/*"
          onChange={handleFileChange}
          disabled={uploading}
          className="w-full px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none"
        />
        {uploading && <p className="text-gray-400 text-sm mt-2">Uploading...</p>}
      </div>

      <div className="flex justify-end space-x-4 pt-4">
        <button
          onClick={() => window.history.back()}
          className="px-6 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
        >
          Cancel
        </button>
        <button
          onClick={handleSave}
          disabled={saving || uploading}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
        >
          {saving ? "Saving..." : "Save Changes"}
        </button>
      </div>
    </div>
  );
}

