import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { PrescriptionApi, UpdatePrescriptionRequest, Prescription } from "../api";
import { HiArrowLeft } from "react-icons/hi";

export default function EditPrescription() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [prescription, setPrescription] = useState<Prescription | null>(null);
  
  const [formData, setFormData] = useState<UpdatePrescriptionRequest>({
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
    if (id) {
      fetchPrescription();
    }
  }, [id, user]);

  const fetchPrescription = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      setError(null);
      const data = await PrescriptionApi.getPrescription(parseInt(id));
      setPrescription(data);
      setFormData({
        medicationName: data.medicationName,
        dosage: data.dosage,
        frequency: data.frequency,
        duration: data.duration,
        instructions: data.instructions || "",
        validUntil: data.validUntil.split("T")[0], // Convert to date format
      });
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load prescription");
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
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

    if (!id) return;

    try {
      setSaving(true);
      setError(null);
      await PrescriptionApi.updatePrescription(parseInt(id), formData);
      navigate("/prescriptions");
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || "Failed to update prescription");
    } finally {
      setSaving(false);
    }
  };

  const getMinDate = () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today.toISOString().split("T")[0];
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 py-8">
        <div className="max-w-3xl mx-auto px-4">
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-gray-400">Loading prescription...</div>
          </div>
        </div>
      </div>
    );
  }

  if (!prescription) {
    return (
      <div className="min-h-screen bg-gray-900 py-8">
        <div className="max-w-3xl mx-auto px-4">
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-red-500">{error || "Prescription not found"}</div>
          </div>
        </div>
      </div>
    );
  }

  if (prescription.status !== "DRAFT") {
    return (
      <div className="min-h-screen bg-gray-900 py-8">
        <div className="max-w-3xl mx-auto px-4">
          <div className="bg-yellow-500/20 border border-yellow-500 rounded-lg p-6 text-yellow-300">
            <p className="font-semibold mb-2">Cannot Edit Prescription</p>
            <p className="text-sm mb-4">
              Only DRAFT prescriptions can be edited. This prescription is currently {prescription.status}.
            </p>
            <button
              onClick={() => navigate("/prescriptions")}
              className="px-4 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
            >
              Back to Prescriptions
            </button>
          </div>
        </div>
      </div>
    );
  }

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
          <h1 className="text-3xl font-bold text-white mb-2">Edit Prescription</h1>
          <p className="text-gray-400 mb-2">Patient: {prescription.patientName}</p>
          <p className="text-gray-400 mb-6">Status: <span className="text-yellow-400">{prescription.status}</span></p>

          {error && (
            <div className="mb-6 p-4 bg-red-500/20 border border-red-500 rounded-lg text-red-300">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
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
                disabled={saving}
                className="flex-1 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {saving ? "Saving..." : "Save Changes"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

