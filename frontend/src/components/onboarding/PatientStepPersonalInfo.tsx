import React, { useState } from "react";
import { PatientOnboardRequest } from "../../api";
import FormField from "../ui/FormField";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Select from "../ui/select";
import StepHeader from "../ui/StepHeader";
import NavigationButtons from "../ui/NavigationButtons";
import { validators } from "../../utils/validation";

interface PatientStepPersonalInfoProps {
  formData: Partial<PatientOnboardRequest>;
  updateFormData: (data: Partial<PatientOnboardRequest>) => void;
  onNext: () => void;
  onPrev: () => void;
  onSubmit: (data: PatientOnboardRequest) => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  loading: boolean;
}

export default function PatientStepPersonalInfo({
  formData,
  updateFormData,
  onNext,
  isFirstStep,
  loading,
}: PatientStepPersonalInfoProps) {
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

    const dateError = validators.dateOfBirth(formData.dateOfBirth);
    if (dateError) {
      newErrors.dateOfBirth = dateError;
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
            onChange={(e) => updateFormData({ firstName: e.target.value })}
            placeholder="Enter your first name"
            maxLength={50}
            error={!!errors.firstName}
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
            onChange={(e) => updateFormData({ lastName: e.target.value })}
            placeholder="Enter your last name"
            maxLength={50}
            error={!!errors.lastName}
          />
        </FormField>
      </div>

      <FormField
        label="Date of Birth"
        name="dateOfBirth"
        required
        error={errors.dateOfBirth}
        className="animate-in slide-in-from-left duration-700"
      >
        <Input
          type="date"
          id="dateOfBirth"
          value={formData.dateOfBirth || ""}
          onChange={(e) => updateFormData({ dateOfBirth: e.target.value })}
          error={!!errors.dateOfBirth}
        />
      </FormField>

      <FormField
        label="Gender"
        name="gender"
        className="animate-in slide-in-from-right duration-700"
      >
        <Select
          id="gender"
          value={formData.gender || ""}
          onChange={(e) => updateFormData({ gender: e.target.value })}
        >
          <option value="">Select gender</option>
          <option value="Male">Male</option>
          <option value="Female">Female</option>
          <option value="Other">Other</option>
          <option value="Prefer not to say">Prefer not to say</option>
        </Select>
      </FormField>

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

