import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../AuthContext";
import { DoctorApi, DoctorOnboardRequest } from "../../api";
import OnboardingLayout from "../../components/onboarding/OnboardingLayout";
import DoctorStepPersonalInfo from "../../components/onboarding/DoctorStepPersonalInfo";
import DoctorStepProfessionalInfo from "../../components/onboarding/DoctorStepProfessionalInfo";
import DoctorStepUploadDocs from "../../components/onboarding/DoctorStepUploadDocs";

const STEPS = [
  { id: 1, title: "Personal Information", component: DoctorStepPersonalInfo },
  { id: 2, title: "Professional Details", component: DoctorStepProfessionalInfo },
  { id: 3, title: "Upload Documents", component: DoctorStepUploadDocs },
];

export default function DoctorOnboarding() {
  const navigate = useNavigate();
  const { completeOnboarding } = useAuth();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState<Partial<DoctorOnboardRequest>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateFormData = (data: Partial<DoctorOnboardRequest>) => {
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

  const handleSubmit = async (finalData: DoctorOnboardRequest) => {
    setLoading(true);
    setError(null);

    try {
      await DoctorApi.createOrUpdateProfile(finalData);
      // Mark onboarding as complete
      completeOnboarding();
      // Redirect to dashboard
      navigate("/");
    } catch (error) {
      console.error("Failed to create doctor profile:", error);
      setError("Failed to create profile. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const CurrentStepComponent = STEPS[currentStep - 1].component;

  return (
    <OnboardingLayout
      title="Doctor Onboarding"
      description="Complete your professional profile to get started"
      iconColor="blue"
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
