import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../AuthContext";
import { PatientApi, PatientOnboardRequest } from "../../api";
import OnboardingLayout from "../../components/onboarding/OnboardingLayout";
import PatientStepPersonalInfo from "../../components/onboarding/PatientStepPersonalInfo";
import PatientStepMedicalInfo from "../../components/onboarding/PatientStepMedicalInfo";

const STEPS = [
  { id: 1, title: "Personal Information", component: PatientStepPersonalInfo },
  { id: 2, title: "Medical Information", component: PatientStepMedicalInfo },
];

export default function PatientOnboarding() {
  const navigate = useNavigate();
  const { completeOnboarding } = useAuth();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState<Partial<PatientOnboardRequest>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateFormData = (data: Partial<PatientOnboardRequest>) => {
    setFormData(prev => ({ ...prev, ...data }));
  };

  const nextStep = () => {
    if (currentStep < STEPS.length) {
      setCurrentStep(currentStep + 1);
    }
  };

  const prevStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleSubmit = async (finalData: PatientOnboardRequest) => {
    setLoading(true);
    setError(null);

    try {
      await PatientApi.createOrUpdateProfile(finalData);
      // Mark onboarding as complete
      completeOnboarding();
      // Redirect to dashboard
      navigate("/");
    } catch (error) {
      console.error("Failed to create patient profile:", error);
      setError("Failed to create profile. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const CurrentStepComponent = STEPS[currentStep - 1].component;

  return (
    <OnboardingLayout
      title="Patient Onboarding"
      description="Complete your profile to get started with SuperDoc"
      iconColor="green"
      currentStep={currentStep}
      steps={STEPS}
      error={error}
    >
      <CurrentStepComponent
        formData={formData}
        updateFormData={updateFormData}
        onNext={nextStep}
        onPrev={prevStep}
        onSubmit={handleSubmit}
        isFirstStep={currentStep === 1}
        isLastStep={currentStep === STEPS.length}
        loading={loading}
      />
    </OnboardingLayout>
  );
}

