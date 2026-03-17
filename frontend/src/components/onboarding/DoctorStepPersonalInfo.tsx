import React, { useState } from "react";
import { DoctorOnboardRequest } from "../../api";
import FormField from "../ui/FormField";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import StepHeader from "../ui/StepHeader";
import NavigationButtons from "../ui/NavigationButtons";
import { validators } from "../../utils/validation";

interface DoctorStepPersonalInfoProps {
  formData: Partial<DoctorOnboardRequest>;
  updateFormData: (data: Partial<DoctorOnboardRequest>) => void;
  onNext: () => void;
  onPrev: () => void;
  onSubmit: (data: DoctorOnboardRequest) => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  loading: boolean;
}

export default function DoctorStepPersonalInfo({
  formData,
  updateFormData,
  onNext,
  isFirstStep,
  loading,
}: DoctorStepPersonalInfoProps) {
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    const firstNameError = validators.name(formData.firstName || "", "First name");
    if (firstNameError) {
      newErrors.firstName = firstNameError;
    }

    const lastNameError = validators.name(formData.lastName || "", "Last name");
    if (lastNameError) {
      newErrors.lastName = lastNameError;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = () => {
    if (validateForm()) {
      onNext();
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <StepHeader
        title="Personal Information"
        description="Tell us about yourself"
      />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <FormField
          label="First Name"
          name="firstName"
          required
          error={errors.firstName}
          className="animate-in slide-in-from-left duration-500"
        >
          <Input
            type="text"
            id="firstName"
            value={formData.firstName || ""}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => updateFormData({ firstName: e.target.value })}
            placeholder="Enter your first name"
            maxLength={50}
            className={errors.firstName ? "border-red-500 ring-2 ring-red-500/20" : ""}
          />
        </FormField>

        <FormField
          label="Last Name"
          name="lastName"
          required
          error={errors.lastName}
          className="animate-in slide-in-from-right duration-500"
        >
          <Input
            type="text"
            id="lastName"
            value={formData.lastName || ""}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => updateFormData({ lastName: e.target.value })}
            placeholder="Enter your last name"
            maxLength={50}
            className={errors.lastName ? "border-red-500 ring-2 ring-red-500/20" : ""}
          />
        </FormField>
      </div>

      <NavigationButtons
        onNext={handleNext}
        isFirstStep={isFirstStep}
        isLastStep={false}
        loading={loading}
        showPrev={false}
      />
    </div>
  );
}
