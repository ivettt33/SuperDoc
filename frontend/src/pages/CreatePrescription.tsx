import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { PrescriptionApi, CreatePrescriptionRequest, AppointmentApi, Appointment, PatientApi, PatientProfile } from "../api";
import { HiArrowLeft, HiX } from "react-icons/hi";

export default function CreatePrescription() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [patients, setPatients] = useState<{ id: number; name: string }[]>([]);
  const [loadingPatients, setLoadingPatients] = useState(true);
  
  const [formData, setFormData] = useState<CreatePrescriptionRequest>({
    patientId: 0,
    medicationName: "",
    dosage: "",
    frequency: "",
    duration: "",
    instructions: "",
    validUntil: "",
  });

  useEffect(() => {
    if (user?.role !== "DOCTOR") {
      navigate("/prescriptions");
      return;
    }
    fetchPatients();
  }, [user]);

  const fetchPatients = async () => {
    try {
      setLoadingPatients(true);
      // Get all patients from the system (not just from appointments)
      const allPatients = await PatientApi.getAllPatients();
      
      const patientsList = allPatients.map((patient) => {
        const fullName = `${patient.firstName} ${patient.lastName}`.trim();
        return {
          id: patient.profileId, // This is PatientProfile ID - backend will convert it
          name: fullName || patient.email, // Use name if available, otherwise email
          profileId: patient.profileId
        };
      });
      
      setPatients(patientsList);
    } catch (err: any) {
      console.error("Failed to fetch patients:", err);
      setError("Failed to load patients. You can still create a prescription by entering a patient ID.");
    } finally {
      setLoadingPatients(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === "patientId" ? parseInt(value) || 0 : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.patientId || formData.patientId <= 0) {
      setError("Please select a patient");
      return;
    }
    
    if (!formData.medicationName.trim()) {
      setError("Medication name is required");
      return;
    }
    
    if (!formData.dosage.trim()) {
      setError("Dosage is required");
      return;
    }
    
    if (!formData.frequency.trim()) {
      setError("Frequency is required");
      return;
    }
    
    if (!formData.duration.trim()) {
      setError("Duration is required");
      return;
    }
    
    if (!formData.validUntil) {
      setError("Valid until date is required");
      return;
    }

    const validUntilDate = new Date(formData.validUntil);
    if (validUntilDate < new Date()) {
      setError("Valid until date must be in the future");
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await PrescriptionApi.createPrescription(formData);
      navigate("/prescriptions");
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || "Failed to create prescription");
    } finally {
      setLoading(false);
    }
  };

  const getMinDate = () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today.toISOString().split("T")[0];
  };

  return (
    <div className="min-h-screen bg-gray-900 py-8">
      <div className="max-w-3xl mx-auto px-4">
        <button
          onClick={() => navigate("/prescriptions")}
          className="flex items-center space-x-2 text-gray-400 hover:text-white mb-6 transition-colors"
        >
          <HiArrowLeft className="w-5 h-5" />
          <span>Back to Prescriptions</span>
        </button>

        <div className="bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-700">
          <h1 className="text-3xl font-bold text-white mb-2">Create Prescription</h1>
          <p className="text-gray-400 mb-6">Fill in the details to create a new prescription</p>

          {error && (
            <div className="mb-6 p-4 bg-red-500/20 border border-red-500 rounded-lg text-red-300">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Patient Selection */}
            <div>
              <label htmlFor="patientId" className="block text-sm font-medium text-gray-300 mb-2">
                Patient <span className="text-red-400">*</span>
              </label>
              {loadingPatients ? (
                <div className="text-gray-400 text-sm">Loading patients...</div>
              ) : patients.length > 0 ? (
                <div>
                  <select
                    id="patientId"
                    name="patientId"
                    value={formData.patientId}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value={0}>Select a patient</option>
                    {patients.map(patient => (
                      <option key={patient.id} value={patient.id}>
                        {patient.name} (Profile ID: {patient.id})
                      </option>
                    ))}
                  </select>
                  <p className="text-xs text-yellow-400 mt-1">
                    ⚠️ Verify the patient name matches before creating the prescription. The backend will convert the Profile ID to the correct User ID.
                  </p>
                  {formData.patientId > 0 && (
                    <p className="text-xs text-gray-400 mt-1">
                      Selected: PatientProfile ID {formData.patientId}
                    </p>
                  )}
                </div>
              ) : (
                <div className="space-y-2">
                  <input
                    type="number"
                    id="patientId"
                    name="patientId"
                    value={formData.patientId || ""}
                    onChange={handleChange}
                    placeholder="Enter Patient ID"
                    required
                    min="1"
                    className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                  <p className="text-xs text-gray-500">Enter the patient ID if you know it</p>
                </div>
              )}
            </div>

            {/* Medication Name */}
            <div>
              <label htmlFor="medicationName" className="block text-sm font-medium text-gray-300 mb-2">
                Medication Name <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                id="medicationName"
                name="medicationName"
                value={formData.medicationName}
                onChange={handleChange}
                required
                placeholder="e.g., Amoxicillin 500mg"
                className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Dosage */}
            <div>
              <label htmlFor="dosage" className="block text-sm font-medium text-gray-300 mb-2">
                Dosage <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                id="dosage"
                name="dosage"
                value={formData.dosage}
                onChange={handleChange}
                required
                placeholder="e.g., 500mg"
                className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Frequency */}
            <div>
              <label htmlFor="frequency" className="block text-sm font-medium text-gray-300 mb-2">
                Frequency <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                id="frequency"
                name="frequency"
                value={formData.frequency}
                onChange={handleChange}
                required
                placeholder="e.g., Twice daily, Every 8 hours"
                className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Duration */}
            <div>
              <label htmlFor="duration" className="block text-sm font-medium text-gray-300 mb-2">
                Duration <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                id="duration"
                name="duration"
                value={formData.duration}
                onChange={handleChange}
                required
                placeholder="e.g., 7 days, 2 weeks"
                className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Instructions */}
            <div>
              <label htmlFor="instructions" className="block text-sm font-medium text-gray-300 mb-2">
                Instructions
              </label>
              <textarea
                id="instructions"
                name="instructions"
                value={formData.instructions}
                onChange={handleChange}
                rows={4}
                placeholder="e.g., Take with food, Avoid alcohol, etc."
                className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              />
            </div>

            {/* Valid Until */}
            <div>
              <label htmlFor="validUntil" className="block text-sm font-medium text-gray-300 mb-2">
                Valid Until <span className="text-red-400">*</span>
              </label>
              <input
                type="date"
                id="validUntil"
                name="validUntil"
                value={formData.validUntil}
                onChange={handleChange}
                required
                min={getMinDate()}
                className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-1">The prescription will expire after this date</p>
            </div>

            {/* Submit Buttons */}
            <div className="flex space-x-4 pt-4">
              <button
                type="button"
                onClick={() => navigate("/prescriptions")}
                className="flex-1 px-6 py-3 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? "Creating..." : "Create Prescription"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

