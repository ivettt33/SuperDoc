import React, { useState } from "react";
import { PatientOnboardRequest } from "../../api";
import { FileApi } from "../../api";
import FormField from "../ui/FormField";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import FileUpload from "../ui/FileUpload";
import StepHeader from "../ui/StepHeader";
import NavigationButtons from "../ui/NavigationButtons";
import { validators } from "../../utils/validation";

interface PatientStepMedicalInfoProps {
  formData: Partial<PatientOnboardRequest>;
  updateFormData: (data: Partial<PatientOnboardRequest>) => void;
  onNext: () => void;
  onPrev: () => void;
  onSubmit: (data: PatientOnboardRequest) => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  loading: boolean;
}

export default function PatientStepMedicalInfo({
  formData,
  updateFormData,
  onPrev,
  onSubmit,
  isLastStep,
  loading,
}: PatientStepMedicalInfoProps) {
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    // Validate optional fields if provided
    const conditionsError = validators.textLength(formData.conditions, "Medical conditions", 2000);
    if (conditionsError) {
      newErrors.conditions = conditionsError;
    }

    const insuranceError = validators.insuranceNumber(formData.insuranceNumber);
    if (insuranceError) {
      newErrors.insuranceNumber = insuranceError;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleFileUpload = async (file: File) => {
    const result = await FileApi.uploadFile(file);
    // Store just the filename, not the full path
    const fileName = result.fileName || result.filePath;
    updateFormData({ profilePicture: fileName });
  };

  const handleSubmit = () => {
    if (!validateForm()) {
      return;
    }
    
    // Ensure all required fields are present
    if (formData.firstName && formData.lastName && formData.dateOfBirth) {
      onSubmit(formData as PatientOnboardRequest);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <StepHeader
        title="Medical Information"
        description="Help us understand your health needs"
      />

      <FormField
        label="Medical Conditions"
        name="conditions"
        helpText="Optional - This information helps doctors provide better care"
        error={errors.conditions}
        className="animate-in slide-in-from-left duration-500"
      >
        <Textarea
          id="conditions"
          value={formData.conditions || ""}
          onChange={(e) => updateFormData({ conditions: e.target.value })}
          placeholder="List any medical conditions, allergies, or health concerns..."
          rows={4}
          maxLength={1000}
        />
      </FormField>

      <FormField
        label="Insurance Number"
        name="insuranceNumber"
        helpText="Optional - helps with billing and coverage verification"
        error={errors.insuranceNumber}
        className="animate-in slide-in-from-right duration-500"
      >
        <Input
          type="text"
          id="insuranceNumber"
          inputMode="numeric"
          pattern="[0-9]*"
          value={formData.insuranceNumber || ""}
          onChange={(e) => updateFormData({ insuranceNumber: e.target.value })}
          placeholder="Enter your insurance policy number"
          maxLength={50}
        />
      </FormField>

      <div className="animate-in slide-in-from-left duration-700">
        <FileUpload
          label="Profile Picture"
          accept="image/*"
          maxSizeMB={5}
          value={formData.profilePicture}
          onChange={handleFileUpload}
          onRemove={() => updateFormData({ profilePicture: undefined })}
          disabled={loading}
          helpText="Optional - Upload a profile picture (max 5MB, images only)"
        />
      </div>

      <NavigationButtons
        onPrev={onPrev}
        onSubmit={handleSubmit}
        isFirstStep={false}
        isLastStep={isLastStep}
        loading={loading}
        prevLabel="← Previous Step"
        submitLabel="Complete Registration ✓"
      />
    </div>
  );
}

