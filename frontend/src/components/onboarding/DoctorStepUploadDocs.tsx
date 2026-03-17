import React from "react";
import { DoctorOnboardRequest } from "../../api";
import FileUpload from "../ui/FileUpload";
import StepHeader from "../ui/StepHeader";
import NavigationButtons from "../ui/NavigationButtons";

interface DoctorStepUploadDocsProps {
  formData: Partial<DoctorOnboardRequest>;
  updateFormData: (data: Partial<DoctorOnboardRequest>) => void;
  onNext: () => void;
  onPrev: () => void;
  onSubmit: (data: DoctorOnboardRequest) => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  loading: boolean;
}

export default function DoctorStepUploadDocs({
  formData,
  updateFormData,
  onPrev,
  onSubmit,
  isLastStep,
  loading,
}: DoctorStepUploadDocsProps) {
  const handleFileUpload = async (file: File) => {
 
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    const mockUrl = `https://example.com/profile-${Date.now()}.${file.name.split('.').pop()}`;
    updateFormData({ profilePhotoUrl: mockUrl });
  };

  const handleSubmit = () => {
    if (
      !formData.firstName ||
      !formData.lastName ||
      !formData.specialization ||
      formData.specialization.trim() === "" ||
      !formData.licenseNumber ||
      formData.licenseNumber.trim() === "" ||
      !formData.clinicName ||
      formData.clinicName.trim() === "" ||
      formData.yearsOfExperience === undefined
    ) {
      return;
    }

    onSubmit({
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      specialization: formData.specialization.trim(),
      bio: formData.bio?.trim() || undefined,
      licenseNumber: formData.licenseNumber.trim(),
      clinicName: formData.clinicName.trim(),
      yearsOfExperience: formData.yearsOfExperience,
      profilePhotoUrl: formData.profilePhotoUrl,
    });
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <StepHeader
        title="Upload Documents"
        description="Upload your profile photo and medical certificates (optional)"
      />

      <div className="space-y-8">
        <FileUpload
          label="Profile Photo"
          accept="image/*"
          maxSizeMB={10}
          value={formData.profilePhotoUrl}
          onChange={handleFileUpload}
          onRemove={() => updateFormData({ profilePhotoUrl: undefined })}
          disabled={loading}
          helpText="PNG, JPG up to 10MB"
        />
      </div>

      <NavigationButtons
        onPrev={onPrev}
        onSubmit={handleSubmit}
        isFirstStep={false}
        isLastStep={isLastStep}
        loading={loading}
        submitLabel="Complete Profile ✓"
      />
    </div>
  );
}
