import React, { useEffect, useState } from "react";
import { DoctorOnboardRequest } from "../../api";
import FormField from "../ui/FormField";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Select from "../ui/select";
import { Textarea } from "@/components/ui/textarea";
import StepHeader from "../ui/StepHeader";
import NavigationButtons from "../ui/NavigationButtons";
import { validators } from "../../utils/validation";
import { SPECIALIZATIONS } from "../../constants/specializations";

interface DoctorStepProfessionalInfoProps {
  formData: Partial<DoctorOnboardRequest>;
  updateFormData: (data: Partial<DoctorOnboardRequest>) => void;
  onNext: () => void;
  onPrev: () => void;
  onSubmit: (data: DoctorOnboardRequest) => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  loading: boolean;
}

export default function DoctorStepProfessionalInfo({
  formData,
  updateFormData,
  onNext,
  onPrev,
  onSubmit,
  isLastStep,
  loading,
}: DoctorStepProfessionalInfoProps) {
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [showOtherInput, setShowOtherInput] = useState(false);
  const [customSpecialization, setCustomSpecialization] = useState("");

  useEffect(() => {
    if (formData.specialization && !SPECIALIZATIONS.includes(formData.specialization)) {
      setShowOtherInput(true);
      setCustomSpecialization(formData.specialization);
    }
  }, [formData.specialization]);

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    const specializationValue = showOtherInput ? customSpecialization : formData.specialization;
    const specializationError = validators.specialization(specializationValue);
    if (specializationError) {
      newErrors.specialization = specializationError;
    }

    const experienceError = validators.experience(formData.yearsOfExperience ?? null);
    if (experienceError) {
      newErrors.yearsOfExperience = experienceError;
    }

    const clinicNameError = validators.clinicName(formData.clinicName);
    if (clinicNameError) {
      newErrors.clinicName = clinicNameError;
    }

    const licenseError = validators.licenseNumber(formData.licenseNumber);
    if (licenseError) {
      newErrors.licenseNumber = licenseError;
    }

    // Validate bio if provided (optional field)
    const bioError = validators.textLength(formData.bio, "Bio", 1000);
    if (bioError) {
      newErrors.bio = bioError;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = () => {
    if (validateForm()) {
      const sanitizedSpecialization = showOtherInput ? customSpecialization.trim() : (formData.specialization ?? "").trim();
      updateFormData({
        specialization: sanitizedSpecialization,
        clinicName: formData.clinicName?.trim(),
        licenseNumber: formData.licenseNumber?.trim(),
        bio: formData.bio?.trim(),
      });
      onNext();
    }
  };

  const handleSubmit = () => {
    if (!validateForm()) {
      return;
    }

    const specialization = (showOtherInput ? customSpecialization : formData.specialization ?? "").trim();
    const license = formData.licenseNumber?.trim() ?? "";
    const clinic = formData.clinicName?.trim() ?? "";
    const years = formData.yearsOfExperience;

    if (!formData.firstName || !formData.lastName || !specialization || !license || !clinic || years === undefined) {
      return;
    }

    onSubmit({
      firstName: formData.firstName,
      lastName: formData.lastName,
      specialization,
      bio: formData.bio?.trim() || undefined,
      licenseNumber: license,
      clinicName: clinic,
      yearsOfExperience: years,
      profilePhotoUrl: formData.profilePhotoUrl,
      openingHours: formData.openingHours || "09:00",
      closingHours: formData.closingHours || "17:00",
    });
  };
  
  const handleSpecializationChange = (value: string) => {
    if (value === "Other") {
      setShowOtherInput(true);
      setCustomSpecialization("");
      updateFormData({ specialization: "" });
    } else {
      setShowOtherInput(false);
      setCustomSpecialization("");
      updateFormData({ specialization: value });
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <StepHeader
        title="Professional Details"
        description="Tell us about your medical practice"
      />

      <div className="space-y-6">
        <FormField
          label="Specialization"
          name="specialization"
          required
          error={errors.specialization}
          className="animate-in slide-in-from-left duration-500"
        >
          <Select
            id="specialization"
            value={showOtherInput ? "Other" : (formData.specialization || "")}
            onChange={(e) => handleSpecializationChange(e.target.value)}
            error={!!errors.specialization}
          >
            <option value="">Select your specialization</option>
            {SPECIALIZATIONS.map((spec) => (
              <option key={spec} value={spec}>
                {spec}
              </option>
            ))}
          </Select>
          
          {/* Custom specialization input for "Other" */}
          {showOtherInput && (
            <div className="mt-3 animate-in slide-in-from-top duration-300">
              <Input
                type="text"
                value={customSpecialization}
                onChange={(e) => {
                  setCustomSpecialization(e.target.value);
                  updateFormData({ specialization: e.target.value });
                }}
                placeholder="Please specify your specialization"
                maxLength={100}
                autoFocus
                error={!!errors.specialization}
              />
            </div>
          )}
        </FormField>

        <FormField
          label="Years of Experience"
          name="yearsOfExperience"
          required
          error={errors.yearsOfExperience}
          className="animate-in slide-in-from-right duration-500"
        >
          <Input
            type="number"
            id="yearsOfExperience"
            min="0"
            max="50"
            value={formData.yearsOfExperience ?? ""}
            onChange={(e) => {
              const value = e.target.value;
              updateFormData({ yearsOfExperience: value === "" ? undefined : Number(value) });
            }}
            placeholder="Enter years of experience"
            error={!!errors.yearsOfExperience}
          />
        </FormField>

        <FormField
          label="Clinic/Hospital Name"
          name="clinicName"
          required
          error={errors.clinicName}
          className="animate-in slide-in-from-left duration-700"
        >
          <Input
            type="text"
            id="clinicName"
            value={formData.clinicName || ""}
            onChange={(e) => updateFormData({ clinicName: e.target.value })}
            placeholder="Enter clinic or hospital name"
            maxLength={100}
            error={!!errors.clinicName}
          />
        </FormField>

        <FormField
          label="License Number"
          name="licenseNumber"
          required
          error={errors.licenseNumber}
          className="animate-in slide-in-from-right duration-700"
        >
          <Input
            type="text"
            id="licenseNumber"
            inputMode="numeric"
            pattern="[0-9]*"
            value={formData.licenseNumber || ""}
            onChange={(e) => updateFormData({ licenseNumber: e.target.value })}
            placeholder="Enter your medical license number"
            maxLength={50}
            error={!!errors.licenseNumber}
          />
        </FormField>

        <FormField
          label="Bio"
          name="bio"
          helpText="Optional (max 1000 characters)"
          error={errors.bio}
          className="animate-in fade-in duration-1000"
        >
          <Textarea
            id="bio"
            value={formData.bio || ""}
            onChange={(e) => updateFormData({ bio: e.target.value })}
            placeholder="Tell us about your medical background and experience..."
            rows={4}
            maxLength={1000}
            error={!!errors.bio}
          />
        </FormField>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            label="Opening Hours"
            name="openingHours"
            helpText="Default: 09:00"
            error={errors.openingHours}
            className="animate-in slide-in-from-left duration-700"
          >
            <Input
              type="time"
              id="openingHours"
              value={formData.openingHours || "09:00"}
              onChange={(e) => updateFormData({ openingHours: e.target.value })}
            />
          </FormField>

          <FormField
            label="Closing Hours"
            name="closingHours"
            helpText="Default: 17:00"
            error={errors.closingHours}
            className="animate-in slide-in-from-right duration-700"
          >
            <Input
              type="time"
              id="closingHours"
              value={formData.closingHours || "17:00"}
              onChange={(e) => updateFormData({ closingHours: e.target.value })}
            />
          </FormField>
        </div>
      </div>

      <NavigationButtons
        onPrev={onPrev}
        onNext={handleNext}
        onSubmit={handleSubmit}
        isFirstStep={false}
        isLastStep={isLastStep}
        loading={loading}
      />
    </div>
  );
}
